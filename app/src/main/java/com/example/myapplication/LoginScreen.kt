package com.example.myapplication

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.data.DataClassResponses
import com.example.myapplication.data.DataClassResponses.LoginResponse
import com.example.myapplication.data.LoginRequest
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
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val walletAddress = PreferencesHelper.getWalletAddress(context)

    var password by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(walletAddress) {
        if (walletAddress.isNullOrEmpty()) {
            Toast.makeText(context, "Wallet address tidak ditemukan, mohon hubungkan wallet Anda.", Toast.LENGTH_LONG).show()
            navController.navigate("walletComponent") {
                popUpTo("login") { inclusive = true }
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
        ) {
            Image(
                painter = painterResource(id = R.drawable.plant),
                contentDescription = "Logo Tanaman",
                modifier = Modifier
                    .size(130.dp)
                    .padding(bottom = 8.dp)
            )

            Text(
                text = "LOGIN",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.dark_green),
            )

            Text(
                text = "Silahkan Masuk!",
                fontSize = 14.sp,
                color = colorResource(id = R.color.black)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.green)),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(0.85f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (passwordError != null) passwordError = null
                            if (generalError != null) generalError = null
                        },
                        label = { Text("Kata Sandi", color = Color.Black) },
                        placeholder = { Text("Masukkan Kata Sandi") },
                        visualTransformation = PasswordVisualTransformation(),
                        textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        isError = passwordError != null || generalError != null,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                            errorContainerColor = Color.White,
                            errorTextColor = Color.Red,
                        )
                    )
                    if (passwordError != null) {
                        Text(
                            passwordError!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }

                    if (generalError != null) {
                        Text(
                            text = generalError!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }


                    Spacer(modifier = Modifier.height(30.dp))

                    // Login Button
                    Button(
                        onClick = {
                            passwordError = null
                            generalError = null

                            if (password.isBlank()) {
                                passwordError = "Kata sandi tidak boleh kosong."
                                return@Button
                            }

                            // Pindahkan validasi walletAddress ke awal
                            if (walletAddress.isNullOrEmpty()) {
                                Toast.makeText(context, "Wallet address tidak ditemukan. Silahkan hubungkan wallet Anda terlebih dahulu.", Toast.LENGTH_LONG).show()
                                navController.navigate("walletComponent") {
                                    popUpTo("login") { inclusive = true }
                                }
                                return@Button
                            }

                            val userLogin = LoginRequest(walletAddress, password)
                            Log.d("Login", "Wallet Address: $walletAddress, Password: $password")

                            RetrofitClient.apiService.loginUser(userLogin).enqueue(object : Callback<LoginResponse> {
                                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                                    if (response.isSuccessful) {
                                        Log.d("API", "Login Success: ${response.body()?.message}")
                                        val token = response.body()?.token ?: ""
                                        PreferencesHelper.saveJwtToken(context, token)
                                        Toast.makeText(context, "Berhasil masuk!", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                        navController.navigate("home") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    } else {
                                        val errorBodyString = response.errorBody()?.string()
                                        Log.e("API", "Login Failed: ${response.code()} - $errorBodyString")

                                        var displayErrorMessage = "Terjadi kesalahan saat masuk."
                                        try {
                                            val errorJson = JSONObject(errorBodyString)
                                            val backendMessage = errorJson.optString("message", "unknown_error")

                                            if (backendMessage == "Login gagal: Password salah") {
                                                displayErrorMessage = "Kredensial yang diinput tidak valid."
                                                generalError = displayErrorMessage
                                            } else if (backendMessage.contains("Login gagal: User not registered", ignoreCase = true)) { // Contoh: Jika backend Anda mengirim ini
                                                displayErrorMessage = "Akun tidak terdaftar. Silahkan daftar."
                                                generalError = displayErrorMessage
                                            }
                                            else {
                                                displayErrorMessage = backendMessage
                                                generalError = displayErrorMessage
                                            }
                                        } catch (e: Exception) {
                                            Log.e("API", "Error parsing error body: ${e.message}")
                                            generalError = "Kredensial yang diinput tidak valid."
                                        }
                                        Toast.makeText(context, displayErrorMessage, Toast.LENGTH_LONG).show()
                                    }
                                }

                                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                                    Log.e("API", "Error: ${t.message}", t)
                                    generalError = "Tidak dapat terhubung ke server."
                                    Toast.makeText(context, "Koneksi gagal: ${t.message}", Toast.LENGTH_LONG).show()
                                }
                            })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.dark_green)),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text("Masuk", fontSize = 14.sp, color = Color.White)
                    }

                    TextButton(onClick = onNavigateToRegister) {
                        Text(
                            "Belum memiliki akun? Daftar disini",
                            fontSize = 12.sp,
                            color = colorResource(id = R.color.purple)
                        )
                    }
                }
            }
        }
    }
}