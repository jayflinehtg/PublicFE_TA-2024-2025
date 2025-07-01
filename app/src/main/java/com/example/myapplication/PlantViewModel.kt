package com.example.myapplication

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AddPlantResult
import com.example.myapplication.data.CommentPlantResult
import com.example.myapplication.data.DataClassResponses
import com.example.myapplication.data.DataClassResponses.AddPlantRequest
import com.example.myapplication.data.DataClassResponses.CommentRequest
import com.example.myapplication.data.DataClassResponses.EditPlantRequest
import com.example.myapplication.data.DataClassResponses.LikeRequest
import com.example.myapplication.data.DataClassResponses.RatePlantRequest
import com.example.myapplication.data.DataClassResponses.RatedPlant
import com.example.myapplication.data.EditPlantResult
import com.example.myapplication.data.LikePlantResult
import com.example.myapplication.data.PaginatedPlantResponse
import com.example.myapplication.data.PlantResponse
import com.example.myapplication.data.PlantUiState
import com.example.myapplication.data.PreferencesHelper
import com.example.myapplication.data.RatePlantResult
import com.example.myapplication.data.UiEvent
import com.example.myapplication.exceptions.ViewModelValidationException
import com.example.myapplication.services.ApiService
import com.example.myapplication.services.RetrofitClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.metamask.androidsdk.EthereumFlow
import io.metamask.androidsdk.EthereumMethod
import io.metamask.androidsdk.EthereumRequest
import io.metamask.androidsdk.Result
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.myapplication.data.IPFSUploadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

