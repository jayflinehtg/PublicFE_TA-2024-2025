package com.example.myapplication

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.*
import com.example.myapplication.data.DataClassResponses.PrepareRegistrationRequest
import com.example.myapplication.data.EventSink
import com.example.myapplication.data.LoginRequest
import com.example.myapplication.data.LoginResult
import com.example.myapplication.data.LogoutResult
import com.example.myapplication.data.PreferencesHelper
import com.example.myapplication.data.RegistrationResult
import com.example.myapplication.data.UiEvent
import com.example.myapplication.data.UiState
import com.example.myapplication.exceptions.ViewModelValidationException
import com.example.myapplication.services.ApiService
import com.example.myapplication.services.RetrofitClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.metamask.androidsdk.EthereumFlow
import io.metamask.androidsdk.EthereumMethod
import io.metamask.androidsdk.EthereumRequest
import io.metamask.androidsdk.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val ethereum: EthereumFlow,
    @ApplicationContext private val context: Context,
    private val apiService: ApiService
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _uiState = MutableStateFlow(
        UiState(
            shouldShowWalletConnect = !PreferencesHelper.isMetaMaskConnected(context),
            registrationState = RegistrationResult.Idle,
            loginState = LoginResult.Idle,
            logoutState = LogoutResult.Idle
        ).copy(fullName = PreferencesHelper.getUserFullName(context) ?: "")
    )
    val uiState = _uiState.asStateFlow()

    val userToken: String
        get() {
            val jwtToken = PreferencesHelper.getJwtToken(context)
            return if (jwtToken != null && !jwtToken.startsWith("Bearer ", ignoreCase = true)) {
                "Bearer $jwtToken"
            } else {
                jwtToken ?: ""
            }
        }

    private fun sendUiMessage(message: String) {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.Message(message))
        }
    }

    // Fungsi reset state untuk dipanggil dari UI setelah menangani hasil operasi
    fun resetRegistrationState() { _uiState.update { it.copy(registrationState = RegistrationResult.Idle) } }
    fun resetLoginState() { _uiState.update { it.copy(loginState = LoginResult.Idle) } }
    fun resetLogoutState() { _uiState.update { it.copy(logoutState = LogoutResult.Idle) } }
    fun resetGuestState() { _uiState.update { it.copy(isGuest = false) } }

    fun eventSink(eventSink: EventSink) {
        viewModelScope.launch {
            when (eventSink) {
                EventSink.Connect -> connectWallet()
                EventSink.Disconnect -> disconnectWallet()
                EventSink.GuestLogin -> handleGuestLogin()
            }
        }
    }

    fun fetchUserDataFromPrefs(onAuthFailure: (() -> Unit)? = null) {
        val walletAddress = PreferencesHelper.getWalletAddress(context)
        if (walletAddress.isNullOrEmpty()) {
            if (!_uiState.value.isGuest) {
                _uiState.update { it.copy(isLoggedIn = false, fullName = null) }
            }
            return
        }
        viewModelScope.launch {
            Log.d("MainViewModel_Fetch", "Fetching user info for $walletAddress")
            try {
                val response = apiService.getUserInfo(walletAddress)
                Log.d("MainViewModel_Fetch", "User Info Response: $response")
                if (response.success) {
                    _uiState.update { state ->
                        state.copy(
                            fullName = response.userData.fullName ?: state.fullName ?: "Nama Pengguna",
                            isLoggedIn = response.userData.isLoggedIn
                        )
                    }
                    response.userData.fullName?.let { PreferencesHelper.saveUserFullName(context, it) }
                } else {
                    // Karena tidak ada response.message, gunakan pesan default
                    Log.w("MainViewModel_Fetch", "Gagal mengambil info pengguna (success=false)")
                    onAuthFailure?.invoke()
                    sendUiMessage("Gagal memuat data pengguna.")
                }
            } catch (e: retrofit2.HttpException) {
                Log.e("MainViewModel_Fetch", "HttpException fetching user info: ${e.code()} - ${e.message()}", e)
                if (e.code() == 401) {
                    onAuthFailure?.invoke()
                } else {
                    sendUiMessage("Gagal memuat data pengguna (HTTP ${e.code()})")
                }
            }
            catch (e: Exception) {
                Log.e("MainViewModel_Fetch", "Error fetching user info: ${e.message}", e)
                sendUiMessage("Gagal memuat data pengguna: ${e.message}")
            }
        }
    }


    fun performRegistration(fullNameInput: String, passwordInput: String) {
        Log.d("MainViewModel_Register", "performRegistration untuk: $fullNameInput")
        viewModelScope.launch {
            _uiState.update { it.copy(registrationState = RegistrationResult.Loading, isConnecting = true) }
            val registrationOutcome = runCatching {

                val userWalletAddress = ethereum.selectedAddress
                if (userWalletAddress.isEmpty()) throw ViewModelValidationException("Alamat wallet tidak tersedia.")

                val prepareResponse = try {
                    apiService.prepareRegistration(
                        PrepareRegistrationRequest(
                            fullName = fullNameInput,
                            password = passwordInput,
                            walletAddress = userWalletAddress
                        )
                    )
                } catch (e: retrofit2.HttpException) {
                    // Handle HTTP error dari backend
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e("MainViewModel_Register", "HTTP Error: ${e.code()} - $errorBody")

                    // Parse error message dari backend
                    val errorMessage = try {
                        val errorJson = com.google.gson.JsonParser.parseString(errorBody ?: "")
                        errorJson.asJsonObject.get("message")?.asString ?: "Error tidak diketahui"
                    } catch (parseError: Exception) {
                        "Error dari server: ${e.message()}"
                    }

                    // Jika error adalah "sudah terdaftar", langsung throw tanpa transaksi
                    if (errorMessage.contains("Anda sudah memiliki akun", ignoreCase = true)) {
                        Log.d("MainViewModel_Register", "User sudah terdaftar, tidak akan menampilkan MetaMask")
                        throw ViewModelValidationException(errorMessage)
                    } else {
                        throw ViewModelValidationException("Gagal mempersiapkan registrasi: $errorMessage")
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel_Register", "Network Error: ${e.message}")
                    throw ViewModelValidationException("Gagal terhubung ke server: ${e.message}")
                }

                if (!prepareResponse.success || prepareResponse.data?.transactionData == null) {
                    throw ViewModelValidationException(prepareResponse.message ?: "Gagal mempersiapkan data transaksi registrasi.")
                }

                val transactionDataHex = prepareResponse.data.transactionData

                val contractAddress = RetrofitClient.SMART_CONTRACT_ADDRESS
                if (contractAddress.isBlank() || contractAddress == "SMART_CONTRACT_ADDRESS_DI_RETROFIT_CLIENT") {
                    throw ViewModelValidationException("Alamat smart contract belum dikonfigurasi.")
                }
                if (transactionDataHex.isBlank() || transactionDataHex == "0x") {
                    throw ViewModelValidationException("Data transaksi dari server tidak valid.")
                }

                //Hanya sampai sini jika backend berhasil dan tidak ada error "sudah terdaftar"
                Log.d("MainViewModel_Register", "Memulai transaksi on-chain untuk registrasi...")

                val txParams = mapOf("from" to userWalletAddress, "to" to contractAddress, "data" to transactionDataHex)
                val request = EthereumRequest(method = EthereumMethod.ETH_SEND_TRANSACTION.value, params = listOf(txParams))
                val transactionResult = ethereum.sendRequest(request)

                when (val specificResult = transactionResult) {
                    is Result.Success.Item -> {
                        val txHash = specificResult.value
                        if (txHash.isNotEmpty()) txHash else throw Exception("Registrasi on-chain sukses tapi txHash tidak valid: $txHash")
                    }
                    is Result.Success.Items -> {
                        val txHashes = specificResult.value
                        if (txHashes.isNotEmpty()) txHashes[0] else throw Exception("Registrasi on-chain sukses tapi txHashes kosong")
                    }
                    is Result.Success.ItemMap -> {
                        val txMap = specificResult.value
                        txMap["txHash"] as? String ?: throw Exception("Registrasi on-chain sukses tapi txHash tidak ditemukan di map")
                    }
                    is Result.Error -> {
                        val error = specificResult.error

                        // Handle user rejection
                        if (error.code == 4001 || error.message.contains("user rejected", ignoreCase = true)) {
                            throw Exception("User membatalkan transaksi registrasi")
                        } else {
                            throw Exception("Registrasi ke blockchain gagal: ${error.message} (Code: ${error.code})")
                        }
                    }
                }
            }.fold(
                onSuccess = { txHash -> RegistrationResult.Success(txHash) },
                onFailure = { throwable ->
                    val errorMessage = throwable.message ?: "Terjadi kesalahan registrasi"
                    Log.e("MainViewModel_Register", "Registration failed: $errorMessage")

                    // Cek error message untuk sudah terdaftar
                    when {
                        errorMessage.contains("Anda sudah memiliki akun", ignoreCase = true) -> {
                            RegistrationResult.AlreadyRegistered(errorMessage)
                        }
                        errorMessage.contains("User membatalkan", ignoreCase = true) -> {
                            RegistrationResult.Error("Registrasi dibatalkan oleh user")
                        }
                        else -> {
                            RegistrationResult.Error(errorMessage)
                        }
                    }
                }
            )

            _uiState.update { it.copy(registrationState = registrationOutcome, isConnecting = false) }

            when (registrationOutcome) {
                is RegistrationResult.Success -> {
                    // refresh data
                    onRegisterSuccessAfterChain()
                    viewModelScope.launch {
                        _uiEvent.emit(UiEvent.Message("Registrasi berhasil!"))
                        _uiEvent.emit(UiEvent.NavigateTo(Screen.Login.route))
                    }
                }
                is RegistrationResult.AlreadyRegistered -> {
                    Log.d("MainViewModel_Register", "User sudah terdaftar, navigasi ke login")
                    viewModelScope.launch {
                        _uiEvent.emit(UiEvent.Message(registrationOutcome.message))
                        _uiEvent.emit(UiEvent.NavigateTo(Screen.Login.route))
                    }
                }
                is RegistrationResult.Error -> {
                    viewModelScope.launch {
                        _uiEvent.emit(UiEvent.Message("Error: ${registrationOutcome.errorMessage}"))
                    }
                }
                else -> {}
            }
        }
    }

    fun performLogin(passwordInput: String) {
        val userWalletAddress = ethereum.selectedAddress
        if (userWalletAddress.isEmpty()) {
            _uiState.update { it.copy(loginState = LoginResult.Error("Alamat wallet tidak aktif."), isConnecting = false) }
            return
        }
        Log.d("MainViewModel_Login", "performLogin untuk: $userWalletAddress")
        viewModelScope.launch {
            _uiState.update { it.copy(loginState = LoginResult.Loading, isConnecting = true) }
            val loginOutcome = runCatching {
                val loginApiResponse = try {
                    apiService.loginUser(
                        LoginRequest(walletAddress = userWalletAddress, password = passwordInput)
                    )
                } catch (e: retrofit2.HttpException) {
                    // Handle HTTP error dari backend
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e("MainViewModel_Login", "HTTP Error: ${e.code()} - $errorBody")

                    // Parse error message dari backend
                    val errorMessage = try {
                        val errorJson = com.google.gson.JsonParser.parseString(errorBody ?: "")
                        errorJson.asJsonObject.get("message")?.asString ?: "Error tidak diketahui"
                    } catch (parseError: Exception) {
                        "Error dari server: ${e.message()}"
                    }

                    // **Handle specific login errors**
                    when {
                        errorMessage.contains("Password salah", ignoreCase = true) -> {
                            Log.d("MainViewModel_Login", "Password salah, tidak akan menampilkan MetaMask")
                            throw ViewModelValidationException("Password salah.")
                        }
                        errorMessage.contains("Pengguna belum terdaftar", ignoreCase = true) -> {
                            Log.d("MainViewModel_Login", "Pengguna belum terdaftar, tidak akan menampilkan MetaMask")
                            throw ViewModelValidationException("Pengguna belum terdaftar.")
                        }
                        errorMessage.contains("Login gagal", ignoreCase = true) -> {
                            // Extract original message after "Login gagal: "
                            val originalMessage = errorMessage.substringAfter("Login gagal: ")
                            throw ViewModelValidationException(originalMessage)
                        }
                        else -> {
                            throw ViewModelValidationException("Gagal login: $errorMessage")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel_Login", "Network Error: ${e.message}")
                    throw ViewModelValidationException("Gagal terhubung ke server: ${e.message}")
                }

                if (!loginApiResponse.success || loginApiResponse.token.isNullOrBlank() ||
                    loginApiResponse.userData == null || loginApiResponse.loginTransactionData.isNullOrBlank()) {
                    throw ViewModelValidationException(loginApiResponse.message ?: "Gagal login dari server.")
                }

                PreferencesHelper.saveJwtToken(context, loginApiResponse.token)
                PreferencesHelper.saveUserFullName(context, loginApiResponse.userData.fullName ?: "")
                PreferencesHelper.saveUserRegistrationStatus(context, loginApiResponse.userData.isRegistered)

                Log.d("MainViewModel_Login", "Memulai transaksi on-chain untuk login...")

                val loginTransactionDataHex = loginApiResponse.loginTransactionData

                val txParams = mapOf("from" to userWalletAddress, "to" to RetrofitClient.SMART_CONTRACT_ADDRESS, "data" to loginTransactionDataHex)
                val request = EthereumRequest(method = EthereumMethod.ETH_SEND_TRANSACTION.value, params = listOf(txParams))
                val transactionResult = ethereum.sendRequest(request)

                when (val specificResult = transactionResult) {
                    is Result.Success.Item -> {
                        val txHash = specificResult.value
                        if (txHash.isNotEmpty()) Triple(loginApiResponse.token, loginApiResponse.userData, txHash) else {
                            PreferencesHelper.clearJwtToken(context)
                            throw Exception("Login on-chain sukses tapi txHash tidak valid: $txHash")
                        }
                    }
                    is Result.Success.Items -> {
                        val txHashes = specificResult.value
                        if (txHashes.isNotEmpty()) Triple(loginApiResponse.token, loginApiResponse.userData, txHashes[0]) else {
                            PreferencesHelper.clearJwtToken(context)
                            throw Exception("Login on-chain sukses tapi txHashes kosong")
                        }
                    }
                    is Result.Success.ItemMap -> {
                        val txMap = specificResult.value
                        val txHash = txMap["txHash"] as? String
                        if (txHash != null && txHash.isNotEmpty()) Triple(loginApiResponse.token, loginApiResponse.userData, txHash) else {
                            PreferencesHelper.clearJwtToken(context)
                            throw Exception("Login on-chain sukses tapi txHash tidak ditemukan di map")
                        }
                    }
                    is Result.Error -> {
                        val error = specificResult.error
                        PreferencesHelper.clearJwtToken(context)

                        // Handle user rejection
                        if (error.code == 4001 || error.message.contains("user rejected", ignoreCase = true)) {
                            throw Exception("User membatalkan transaksi login")
                        } else {
                            throw Exception("Login ke blockchain gagal: ${error.message} (Code: ${error.code})")
                        }
                    }
                }
            }.fold(
                onSuccess = { (token, userData, txHash) -> LoginResult.Success(token, userData, txHash) },
                onFailure = { throwable ->
                    PreferencesHelper.clearJwtToken(context)
                    Log.e("MainViewModel_Login", "Proses login gagal: ${throwable.message}", throwable)

                    // **Enhanced error classification**
                    val errorMessage = throwable.message ?: "Terjadi kesalahan login"
                    when {
                        errorMessage.contains("Password salah", ignoreCase = true) -> {
                            LoginResult.Error("Password salah.")
                        }
                        errorMessage.contains("Pengguna belum terdaftar", ignoreCase = true) -> {
                            LoginResult.Error("Pengguna belum terdaftar.")
                        }
                        errorMessage.contains("User membatalkan", ignoreCase = true) -> {
                            LoginResult.Error("Login dibatalkan oleh user")
                        }
                        else -> {
                            LoginResult.Error(errorMessage)
                        }
                    }
                }
            )

            _uiState.update { it.copy(loginState = loginOutcome, isConnecting = false) }
            if (loginOutcome is LoginResult.Success) {
                _uiState.update {
                    it.copy(
                        isLoggedIn = true,
                        fullName = loginOutcome.userData.fullName ?: it.fullName,
                        walletAddress = userWalletAddress
                    )
                }
            }
        }
    }

    private suspend fun performAppLogout(): LogoutResult {
        _uiState.update { it.copy(logoutState = LogoutResult.Loading) }

        val currentToken = PreferencesHelper.getJwtToken(context)
        var serverMessage = "Proses logout..."

        // Simpan wallet address  sebelum ter-reset function disconnectWallet
        val savedWalletAddress = ethereum.selectedAddress.takeIf { it.isNotEmpty() }
            ?: PreferencesHelper.getWalletAddress(context)

        return runCatching {
            var logoutTxDataFromMiddleware: String? = null
            var publicKeyForOnChainLogout = savedWalletAddress

            // Memastikan wallet masih terhubung untuk transaksi
            if (publicKeyForOnChainLogout.isNullOrEmpty()) {
                Log.w("MainViewModel_Logout", "Wallet tidak terhubung, tidak bisa melakukan logout on-chain")
                throw Exception("Wallet tidak terhubung untuk melakukan logout on-chain")
            }

            // Proses logout dari server jika ada token
            if (!currentToken.isNullOrEmpty()) {
                try {
                    val serverResponse = apiService.logoutUserFromServer("Bearer $currentToken")

                    // Check if token was refreshed by interceptor
                    val tokenAfterCall = PreferencesHelper.getJwtToken(context)
                    if (tokenAfterCall != currentToken) {
                        Log.d("MainViewModel_Logout", "✅ Token was automatically refreshed by interceptor!")
                        Log.d("MainViewModel_Logout", "Old token: ${currentToken?.take(20)}...")
                        Log.d("MainViewModel_Logout", "New token: ${tokenAfterCall?.take(20)}...")
                    }

                    if (serverResponse.success) {
                        Log.d("MainViewModel_Logout", "Logout sisi server berhasil: ${serverResponse.message}")
                        logoutTxDataFromMiddleware = serverResponse.logoutTransactionData

                        // Gunakan publicKey dari response server jika tersedia
                        if (!serverResponse.publicKey.isNullOrEmpty()) {
                            publicKeyForOnChainLogout = serverResponse.publicKey
                        }

                        serverMessage = serverResponse.message ?: "Token logout sisi server diproses. Silakan lanjutkan logout on-chain."
                    } else {
                        Log.w("MainViewModel_Logout", "Logout sisi server tidak sukses: ${serverResponse.message}")
                        serverMessage = serverResponse.message ?: "Gagal logout dari server."

                        // Jika server response tidak sukses, tetap lanjutkan proses cleanup
                        throw Exception(serverMessage)
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel_Logout", "Error saat logout dari server: ${e.message}", e)

                    // Jika error adalah karena token expired, masih bisa lanjut logout
                    if (e.message?.contains("expired", ignoreCase = true) == true ||
                        e.message?.contains("kedaluwarsa", ignoreCase = true) == true) {
                        Log.d("MainViewModel_Logout", "Token expired, lanjutkan proses logout on-chain")
                        serverMessage = "Token sudah kedaluwarsa. Logout dianggap berhasil."

                        // Tetap perlu data transaksi untuk logout on-chain
                        // Server middleware akan tetap memberikan logoutTransactionData meski token expired
                    } else {
                        // Untuk error lain, tetap lanjutkan cleanup tapi catat error
                        serverMessage = "Logout server gagal: ${e.message}. Melanjutkan cleanup lokal."
                    }
                }
            } else {
                serverMessage = "Tidak ada sesi server untuk di-logout (tidak ada token)."
                Log.d("MainViewModel_Logout", serverMessage)
            }

            // Proses logout on-chain jika ada data transaksi dan alamat wallet
            if (publicKeyForOnChainLogout.isNotEmpty() && !logoutTxDataFromMiddleware.isNullOrBlank()) {
                try {
                    Log.d("MainViewModel_Logout", "Melanjutkan dengan logout on-chain untuk: $publicKeyForOnChainLogout")

                    val contractAddress = RetrofitClient.SMART_CONTRACT_ADDRESS
                    if (contractAddress.isBlank() || contractAddress == "ALAMAT_SMART_CONTRACT_ANDA_ISI_DI_RETROFIT_CLIENT") {
                        throw Exception("Alamat smart contract belum dikonfigurasi.")
                    }

                    val txParams = mapOf(
                        "from" to publicKeyForOnChainLogout,
                        "to" to contractAddress,
                        "data" to logoutTxDataFromMiddleware
                    )

                    val request = EthereumRequest(
                        method = EthereumMethod.ETH_SEND_TRANSACTION.value,
                        params = listOf(txParams)
                    )

                    val transactionResult = ethereum.sendRequest(request)

                    when (transactionResult) {
                        is Result.Success.Item -> {
                            val txHash = transactionResult.value
                            if (txHash.isNotEmpty()) {
                                Log.d("MainViewModel_Logout", "Transaksi logout on-chain berhasil: $txHash")
                                serverMessage += " Logout on-chain berhasil dengan txHash: $txHash"
                            } else {
                                Log.w("MainViewModel_Logout", "Logout on-chain sukses tapi txHash kosong")
                                serverMessage += " Logout on-chain sukses tapi txHash tidak valid."
                            }
                        }
                        is Result.Error -> {
                            val error = transactionResult.error
                            Log.e("MainViewModel_Logout", "Transaksi logout on-chain GAGAL: ${error.message} (Code: ${error.code})")

                            if (error.code == 4001 || error.message.contains("user rejected", ignoreCase = true)) {
                                throw Exception("User membatalkan transaksi logout")
                            } else {
                                serverMessage += " Logout on-chain gagal: ${error.message}."
                            }
                        }
                        else -> {
                            Log.d("MainViewModel_Logout", "Logout on-chain completed with result: $transactionResult")
                            serverMessage += " Logout on-chain berhasil."
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel_Logout", "Error saat proses logout on-chain: ${e.message}", e)

                    // Jika user membatalkan, jangan lanjutkan disconnect
                    if (e.message?.contains("user rejected", ignoreCase = true) == true ||
                        e.message?.contains("User membatalkan", ignoreCase = true) == true) {
                        throw e
                    }

                    serverMessage += " Error logout on-chain: ${e.message}."
                }
            } else {
                if (publicKeyForOnChainLogout.isEmpty()) {
                    Log.d("MainViewModel_Logout", "Tidak ada alamat wallet untuk logout on-chain")
                    serverMessage += " Tidak ada alamat wallet untuk logout on-chain."
                }
                if (logoutTxDataFromMiddleware.isNullOrBlank()) {
                    Log.d("MainViewModel_Logout", "Tidak ada data transaksi untuk logout on-chain")
                    serverMessage += " Tidak ada data transaksi untuk logout on-chain."
                }
            }
            serverMessage
        }.fold(
            onSuccess = { message ->
                Log.d("MainViewModel_Logout", "Logout berhasil: $message")
                LogoutResult.Success(message)
            },
            onFailure = { throwable ->
                Log.e("MainViewModel_Logout", "Logout gagal: ${throwable.message}", throwable)
                LogoutResult.Error(throwable.message ?: "Terjadi kesalahan saat logout aplikasi")
            }
        )
    }

    private fun cleanupLocalSessionData(navigateToWallet: Boolean = false) {
        PreferencesHelper.clearJwtToken(context)
        PreferencesHelper.saveMetaMaskConnectionStatus(context, false)
        if (navigateToWallet) {
            PreferencesHelper.clearWalletAddress(context)
        }
        PreferencesHelper.clearUserFullName(context)

        _uiState.update {
            it.copy(
                walletAddress = if (navigateToWallet) null else it.walletAddress,
                fullName = null,
                isGuest = false,
                isLoggedIn = false,
                shouldShowWalletConnect = navigateToWallet,
                registrationState = RegistrationResult.Idle,
                loginState = LoginResult.Idle,
                logoutState = LogoutResult.Idle
            )
        }
        if (navigateToWallet) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.NavigateTo(Screen.WalletComponent.route))
            }
        }
    }

    private fun onRegisterSuccessAfterChain() {
        val userWalletAddress = ethereum.selectedAddress
        Log.d("MainViewModel", "onRegisterSuccessAfterChain dipanggil untuk: $userWalletAddress")
        if(userWalletAddress.isNotEmpty()){
            fetchUserDataFromPrefs()
        }
    }

    fun logoutAndDisconnect() {
        Log.d("MainViewModel", "logoutAndDisconnect dipanggil")
        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true) }

            try {
                val appLogoutOutcome = performAppLogout()
                _uiState.update { it.copy(logoutState = appLogoutOutcome) }

                when (appLogoutOutcome) {
                    is LogoutResult.Success -> {
                        sendUiMessage(appLogoutOutcome.message)

                        _uiState.update { it.copy(shouldCleanupAfterLogout = true) }
                    }
                    is LogoutResult.Error -> {
                        if (appLogoutOutcome.errorMessage.contains("User membatalkan", ignoreCase = true) ||
                            appLogoutOutcome.errorMessage.contains("user rejected", ignoreCase = true)) {
                            sendUiMessage("Logout dibatalkan oleh user")
                        } else {
                            sendUiMessage("Logout dengan error: ${appLogoutOutcome.errorMessage}")
                            _uiState.update { it.copy(shouldCleanupAfterLogout = true) }
                        }
                    }
                    else -> {
                        sendUiMessage("Proses logout selesai.")
                        _uiState.update { it.copy(shouldCleanupAfterLogout = true) }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error dalam logoutAndDisconnect: ${e.message}", e)
                sendUiMessage("Error logout: ${e.message}")
                _uiState.update { it.copy(shouldCleanupAfterLogout = true) }
            } finally {
                _uiState.update { it.copy(isConnecting = false) }
            }
        }
    }

    fun performCleanupAndNavigate() {
        cleanupLocalSessionData(navigateToWallet = false)
        disconnectWallet()
        _uiState.update { it.copy(shouldCleanupAfterLogout = false) }
    }

    private fun connectWallet() {
        Log.d("MainViewModel_Connect", "connectWallet DIPANGGIL")
        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true) }
            Log.d("MainViewModel_Connect", "Memulai ethereum.connect()")

            when (val result = ethereum.connect()) {
                is Result.Success -> {
                    val userWalletAddress = ethereum.selectedAddress
                    if (userWalletAddress.isNotEmpty()) {
                        Log.d("MainViewModel_Connect", "Alamat terdeteksi: '$userWalletAddress', memproses...")
                        PreferencesHelper.saveMetaMaskConnectionStatus(context, true)
                        PreferencesHelper.saveWalletAddress(context, userWalletAddress)  // Pastikan wallet address disimpan di sini
                        _uiState.update {
                            it.copy(
                                walletAddress = userWalletAddress,
                                shouldShowWalletConnect = false,
                                isGuest = false
                            )
                        }
                        _uiEvent.emit(UiEvent.NavigateTo(Screen.Login.route))
                    } else {
                        Log.e("MainViewModel_Connect", "Connect sukses tapi ethereum.selectedAddress kosong!")
                        sendUiMessage("Gagal mendapatkan alamat wallet setelah koneksi berhasil.")
                    }
                }
                is Result.Error -> {
                    val error = result.error
                    Log.e("MainViewModel_Connect", "ethereum.connect() ERROR: ${error.message} (Code: ${error.code})")

                    // Handle user cancellation
                    val errorMessage = when {
                        error.code == 4001 || error.message.contains("user rejected", ignoreCase = true) -> {
                            Log.d("MainViewModel_Connect", "User membatalkan koneksi wallet")
                            "Koneksi wallet dibatalkan."
                        }
                        error.message.contains("user denied", ignoreCase = true) -> {
                            "Koneksi wallet ditolak."
                        }
                        else -> {
                            "Terjadi kesalahan koneksi: ${error.message}"
                        }
                    }

                    sendUiMessage(errorMessage)
                }
            }

            _uiState.update { it.copy(isConnecting = false) }
            Log.d("MainViewModel_Connect", "connectWallet SELESAI, isConnecting=false")
        }
    }

    private fun disconnectWallet() {
        Log.d("MainViewModel", "disconnectWallet dipanggil: Membersihkan sesi & memutuskan koneksi SDK.")
        ethereum.disconnect(true)
        cleanupLocalSessionData(navigateToWallet = true)
        sendUiMessage("Koneksi wallet diputus!")
    }

    private fun handleGuestLogin() {
        PreferencesHelper.clearJwtToken(context)
        PreferencesHelper.saveMetaMaskConnectionStatus(context, false)
        PreferencesHelper.clearWalletAddress(context)
        PreferencesHelper.clearUserFullName(context)
        PreferencesHelper.saveUserRegistrationStatus(context, false)

        _uiState.update {
            it.copy(
                walletAddress = null,
                isConnecting = false,
                shouldShowWalletConnect = false,
                isGuest = true,
                fullName = "Tamu",
                isLoggedIn = false,
                registrationState = RegistrationResult.Idle,
                loginState = LoginResult.Idle,
                logoutState = LogoutResult.Idle
            )
        }
        PreferencesHelper.saveUserFullName(context, "Tamu")
        sendUiMessage("Masuk sebagai Tamu")
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.NavigateTo(Screen.Home.route))
        }
    }

    init {
        initializeConnectionState()
        val token = PreferencesHelper.getJwtToken(context)
        val connectedWallet = PreferencesHelper.getWalletAddress(context)
        if (!token.isNullOrEmpty()) {
            if (!connectedWallet.isNullOrEmpty()) {
                Log.d("MainViewModel_Init", "Token ditemukan. Mencoba fetch user data untuk memvalidasi sesi.")
                fetchUserDataFromPrefs {
                    Log.w("MainViewModel_Init", "Auth failure dari server saat init. Membersihkan sesi lokal (token & nama).")
                    cleanupLocalSessionData(navigateToWallet = false)
                    viewModelScope.launch {
                        _uiEvent.emit(UiEvent.NavigateTo(Screen.Login.route)) // Arahkan ke Login jika token tidak valid
                        _uiEvent.emit(UiEvent.Message("Sesi Anda telah berakhir. Silakan login kembali."))
                    }
                }
            } else {
                Log.w("MainViewModel_Init", "Token ada tapi wallet address tidak ada di prefs. Membersihkan sesi.")
                cleanupLocalSessionData(navigateToWallet = false)
            }
        } else if (!connectedWallet.isNullOrEmpty()){
            Log.d("MainViewModel_Init", "Tidak ada token, tapi wallet terhubung. Fetch user data untuk cek status on-chain.")

            viewModelScope.launch {
                _uiEvent.emit(UiEvent.NavigateTo(Screen.Login.route))
            }

            fetchUserDataFromPrefs()
        } else {
            Log.d("MainViewModel_Init", "Tidak ada token atau koneksi wallet saat init.")
        }
    }

    private fun initializeConnectionState() {
        viewModelScope.launch {
            val savedAddress = PreferencesHelper.getWalletAddress(context)
            if (PreferencesHelper.isMetaMaskConnected(context) && !savedAddress.isNullOrEmpty()) {
                _uiState.update {
                    it.copy(walletAddress = savedAddress, shouldShowWalletConnect = false)
                }
            } else {
                _uiState.update { it.copy(shouldShowWalletConnect = true) }
            }
        }
    }
}
