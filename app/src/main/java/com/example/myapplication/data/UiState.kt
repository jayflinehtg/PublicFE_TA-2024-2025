package com.example.myapplication.data

sealed interface RegistrationResult {
    object Idle : RegistrationResult
    object Loading : RegistrationResult
    data class Success(val txHash: String, val message: String = "Registrasi berhasil!") : RegistrationResult
    data class AlreadyRegistered(val message: String) : RegistrationResult
    data class Error(val errorMessage: String) : RegistrationResult
}

sealed interface LoginResult {
    object Idle : LoginResult
    object Loading : LoginResult
    data class Success(val jwtToken: String, val userData: DataClassResponses.UserData, val txHash: String, val message: String = "Login berhasil!") : LoginResult
    data class Error(val errorMessage: String) : LoginResult
}

sealed interface LogoutResult {
    object Idle : LogoutResult
    object Loading : LogoutResult
    data class Success(val message: String) : LogoutResult
    data class Error(val errorMessage: String) : LogoutResult
}

data class UiState(
    val shouldShowWalletConnect: Boolean = false,
    val shouldCleanupAfterLogout: Boolean = false,
    val isConnecting: Boolean = false,
    val walletAddress: String? = null,
    val isGuest: Boolean = false,
    val fullName: String? = null,
    val isLoggedIn: Boolean = false,
    val registrationState: RegistrationResult = RegistrationResult.Idle,
    val loginState: LoginResult = LoginResult.Idle,
    val logoutState: LogoutResult = LogoutResult.Idle

)