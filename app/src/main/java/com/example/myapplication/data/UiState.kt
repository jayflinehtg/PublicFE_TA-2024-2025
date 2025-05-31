package com.example.myapplication.data

data class UiState(
    val shouldShowWalletConnect: Boolean = false,
    val isConnecting: Boolean = false,
    val walletAddress: String? = null,
//    val balance: String? = null,
    val isGuest: Boolean = false,
    val fullName: String? = null,
    val message: String? = null,
    val isLoggedIn: Boolean = false
)