@HiltViewModel
class PlantViewModel @Inject constructor(
    private val apiService: ApiService,
    private val ethereum: EthereumFlow,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = mutableStateOf(PlantUiState())
    val uiState: State<PlantUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<UiEvent>()

    fun getCurrentUserAddress(): String {
        return try {
            val userWalletAddress = ethereum.selectedAddress
            Log.d("PlantViewModel", "Current wallet address: '$userWalletAddress'")

            if (userWalletAddress.isNullOrEmpty()) {
                Log.w("PlantViewModel", "selectedAddress is empty, checking PreferencesHelper")

                val savedAddress = PreferencesHelper.getWalletAddress(context)
                Log.d("PlantViewModel", "savedAddress from preferences: '$savedAddress'")

                savedAddress ?: ""
            } else {
                userWalletAddress
            }
        } catch (e: Exception) {
            Log.e("PlantViewModel", "Error getting wallet address: ${e.message}")
            ""
        }
    }

    // Fungsi reset state untuk dipanggil dari UI setelah menangani hasil
    fun resetAddPlantState() { _uiState.value = _uiState.value.copy(addPlantState = AddPlantResult.Idle) }
    fun resetEditPlantState() { _uiState.value = _uiState.value.copy(editPlantState = EditPlantResult.Idle) }
    fun resetIPFSUploadState() { _uiState.value = _uiState.value.copy(ipfsUploadState = IPFSUploadResult.Idle) }
    fun resetLikePlantState() { _uiState.value = _uiState.value.copy(likePlantState = LikePlantResult.Idle) }
    fun resetRatePlantState() { _uiState.value = _uiState.value.copy(ratePlantState = RatePlantResult.Idle) }
    fun resetCommentPlantState() { _uiState.value = _uiState.value.copy(commentPlantState = CommentPlantResult.Idle) }

    // Fungi untuk Transaksi On-Chain
    private suspend fun sendPlantTransaction(transactionDataHex: String, actionNameForLog: String)
    : Result {
        val userWalletAddress = try {
            ethereum.selectedAddress.takeIf { !it.isNullOrEmpty() }
                ?: PreferencesHelper.getWalletAddress(context)
                ?: throw ViewModelValidationException("Tidak dapat mendapatkan alamat wallet untuk transaksi.")
        } catch (e: Exception) {
            Log.e("PlantViewModel_$actionNameForLog", "Error getting wallet address: ${e.message}")
            throw ViewModelValidationException("Tidak dapat mendapatkan alamat wallet untuk transaksi.")
        }

        Log.d("PlantViewModel_$actionNameForLog", "Menggunakan wallet address: $userWalletAddress")

        val contractAddress = RetrofitClient.SMART_CONTRACT_ADDRESS
        if (contractAddress.isBlank()) {
            throw ViewModelValidationException("Alamat smart contract belum dikonfigurasi.")
        }
        if (transactionDataHex.isBlank() || transactionDataHex == "0x") {
            throw ViewModelValidationException("Data transaksi dari server tidak valid.")
        }

        val txParams = mapOf("from" to userWalletAddress, "to" to contractAddress, "data" to transactionDataHex)
        val request = EthereumRequest(method = EthereumMethod.ETH_SEND_TRANSACTION.value, params = listOf(txParams))
        Log.d("PlantViewModel_$actionNameForLog", "Mengirim permintaan transaksi: $request")
        return try {
            val result = ethereum.sendRequest(request)

            // Log transaction result untuk debugging
            when (result) {
                is Result.Success.Item -> {
                    Log.d("PlantViewModel_$actionNameForLog", "Transaction successful: ${result.value}")
                }
                is Result.Success.ItemMap -> {
                    Log.d("PlantViewModel_$actionNameForLog", "Transaction successful (ItemMap): ${result.value}")
                }
                is Result.Success.Items -> {
                    Log.d("PlantViewModel_$actionNameForLog", "Transaction successful (Items): ${result.value}")
                }
                is Result.Error -> {
                    Log.e("PlantViewModel_$actionNameForLog", "Transaction failed: ${result.error.message}")
                }
            }

            result
        } catch (e: Exception) {
            Log.e("PlantViewModel_$actionNameForLog", "Exception during transaction: ${e.message}")
            throw e
        }
    }

    // Fungsi untuk mengambil fullName berdasarkan wallet address (owner)
    fun fetchOwnerFullName(ownerAddress: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getUserInfo(ownerAddress)
                if (response.success) {
                    _uiState.value = _uiState.value.copy(ownerFullName = response.userData.fullName ?: "Pengguna Tidak Dikenal")
                } else {
                    _uiState.value = _uiState.value.copy(ownerFullName = "Tidak Dapat Mengambil Nama")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(ownerFullName = "Error Mengambil Nama")
            }
        }
    }

    suspend fun fetchImageBitmapFromIpfs(cid: String): ImageBitmap? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("PlantViewModel_IPFS", "Fetching image from IPFS with CID: $cid")
                val responseBody = apiService.getFileFromIPFS(cid)
                val bytes = responseBody.bytes()
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                bitmap?.asImageBitmap()
            } catch (e: Exception) {
                Log.e("PlantViewModel_IPFS", "Gagal memuat gambar dari IPFS: ${e.message}")
                null
            }
        }
    }

    // ==================== Tambah Tanaman ====================
    fun performAddPlant(request: AddPlantRequest) {
        Log.d("PlantViewModel_Add", "performAddPlant untuk: ${request.name}")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(addPlantState = AddPlantResult.Loading, isLoading = true)
            val outcome = runCatching {

                val prepareResponse = try {
                    val currentToken = PreferencesHelper.getJwtToken(context)
                    if (currentToken.isNullOrEmpty()) {
                        throw ViewModelValidationException("Session expired. Silakan login ulang.")
                    }
                    apiService.prepareAddPlant("Bearer $currentToken", request)
                } catch (e: retrofit2.HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e("PlantViewModel_Add", "HTTP Error: ${e.code()} - $errorBody")

                    val errorMessage = try {
                        val errorJson = com.google.gson.JsonParser.parseString(errorBody ?: "")
                        errorJson.asJsonObject.get("message")?.asString ?: "Error tidak diketahui"
                    } catch (parseError: Exception) {
                        "Error dari server: ${e.message()}"
                    }

                    throw ViewModelValidationException("Gagal menambah tanaman: $errorMessage")
                } catch (e: Exception) {
                    Log.e("PlantViewModel_Add", "Network Error: ${e.message}")
                    throw ViewModelValidationException("Gagal terhubung ke server: ${e.message}")
                }

                if (!prepareResponse.success || prepareResponse.data?.transactionData == null) {
                    throw ViewModelValidationException(prepareResponse.message ?: "Gagal mempersiapkan data transaksi.")
                }

                val transactionDataHex = prepareResponse.data.transactionData

                Log.d("PlantViewModel_Add", "Memulai transaksi on-chain untuk add plant...")

                val transactionResult = sendPlantTransaction(transactionDataHex, "AddPlant")

                when(val specificResult = transactionResult) {
                    is Result.Success.Item -> {
                        val txHash = specificResult.value
                        if(txHash.isNotEmpty()) txHash else throw Exception("Add plant on-chain sukses tapi txHash tidak valid: $txHash")
                    }
                    is Result.Error -> {
                        val error = specificResult.error

                        if (error.code == 4001 || error.message.contains("user rejected", ignoreCase = true)) {
                            throw Exception("User membatalkan transaksi add plant")
                        } else {
                            throw Exception("Add plant ke blockchain gagal: ${error.message} (Code: ${error.code})")
                        }
                    }
                    else -> throw Exception("Tipe hasil sukses tidak terduga: $specificResult")
                }
            }.fold(
                onSuccess = { txHash -> AddPlantResult.Success(txHash, "ID_BARU_TIDAK_DIKETAHUI") },
                onFailure = { throwable ->
                    val errorMessage = throwable.message ?: "Terjadi kesalahan menambah tanaman"
                    Log.e("PlantViewModel_Add", "Add plant failed: $errorMessage")

                    when {
                        errorMessage.contains("Session expired", ignoreCase = true) -> {
                            AddPlantResult.Error("Session expired. Silakan login ulang.")
                        }
                        errorMessage.contains("User membatalkan", ignoreCase = true) -> {
                            AddPlantResult.Error("Penambahan tanaman dibatalkan oleh user")
                        }
                        else -> {
                            AddPlantResult.Error(errorMessage)
                        }
                    }
                }
            )

            _uiState.value = _uiState.value.copy(addPlantState = outcome, isLoading = false)

            if (outcome is AddPlantResult.Success) {
                Log.d("PlantViewModel_Add", "Add plant berhasil, refreshing plant list...")
                fetchPlantsByPage(1)
            }
        }
    }

    // ==================== Upload Gambar ====================
    suspend fun performUploadImage(imageUri: Uri): String {
        return withContext(Dispatchers.IO) {

            // TAMBAHAN: Update IPFS state ke Loading
            _uiState.value = _uiState.value.copy(ipfsUploadState = IPFSUploadResult.Loading)

            var tempFile: File? = null
            try {
                tempFile = with(context) {
                    val contentResolver = contentResolver
                    val inputStream = contentResolver.openInputStream(imageUri)
                    val fileName = "upload_${System.currentTimeMillis()}.jpg"
                    val file = File(cacheDir, fileName)
                    inputStream?.use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }

                    // Validasi ukuran file
                    val maxSizeInBytes = 2 * 1024 * 1024 // 2 MB
                    val minSizeInBytes = 150 * 1024  // 150 kb
                    if (file.length() > maxSizeInBytes) {
                        throw ViewModelValidationException("Ukuran gambar maksimal 2MB.")
                    }
                    if (file.length() < minSizeInBytes) {
                        throw ViewModelValidationException("Ukuran gambar minimal 150kb.")
                    }
                    file
                }

                val requestBody = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", tempFile.name, requestBody)

                Log.d("PlantViewModel_IPFS", "Mengunggah gambar...")

                // Error handling
                val response = try {
                    val currentToken = PreferencesHelper.getJwtToken(context)
                    apiService.uploadImage("Bearer $currentToken", filePart)
                } catch (e: retrofit2.HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e("PlantViewModel_IPFS", "HTTP Error: ${e.code()} - $errorBody")

                    val errorMessage = try {
                        val errorJson = com.google.gson.JsonParser.parseString(errorBody ?: "")
                        errorJson.asJsonObject.get("message")?.asString ?: "Error tidak diketahui"
                    } catch (parseError: Exception) {
                        "Error dari server: ${e.message()}"
                    }

                    throw ViewModelValidationException("Gagal upload ke IPFS: $errorMessage")
                } catch (e: Exception) {
                    Log.e("PlantViewModel_IPFS", "Network Error: ${e.message}")
                    throw ViewModelValidationException("Gagal terhubung ke IPFS: ${e.message}")
                }

                if (!response.success || response.cid.isNullOrEmpty()) {
                    throw ViewModelValidationException(response.message ?: "Gagal upload ke IPFS")
                }

                Log.d("PlantViewModel_IPFS", "Upload sukses, CID: ${response.cid}")

                // Update success state
                _uiState.value = _uiState.value.copy(ipfsUploadState = IPFSUploadResult.Success(response.cid))

                return@withContext response.cid

            } catch (e: Exception) {
                Log.e("PlantViewModel_IPFS", "Error saat upload ke IPFS: ${e.message}", e)

                // Update error state
                _uiState.value = _uiState.value.copy(ipfsUploadState = IPFSUploadResult.Error(e.message ?: "Upload failed"))

                throw e
            } finally {
                tempFile?.delete()
            }
        }
    }

    // ==================== Edit Tanaman =======================
    fun performEditPlant(request: EditPlantRequest) {
        Log.d("PlantViewModel_Edit", "performEditPlant untuk: ${request.plantId}")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(editPlantState = EditPlantResult.Loading, isLoading = true)
            val outcome = runCatching {

                val prepareResponse = try {
                    val currentToken = PreferencesHelper.getJwtToken(context)
                    if (currentToken.isNullOrEmpty()) {
                        throw ViewModelValidationException("Session expired. Silakan login ulang.")
                    }
                    apiService.prepareEditPlant("Bearer $currentToken", request.plantId, request)
                } catch (e: retrofit2.HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e("PlantViewModel_Edit", "HTTP Error: ${e.code()} - $errorBody")

                    val errorMessage = try {
                        val errorJson = com.google.gson.JsonParser.parseString(errorBody ?: "")
                        errorJson.asJsonObject.get("message")?.asString ?: "Error tidak diketahui"
                    } catch (parseError: Exception) {
                        "Error dari server: ${e.message()}"
                    }

                    throw ViewModelValidationException("Gagal edit tanaman: $errorMessage")
                } catch (e: Exception) {
                    Log.e("PlantViewModel_Edit", "Network Error: ${e.message}")
                    throw ViewModelValidationException("Gagal terhubung ke server: ${e.message}")
                }

                if (!prepareResponse.success || prepareResponse.data?.transactionData == null) {
                    throw ViewModelValidationException(prepareResponse.message ?: "Gagal mempersiapkan data transaksi.")
                }

                val transactionDataHex = prepareResponse.data.transactionData

                Log.d("PlantViewModel_Edit", "Memulai transaksi on-chain untuk edit plant...")

                val transactionResult = sendPlantTransaction(transactionDataHex, "EditPlant")

                when(val specificResult = transactionResult) {
                    is Result.Success.Item -> {
                        val txHash = specificResult.value
                        if(txHash.isNotEmpty()) txHash else throw Exception("Edit plant on-chain sukses tapi txHash tidak valid: $txHash")
                    }
                    is Result.Error -> {
                        val error = specificResult.error

                        // Handle user cancellation berdasarkan search results
                        if (error.code == 4001 || error.message.contains("user rejected", ignoreCase = true)) {
                            throw Exception("User membatalkan transaksi edit plant")
                        } else {
                            throw Exception("Edit plant ke blockchain gagal: ${error.message} (Code: ${error.code})")
                        }
                    }
                    else -> throw Exception("Tipe hasil sukses tidak terduga: $specificResult")
                }
            }.fold(
                onSuccess = { txHash -> EditPlantResult.Success(txHash, request.plantId) },
                onFailure = { throwable ->
                    val errorMessage = throwable.message ?: "Terjadi kesalahan edit tanaman"
                    Log.e("PlantViewModel_Edit", "Edit plant failed: $errorMessage")

                    when {
                        errorMessage.contains("Session expired", ignoreCase = true) -> {
                            EditPlantResult.Error("Session expired. Silakan login ulang.")
                        }
                        errorMessage.contains("User membatalkan", ignoreCase = true) -> {
                            EditPlantResult.Error("Edit tanaman dibatalkan oleh user")
                        }
                        else -> {
                            EditPlantResult.Error(errorMessage)
                        }
                    }
                }
            )

            _uiState.value = _uiState.value.copy(editPlantState = outcome, isLoading = false)

            // Menggunakan token dari PreferencesHelper untuk refresh
            if (outcome is EditPlantResult.Success) {
                val currentToken = PreferencesHelper.getJwtToken(context)
                fetchPlantDetail(request.plantId, currentToken?.let { "Bearer $it" })
            }
        }
    }

    // ==================== Search Tanaman ========================
    fun searchPlants(
        name: String = "",
        namaLatin: String = "",
        komposisi: String = "",
        manfaat: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiService.searchPlants(name, namaLatin, komposisi, manfaat)
                val ratedResults = response.plants.map { plant ->
                    val ratingResponse = try { apiService.getAverageRating(plant.id) } catch (e: Exception) { null }
                    val avgRating = ratingResponse?.averageRating?: 0.0
                    RatedPlant(plant, avgRating)
                }
                // Update _uiState
                _uiState.value = _uiState.value.copy(
                    plantList = ratedResults,
                    totalPlants = ratedResults.size,
                    currentPage = 1,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("SearchPlant", "Gagal mencari tanaman: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false, plantList = emptyList())
            }
        }
    }

    // ==================== Pagination Tanaman ====================
    fun fetchPlantsByPage(page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true) // Set loading
            try {
                // Panggil API untuk mendapatkan data halaman yang diminta
                val response = apiService.getPaginatedPlants(page, 10)

                if (response.success) {
                    // Proses setiap tanaman untuk mendapatkan rating rata-ratanya
                    val ratedPlants = response.plants.map { plant ->
                        val ratingResponse = try {
                            apiService.getAverageRating(plant.id)
                        } catch (e: Exception) {
                            Log.e("FetchPlants", "Gagal mendapatkan rating untuk plantId: ${plant.id}", e)
                            null
                        }
                        val avgRating = ratingResponse?.averageRating ?: 0.0
                        RatedPlant(plant, avgRating)
                    }

                    _uiState.value = _uiState.value.copy(
                        plantList = ratedPlants,
                        totalPlants = response.total,   // Update totalPlants
                        currentPage = response.currentPage, // Update currentPage
                        isLoading = false
                    )

                } else {
                    Log.e("Pagination", "Gagal mengambil tanaman (success=false)")
                    // Reset daftar jika gagal
                    _uiState.value = _uiState.value.copy(isLoading = false, plantList = emptyList())
                }
            } catch (e: Exception) {
                Log.e("Pagination", "Error saat mengambil tanaman: ${e.message}")
                // Reset daftar jika terjadi error
                _uiState.value = _uiState.value.copy(isLoading = false, plantList = emptyList())
            }
        }
    }

    fun fetchPlantDetail(plantId: String, token: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true) // Mulai loading
            try {
                // Mencari di daftar yang sudah ada terlebih dahulu untuk menghindari panggilan API
                val localPlant = _uiState.value.plantList.find { it.plant.id == plantId }

                if (localPlant != null) {
                    // Jika ditemukan di daftar, langsung update UI dari data yang ada
                    _uiState.value = _uiState.value.copy(
                        selectedPlant = localPlant.plant,
                        selectedRating = localPlant.averageRating
                    )
                    // Panggil juga fetchOwnerFullName
                    fetchOwnerFullName(localPlant.plant.owner)
                    Log.d("DetailViewModel", "Berhasil load detail tanaman dari data lokal.")

                } else {
                    // Jika tidak ditemukan, ambil dari API
                    val bearerToken = token?.let { if (!it.startsWith("Bearer ")) "Bearer $it" else it }
                    val response = apiService.getPlantById(plantId, bearerToken)

                    if (response.success) {
                        val plantData = response.plant
                        // Hitung rata-rata rating
                        val ratingTotal = plantData.ratingTotal.toDoubleOrNull() ?: 0.0
                        val ratingCount = plantData.ratingCount.toDoubleOrNull() ?: 0.0
                        val average = if (ratingCount > 0) ratingTotal / ratingCount else 0.0

                        // Update state dengan data dari API
                        _uiState.value = _uiState.value.copy(
                            selectedPlant = plantData,
                            selectedRating = average
                        )
                        // Ambil nama lengkap pemilik setelah mendapatkan data tanaman
                        fetchOwnerFullName(plantData.owner)
                        Log.d("DetailViewModel", "Berhasil load detail tanaman dari API.")
                    } else {
                        Log.e("DetailViewModel", "Gagal load detail tanaman dari API (success=false)")
                        _uiState.value = _uiState.value.copy(selectedPlant = null)
                    }
                }
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error saat memuat detail tanaman: ${e.message}")
                _uiState.value = _uiState.value.copy(selectedPlant = null) // Set null jika ada error
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false) // Selalu hentikan loading
            }
        }
    }

    fun performLikePlant(plantId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(likePlantState = LikePlantResult.Loading)
            toggleLikeLocally()
            val outcome = runCatching {

                // Error handling
                val prepareResponse = try {
                    val currentToken = PreferencesHelper.getJwtToken(context)
                    apiService.prepareLikePlant("Bearer $currentToken", LikeRequest(plantId))
                } catch (e: retrofit2.HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e("PlantViewModel_Like", "HTTP Error: ${e.code()} - $errorBody")

                    val errorMessage = try {
                        val errorJson = com.google.gson.JsonParser.parseString(errorBody ?: "")
                        errorJson.asJsonObject.get("message")?.asString ?: "Error tidak diketahui"
                    } catch (parseError: Exception) {
                        "Error dari server: ${e.message()}"
                    }

                    throw ViewModelValidationException("Gagal like tanaman: $errorMessage")
                } catch (e: Exception) {
                    Log.e("PlantViewModel_Like", "Network Error: ${e.message}")
                    throw ViewModelValidationException("Gagal terhubung ke server: ${e.message}")
                }

                if (!prepareResponse.success || prepareResponse.data?.transactionData == null) {
                    throw ViewModelValidationException(prepareResponse.message ?: "Gagal mempersiapkan data like.")
                }

                val transactionResult = sendPlantTransaction(prepareResponse.data.transactionData, "LikePlant")

                when(val specificResult = transactionResult) {
                    is Result.Success.Item -> {
                        val txHash = specificResult.value
                        if(txHash.isNotEmpty()) txHash else throw Exception("Like plant on-chain sukses tapi txHash tidak valid: $txHash")
                    }
                    is Result.Error -> {
                        val error = specificResult.error

                        if (error.code == 4001 || error.message.contains("user rejected", ignoreCase = true)) {
                            throw Exception("User membatalkan transaksi like plant")
                        } else {
                            throw Exception("Like plant ke blockchain gagal: ${error.message} (Code: ${error.code})")
                        }
                    }
                    else -> throw Exception("Tipe hasil sukses tidak terduga: $specificResult")
                }
            }.fold(
                onSuccess = { txHash -> LikePlantResult.Success(txHash, plantId) },
                onFailure = { throwable ->
                    toggleLikeLocally() // Kembalikan state UI jika transaksi gagal
                    val errorMessage = throwable.message ?: "Terjadi kesalahan like tanaman"
                    Log.e("PlantViewModel_Like", "Like plant failed: $errorMessage")

                    when {
                        errorMessage.contains("User membatalkan", ignoreCase = true) -> {
                            LikePlantResult.Error("Like tanaman dibatalkan oleh user")
                        }
                        else -> {
                            LikePlantResult.Error(errorMessage)
                        }
                    }
                }
            )

            _uiState.value = _uiState.value.copy(likePlantState = outcome)

            // Refresh dengan token dari PreferencesHelper
            val currentToken = PreferencesHelper.getJwtToken(context)
            fetchPlantDetail(plantId, currentToken?.let { "Bearer $it" })
        }
    }

    fun toggleLikeLocally() {
        val currentPlant = _uiState.value.selectedPlant ?: return
        val isCurrentlyLiked = currentPlant.isLikedByUser
        val currentLikeCount = currentPlant.likeCount.toIntOrNull() ?: 0
        val newLikeCount = if (isCurrentlyLiked) currentLikeCount - 1 else currentLikeCount + 1

        _uiState.value = _uiState.value.copy(
            selectedPlant = currentPlant.copy(
                isLikedByUser = !isCurrentlyLiked,
                likeCount = newLikeCount.toString()
            )
        )
    }

    fun performCommentPlant(plantId: String, comment: String) {
        Log.d("PlantViewModel_Comment", "performCommentPlant untuk: $plantId")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(commentPlantState = CommentPlantResult.Loading)
            val outcome = runCatching {

                Log.d("PlantViewModel_Comment", "Backend will validate user from JWT token")

                val prepareResponse = try {
                    val currentToken = PreferencesHelper.getJwtToken(context)
                    if (currentToken.isNullOrEmpty()) {
                        throw ViewModelValidationException("Session expired. Silakan login ulang.")
                    }
                    apiService.prepareCommentPlant("Bearer $currentToken", CommentRequest(plantId, comment))
                } catch (e: retrofit2.HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e("PlantViewModel_Comment", "HTTP Error: ${e.code()} - $errorBody")

                    val errorMessage = try {
                        val errorJson = com.google.gson.JsonParser.parseString(errorBody ?: "")
                        errorJson.asJsonObject.get("message")?.asString ?: "Error tidak diketahui"
                    } catch (parseError: Exception) {
                        "Error dari server: ${e.message()}"
                    }

                    throw ViewModelValidationException("Gagal comment tanaman: $errorMessage")
                } catch (e: Exception) {
                    Log.e("PlantViewModel_Comment", "Network Error: ${e.message}")
                    throw ViewModelValidationException("Gagal terhubung ke server: ${e.message}")
                }

                if (!prepareResponse.success || prepareResponse.data?.transactionData == null) {
                    throw ViewModelValidationException(prepareResponse.message ?: "Gagal mempersiapkan data transaksi.")
                }

                val transactionDataHex = prepareResponse.data.transactionData

                Log.d("PlantViewModel_Comment", "Memulai transaksi on-chain untuk comment plant...")

                val transactionResult = sendPlantTransaction(transactionDataHex, "CommentPlant")

                when(val specificResult = transactionResult) {
                    is Result.Success.Item -> {
                        val txHash = specificResult.value
                        if(txHash.isNotEmpty()) txHash else throw Exception("Comment plant on-chain sukses tapi txHash tidak valid: $txHash")
                    }
                    is Result.Error -> {
                        val error = specificResult.error

                        if (error.code == 4001 || error.message.contains("user rejected", ignoreCase = true)) {
                            throw Exception("User membatalkan transaksi comment plant")
                        } else {
                            throw Exception("Comment plant ke blockchain gagal: ${error.message} (Code: ${error.code})")
                        }
                    }
                    else -> throw Exception("Tipe hasil sukses tidak terduga: $specificResult")
                }
            }.fold(
                onSuccess = { txHash -> CommentPlantResult.Success(txHash, plantId) },
                onFailure = { throwable ->
                    val errorMessage = throwable.message ?: "Terjadi kesalahan comment tanaman"
                    Log.e("PlantViewModel_Comment", "Comment plant failed: $errorMessage")

                    when {
                        errorMessage.contains("Session expired", ignoreCase = true) -> {
                            CommentPlantResult.Error("Session expired. Silakan login ulang.")
                        }
                        errorMessage.contains("User membatalkan", ignoreCase = true) -> {
                            CommentPlantResult.Error("Comment tanaman dibatalkan oleh user")
                        }
                        else -> {
                            CommentPlantResult.Error(errorMessage)
                        }
                    }
                }
            )

            _uiState.value = _uiState.value.copy(commentPlantState = outcome)

            if (outcome is CommentPlantResult.Success) {
                val currentToken = PreferencesHelper.getJwtToken(context)
                refreshPlantDetail(plantId, currentToken?.let { "Bearer $it" })
            }
        }
    }

    fun getPlantComments(plantId: String, page: Int = 1, limit: Int = 100) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiService.getPaginatedComments(plantId, page, limit)
                if (response.success) {
                    _uiState.value = _uiState.value.copy(plantComments = response.comments)
                } else {
                    Log.w("GetComments", "Gagal mengambil komentar (success=false). Respons: $response")
                    _uiState.value = _uiState.value.copy(plantComments = emptyList())
                }
            } catch (e: Exception) {
                Log.e("GetComments", "Error: ${e.message}")
                _uiState.value = _uiState.value.copy(plantComments = emptyList())
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun performRatePlant(plantId: String, rating: Int) {
        Log.d("PlantViewModel_Rate", "performRatePlant untuk: $plantId dengan rating: $rating")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ratePlantState = RatePlantResult.Loading)
            val outcome = runCatching {
                Log.d("PlantViewModel_Rate", "Backend will validate user from JWT token")

                val prepareResponse = try {
                    val currentToken = PreferencesHelper.getJwtToken(context)
                    if (currentToken.isNullOrEmpty()) {
                        throw ViewModelValidationException("Session expired. Silakan login ulang.")
                    }
                    apiService.prepareRatePlant("Bearer $currentToken", RatePlantRequest(plantId, rating))
                } catch (e: retrofit2.HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e("PlantViewModel_Rate", "HTTP Error: ${e.code()} - $errorBody")

                    val errorMessage = try {
                        val errorJson = com.google.gson.JsonParser.parseString(errorBody ?: "")
                        errorJson.asJsonObject.get("message")?.asString ?: "Error tidak diketahui"
                    } catch (parseError: Exception) {
                        "Error dari server: ${e.message()}"
                    }

                    throw ViewModelValidationException("Gagal rate tanaman: $errorMessage")
                } catch (e: Exception) {
                    Log.e("PlantViewModel_Rate", "Network Error: ${e.message}")
                    throw ViewModelValidationException("Gagal terhubung ke server: ${e.message}")
                }

                if (!prepareResponse.success || prepareResponse.data?.transactionData == null) {
                    throw ViewModelValidationException(prepareResponse.message ?: "Gagal mempersiapkan data transaksi.")
                }

                val transactionDataHex = prepareResponse.data.transactionData

                Log.d("PlantViewModel_Rate", "Memulai transaksi on-chain untuk rate plant...")

                val transactionResult = sendPlantTransaction(transactionDataHex, "RatePlant")

                when(val specificResult = transactionResult) {
                    is Result.Success.Item -> {
                        val txHash = specificResult.value
                        if(txHash.isNotEmpty()) txHash else throw Exception("Rate plant on-chain sukses tapi txHash tidak valid: $txHash")
                    }
                    is Result.Error -> {
                        val error = specificResult.error

                        if (error.code == 4001 || error.message.contains("user rejected", ignoreCase = true)) {
                            throw Exception("User membatalkan transaksi rate plant")
                        } else {
                            throw Exception("Rate plant ke blockchain gagal: ${error.message} (Code: ${error.code})")
                        }
                    }
                    else -> throw Exception("Tipe hasil sukses tidak terduga: $specificResult")
                }
            }.fold(
                onSuccess = { txHash -> RatePlantResult.Success(txHash, plantId) },
                onFailure = { throwable ->
                    val errorMessage = throwable.message ?: "Terjadi kesalahan rate tanaman"
                    Log.e("PlantViewModel_Rate", "Rate plant failed: $errorMessage")

                    when {
                        errorMessage.contains("Session expired", ignoreCase = true) -> {
                            RatePlantResult.Error("Session expired. Silakan login ulang.")
                        }
                        errorMessage.contains("User membatalkan", ignoreCase = true) -> {
                            RatePlantResult.Error("Rating tanaman dibatalkan oleh user")
                        }
                        else -> {
                            RatePlantResult.Error(errorMessage)
                        }
                    }
                }
            )

            _uiState.value = _uiState.value.copy(ratePlantState = outcome)

            if (outcome is RatePlantResult.Success) {
                val currentToken = PreferencesHelper.getJwtToken(context)
                fetchPlantDetail(plantId, currentToken?.let { "Bearer $it" })
            }
        }
    }

    // ==================== TRANSACTION HISTORY ====================
    private val _transactionHistory = mutableStateOf<List<DataClassResponses.TransactionHistoryItem>>(emptyList())
    val transactionHistory: State<List<DataClassResponses.TransactionHistoryItem>> = _transactionHistory

    private val _historyPagination = mutableStateOf<DataClassResponses.Pagination?>(null)
    val historyPagination: State<DataClassResponses.Pagination?> = _historyPagination

    private val _allPlantRecords = mutableStateOf<List<DataClassResponses.PlantRecord>>(emptyList())
    val allPlantRecords: State<List<DataClassResponses.PlantRecord>> = _allPlantRecords

    private val _recordCount = mutableStateOf("0")
    val recordCount: State<String> = _recordCount

    // Fungsi untuk mengambil transaction history berdasarkan plantId
    fun fetchPlantTransactionHistory(
        plantId: String,
        page: Int = 1,
        limit: Int = 10,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                Log.d("TransactionHistory", "Fetching history for plant $plantId, page $page")

                val response = apiService.getPlantTransactionHistory(plantId, page, limit)

                if (response.success) {
                    _transactionHistory.value = response.data
                    _historyPagination.value = response.pagination

                    Log.d("TransactionHistory", "Successfully fetched ${response.data.size} transactions")
                    onSuccess()
                } else {
                    Log.e("TransactionHistory", "Failed to fetch transaction history")
                    onError("Gagal mengambil riwayat transaksi")
                }
            } catch (e: Exception) {
                Log.e("TransactionHistory", "Error fetching transaction history: ${e.message}")
                onError("Error: ${e.message}")
            }
        }
    }

    // Fungsi untuk mengambil semua plant records
    fun fetchAllPlantRecords(
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                Log.d("AllRecords", "Fetching all plant records")

                val response = apiService.getAllPlantRecords()

                if (response.success) {
                    _allPlantRecords.value = response.records

                    Log.d("AllRecords", "Successfully fetched ${response.records.size} records")
                    onSuccess()
                } else {
                    Log.e("AllRecords", "Failed to fetch all plant records")
                    onError("Gagal mengambil semua record tanaman")
                }
            } catch (e: Exception) {
                Log.e("AllRecords", "Error fetching all plant records: ${e.message}")
                onError("Error: ${e.message}")
            }
        }
    }

    // Fungsi untuk mengambil single plant record berdasarkan recordId
    fun fetchPlantRecord(
        recordId: String,
        onSuccess: (DataClassResponses.PlantRecord) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("PlantRecord", "Fetching record with ID: $recordId")

                val response = apiService.getPlantRecord(recordId)

                if (response.success) {
                    Log.d("PlantRecord", "Successfully fetched record: ${response.record}")
                    onSuccess(response.record)
                } else {
                    Log.e("PlantRecord", "Failed to fetch plant record")
                    onError("Gagal mengambil record tanaman")
                }
            } catch (e: Exception) {
                Log.e("PlantRecord", "Error fetching plant record: ${e.message}")
                onError("Error: ${e.message}")
            }
        }
    }

    // Fungsi untuk mengambil total record count
    fun fetchRecordCount(
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                Log.d("RecordCount", "Fetching total record count")

                val response = apiService.getRecordCount()

                if (response.success) {
                    _recordCount.value = response.recordCount

                    Log.d("RecordCount", "Total records: ${response.recordCount}")
                    onSuccess(response.recordCount)
                } else {
                    Log.e("RecordCount", "Failed to fetch record count")
                    onError("Gagal mengambil jumlah record")
                }
            } catch (e: Exception) {
                Log.e("RecordCount", "Error fetching record count: ${e.message}")
                onError("Error: ${e.message}")
            }
        }
    }

    // Helper function untuk refresh transaction history (untuk pagination)
    fun refreshTransactionHistory(plantId: String, page: Int = 1) {
        fetchPlantTransactionHistory(plantId, page)
    }

    // Helper function untuk refresh data tanaman
    fun refreshPlantDetail(plantId: String, token: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            fetchPlantDetail(plantId, token)
            getPlantComments(plantId)
        }
    }
}
