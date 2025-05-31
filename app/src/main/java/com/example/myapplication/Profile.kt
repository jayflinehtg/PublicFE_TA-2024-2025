package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.navigation.NavController
import com.example.myapplication.data.EventSink
import com.example.myapplication.data.PreferencesHelper
import com.example.myapplication.data.UiEvent

@Composable
fun Profile(navController: NavController) {
    val viewModel: MainViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val walletAddress = PreferencesHelper.getWalletAddress(context).orEmpty()
    val isLoggedIn = uiState.isLoggedIn

    // Event handler navigasi dari ViewModel
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.NavigateTo -> {
                    navController.navigate(event.route) {
                        popUpTo("profile") { inclusive = true }
                    }
                }
                is UiEvent.Message -> {
                    // Tampilkan snackbar atau toast jika perlu
                }
            }
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
        Text(
            text = uiState.fullName ?: "Nama Pengguna",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.dark_green),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Public Key",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = walletAddress,
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Text(
            text = if (isLoggedIn) "Logged In" else "Logged Out",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isLoggedIn) Color.Green else Color.Red,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        if (!isLoggedIn) {
            Text(
                text = "Silahkan hubungkan ulang wallet Anda",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
        Button(
            onClick = { viewModel.logoutAndDisconnect() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth(0.6f),
            enabled = isLoggedIn
        ) {
            Text("Keluar", fontSize = 14.sp, color = Color.White)
        }
        uiState.message?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = when {
                    message.contains("Berhasil", ignoreCase = true) -> Color.Green
                    message.contains("Gagal", ignoreCase = true) -> Color.Red
                    else -> Color.Gray
                },
                fontSize = 14.sp
            )
        }
    }
}
