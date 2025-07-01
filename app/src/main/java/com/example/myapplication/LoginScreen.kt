package com.example.myapplication

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.data.LoginResult
import com.example.myapplication.data.PreferencesHelper
import com.example.myapplication.services.RetrofitClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: MainViewModel,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current

    var password by remember { mutableStateOf("") }
    var passwordErrorLocal by remember { mutableStateOf<String?>(null) }

    val uiState by viewModel.uiState.collectAsState()
    val loginState = uiState.loginState
    val isFormEnabled = loginState !is LoginResult.Loading && !uiState.isConnecting

    // Pengecekan wallet address saat layar pertama kali muncul atau saat uiState.walletAddress berubah
    LaunchedEffect(uiState.walletAddress, uiState.isGuest) {
        if (uiState.walletAddress.isNullOrEmpty() && !uiState.isGuest) {
            Toast.makeText(context, "Wallet tidak terhubung. Silakan hubungkan wallet Anda.", Toast.LENGTH_LONG).show()
            navController.navigate(Screen.WalletComponent.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // Handle hasil operasi login dari ViewModel
    LaunchedEffect(loginState) {
        when (loginState) {
            is LoginResult.Success -> {
                Toast.makeText(context, "Berhasil masuk!", Toast.LENGTH_SHORT).show()
                Log.d("LoginScreen", "Login Sukses: ${loginState.message}")
                viewModel.resetLoginState()
            }
            is LoginResult.Error -> {
                val errorMessage = loginState.errorMessage
                Log.e("LoginScreen", "Login Error: ${errorMessage}")

                when {
                    errorMessage.contains("Password salah.", ignoreCase = true) -> {
                        passwordErrorLocal = "Password yang Anda masukkan salah."
                    }
                    errorMessage.contains("Pengguna belum terdaftar.", ignoreCase = true) -> {
                        Toast.makeText(context, "Akun belum terdaftar, silahkan daftar.", Toast.LENGTH_LONG).show()
                        onNavigateToRegister()
                    }
                    // Handle user cancellation
                    errorMessage.contains("Login dibatalkan oleh user", ignoreCase = true) -> {
                        Toast.makeText(context, "Login dibatalkan.", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
                viewModel.resetLoginState()
            }
            is LoginResult.Loading -> {
                Log.d("LoginScreen", "LoginState adalah Loading.")
            }
            is LoginResult.Idle -> {
                Log.d("LoginScreen", "LoginState adalah Idle.")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.soft_green)),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Image(
                painter = painterResource(id = R.drawable.plant),
                contentDescription = "Logo Tanaman",
                modifier = Modifier.size(130.dp).padding(bottom = 8.dp)
            )
            Text(text = "LOGIN", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = colorResource(id = R.color.dark_green))
            Text(text = "Silahkan Masuk!", fontSize = 14.sp, color = colorResource(id = R.color.black))
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.green)),
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(0.85f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordErrorLocal = null
                        },
                        label = { Text("Kata Sandi", color = Color.Black) },
                        placeholder = { Text("Masukkan Kata Sandi") },
                        visualTransformation = PasswordVisualTransformation(),
                        textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        isError = passwordErrorLocal != null,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            disabledContainerColor = Color.LightGray,
                            errorContainerColor = Color.White,
                            unfocusedIndicatorColor = Color.Gray,
                            focusedIndicatorColor = colorResource(id = R.color.dark_green)
                        ),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                passwordErrorLocal = null
                                viewModel.resetLoginState()

                                if (password.isBlank()) {
                                    passwordErrorLocal = "Kata sandi tidak boleh kosong."
                                } else {
                                    Log.d("LoginScreen_Keyboard", "Memanggil viewModel.performLogin dari keyboard action 'Done'")
                                    viewModel.performLogin(password)
                                }
                            }
                        ),
                        enabled = isFormEnabled
                    )
                    if (passwordErrorLocal != null) {
                        Text(passwordErrorLocal!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start).padding(start = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    Button(
                        onClick = {
                            passwordErrorLocal = null
                            viewModel.resetLoginState() // Reset state error sebelumnya

                            if (password.isBlank()) {
                                passwordErrorLocal = "Kata sandi tidak boleh kosong."
                                return@Button
                            }

                            // Wallet address akan diambil oleh ViewModel dari ethereum.selectedAddress
                            Log.d("LoginScreen_Button", "Memanggil viewModel.performLogin")
                            viewModel.performLogin(password)
                        },
                        enabled = isFormEnabled,
                        colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.dark_green)),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
                    ) {
                        if (loginState is LoginResult.Loading || uiState.isConnecting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Masuk", fontSize = 16.sp, color = Color.White)
                        }
                    }

                    TextButton(onClick = onNavigateToRegister, enabled = isFormEnabled) {
                        Text(
                            "Belum memiliki akun? Daftar disini",
                            fontSize = 12.sp,
                            color = colorResource(id = R.color.purple)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}