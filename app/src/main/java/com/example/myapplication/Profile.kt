package com.example.myapplication

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.myapplication.data.LogoutResult
import com.example.myapplication.data.PreferencesHelper
import com.example.myapplication.data.UiEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Profile(navController: NavController, viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val displayWalletAddress = uiState.walletAddress ?: PreferencesHelper.getWalletAddress(context).orEmpty()
    val isLoggedIn = uiState.isLoggedIn
    val logoutState = uiState.logoutState
    val isConnecting = uiState.isConnecting
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Log.d("ProfileScreen", "App resumed - checking logout state")

                    if (logoutState is LogoutResult.Success) {
                        Log.d("ProfileScreen", "Logout success detected on resume - navigating immediately")
                        navController.navigate("walletconnect") {
                            popUpTo(0) { inclusive = true }
                        }
                    }

                    // Handle stuck loading state
                    if (logoutState is LogoutResult.Loading) {
                        Log.d("ProfileScreen", "Logout loading detected on resume - applying timeout")
                        viewModel.viewModelScope.launch {
                            delay(3500) // 3.5 detik timeout
                            if (viewModel.uiState.value.logoutState is LogoutResult.Loading) {
                                Log.w("ProfileScreen", "Logout timeout - forcing navigation")
                                viewModel.resetLogoutState()
                                navController.navigate("walletconnect") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Event handler untuk pesan umum dan navigasi
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.NavigateTo -> {
                    Log.d("ProfileScreen", "Navigasi ke: ${event.route}")
                    if (event.route == Screen.WalletComponent.route && navController.currentDestination?.route == Screen.Profile.route) {
                        navController.navigate(event.route) {
                            popUpTo(Screen.Profile.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(event.route) {
                            launchSingleTop = true
                        }
                    }
                }
                is UiEvent.Message -> {
                    Log.d("ProfileScreen", "Menampilkan pesan: ${event.message}")
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(logoutState) {
        when (logoutState) {
            is LogoutResult.Success -> {
                Log.d("ProfileScreen", "Logout berhasil: ${logoutState.message}")
                Toast.makeText(context, "Logout berhasil", Toast.LENGTH_SHORT).show()
                viewModel.resetLogoutState()

                navController.navigate("walletconnect") {
                    popUpTo(0) { inclusive = true }
                }
            }
            is LogoutResult.Error -> {
                val errorMessage = logoutState.errorMessage
                Log.e("ProfileScreen", "Logout gagal: $errorMessage")

                val toastMessage = when {
                    errorMessage.contains("User membatalkan", ignoreCase = true) ||
                            errorMessage.contains("Logout dibatalkan", ignoreCase = true) ->
                        "Logout dibatalkan."
                    else -> "Logout gagal: $errorMessage"
                }

                Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
                viewModel.resetLogoutState()
            }
            is LogoutResult.Loading -> {
                Log.d("ProfileScreen", "Logout loading state active")
                launch {
                    delay(5000) // 10 seconds timeout
                    if (logoutState is LogoutResult.Loading) {
                        Log.w("ProfileScreen", "Logout loading timeout - forcing navigation")
                        viewModel.resetLogoutState()
                        navController.navigate("walletconnect") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.soft_green))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Header Profil
        Text(
            text = uiState.fullName ?: "Nama Pengguna",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.dark_green),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Public Key Section
        Text(
            text = "Public Key",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = displayWalletAddress.ifEmpty { "Wallet tidak terhubung" },
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Status Section
        Text(
            text = when {
                uiState.isGuest -> "Status: Mode Tamu"
                isLoggedIn -> "Status: Wallet Terhubung & telah Login"
                displayWalletAddress.isNotEmpty() -> "Status: Wallet Terhubung (Belum Login)"
                else -> "Status: Tidak Terhubung"
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = when {
                uiState.isGuest -> Color(0xFF2196F3) // Blue untuk tamu
                isLoggedIn -> colorResource(id = R.color.dark_green)
                displayWalletAddress.isNotEmpty() -> Color(0xFFFF9800) // Orange untuk terhubung tapi belum login
                else -> Color.Red
            },
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Info text untuk kasus khusus
        if (displayWalletAddress.isEmpty() && !uiState.isGuest) {
            Text(
                text = "Silakan hubungkan wallet Anda.",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        // Logout Button
        val shouldShowLogoutButton = isLoggedIn || displayWalletAddress.isNotEmpty()
        val isLogoutInProgress = logoutState is LogoutResult.Loading || (isConnecting && logoutState != LogoutResult.Idle)

        if (shouldShowLogoutButton) {
            Button(
                onClick = {
                    Log.d("ProfileScreen", "Tombol logout ditekan - isLoggedIn: $isLoggedIn, walletAddress: $displayWalletAddress")

                    when {
                        isLoggedIn -> {
                            Log.d("ProfileScreen", "Memulai proses logout dan disconnect...")
                            viewModel.logoutAndDisconnect()
                        }
                        displayWalletAddress.isNotEmpty() -> {
                            Log.d("ProfileScreen", "User terhubung tapi belum login, hanya disconnect wallet...")
                            viewModel.logoutAndDisconnect() // Tetap gunakan fungsi yang sama untuk konsistensi
                        }
                        else -> {
                            Log.w("ProfileScreen", "Kondisi tidak valid untuk logout")
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(50.dp),
                enabled = !isLogoutInProgress // Disable saat proses logout berlangsung
            ) {
                if (isLogoutInProgress) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Memproses...",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                } else {
                    Text(
                        text = when {
                            isLoggedIn -> "Keluar & Disconnect Wallet"
                            displayWalletAddress.isNotEmpty() -> "Disconnect Wallet"
                            else -> "Keluar"
                        },
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}