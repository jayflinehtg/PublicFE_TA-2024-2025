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
import com.example.myapplication.data.User
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.data.DataClassResponses
import com.example.myapplication.data.DataClassResponses.RegisterResponse
import com.example.myapplication.data.PreferencesHelper
import com.example.myapplication.services.RetrofitClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    onRegisterSuccess: (String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val walletAddress = PreferencesHelper.getWalletAddress(context) // Ini bisa String?

    var fullName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullNameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) } // Sudah benar

    LaunchedEffect(walletAddress) {
        if (walletAddress.isNullOrEmpty()) {
            navController.navigate("walletComponent") {
                popUpTo("register") { inclusive = true }
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
                text = "REGISTER",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.dark_green),
            )

            Text(
                text = "Daftar Akun Anda!",
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
                    // Input Nama Lengkap
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            if (fullNameError != null) fullNameError = null
                            if (generalError != null) generalError = null // <-- Tambahkan ini
                        },
                        label = { Text("Nama Lengkap", style = TextStyle(color = Color.Black)) },
                        placeholder = { Text("Masukkan Nama Lengkap", style = TextStyle(color = Color.Gray)) },
                        textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        isError = fullNameError != null || generalError != null, // <-- Perbaiki ini
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                            errorContainerColor = Color.White,
                            errorTextColor = Color.Red,
                        )
                    )
                    if (fullNameError != null) {
                        Text(
                            fullNameError!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input Kata Sandi
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (passwordError != null) passwordError = null
                            if (generalError != null) generalError = null // <-- Tambahkan ini
                        },
                        label = { Text("Kata Sandi", style = TextStyle(color = Color.Black)) },
                        placeholder = { Text("Masukkan Kata Sandi", style = TextStyle(color = Color.Gray)) },
                        visualTransformation = PasswordVisualTransformation(),
                        textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        isError = passwordError != null || generalError != null, // <-- Perbaiki ini
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
                    // <-- Tambahkan ini untuk menampilkan generalError
                    if (generalError != null) {
                        Text(
                            text = generalError!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tombol Daftar
                    Button(
                        onClick = {
                            fullNameError = null
                            passwordError = null
                            generalError = null // <-- Tambahkan ini

                            if (fullName.isBlank()) {
                                fullNameError = "Nama lengkap tidak boleh kosong."
                                return@Button
                            }

                            if (password.isBlank()) {
                                passwordError = "Kata sandi tidak boleh kosong."
                                return@Button
                            }
                            if (password.length < 6) {
                                passwordError = "Kata sandi minimal 6 karakter."
                                return@Button
                            }
                            val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+\$")
                            if (!password.matches(passwordRegex)) {
                                passwordError = "Kata sandi harus mengandung minimal satu huruf besar, satu huruf kecil, dan satu angka."
                                return@Button
                            }

                            // Gunakan variabel yang sudah ada
                            val currentWalletAddress = walletAddress // Ambil dari `walletAddress` di atas
                            if (currentWalletAddress.isNullOrEmpty()) {
                                Toast.makeText(context, "Wallet address tidak ditemukan. Silakan hubungkan wallet Anda terlebih dahulu.", Toast.LENGTH_LONG).show()
                                navController.navigate("walletComponent") {
                                    popUpTo("register") { inclusive = true }
                                }
                                return@Button
                            }

                            Log.d("Password", "Password yang dikirim: $password")

                            // Buat objek user dengan walletAddress yang diterima
                            val user = User(fullName, currentWalletAddress, password) // <-- Perbaiki ini

                            Log.d("WalletAddress", "Wallet Address: $currentWalletAddress")

                            // Panggil API untuk register
                            RetrofitClient.apiService.registerUser(user).enqueue(object : Callback<RegisterResponse> {
                                override fun onResponse(call: Call<RegisterResponse>, response: Response<DataClassResponses.RegisterResponse>) {
                                    if (response.isSuccessful) {
                                        Log.d("API", "Register Success: ${response.body()?.txHash}")
                                        Toast.makeText(context, "Registrasi berhasil!", Toast.LENGTH_SHORT).show()
                                        onRegisterSuccess(currentWalletAddress) // <-- Perbaiki ini
                                        navController.navigate("login") {
                                            popUpTo("register") { inclusive = true }
                                        }
                                    } else {
                                        val errorBodyString = response.errorBody()?.string()
                                        Log.e("API", "Register Failed: ${response.code()} - $errorBodyString")

                                        var displayErrorMessage = "Registrasi gagal, silakan coba lagi."
                                        try {
                                            val errorJson = JSONObject(errorBodyString)
                                            val backendMessage = errorJson.optString("message", "unknown_error")

                                            // Deteksi pesan error dari smart contract yang dibungkus middleware
                                            if (backendMessage.contains("Pendaftaran gagal: Akun sudah terdaftar", ignoreCase = true)) {
                                                displayErrorMessage = "Wallet Anda sudah terdaftar. Silakan login."
                                                generalError = displayErrorMessage
                                            } else if (backendMessage.contains("Pendaftaran gagal: Password tidak boleh kosong", ignoreCase = true)) {
                                                passwordError = "Kata sandi tidak boleh kosong." // Kata sandi, bukan sandiri
                                                displayErrorMessage = passwordError!!
                                            }
                                            else {
                                                displayErrorMessage = backendMessage
                                                generalError = displayErrorMessage
                                            }
                                        } catch (e: Exception) {
                                            Log.e("API", "Error parsing error body: ${e.message}")
                                            generalError = displayErrorMessage
                                        }
                                        Toast.makeText(context, displayErrorMessage, Toast.LENGTH_LONG).show()
                                    }
                                }

                                override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                                    Log.e("API", "Error: ${t.message}", t)
                                    generalError = "Tidak dapat terhubung ke server."
                                    Toast.makeText(context, "Koneksi gagal: ${t.message}", Toast.LENGTH_LONG).show()
                                }
                            })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text("Daftar", fontSize = 14.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Link Login
                    TextButton(onClick = {
                        navController.navigate("login") {
                            popUpTo("register") { inclusive = true } // Umumnya popUpTo ini inklusif jika Anda mau user langsung ke login
                        }
                    }) {
                        Text(
                            "Sudah memiliki akun? Masuk disini!",
                            fontSize = 12.sp,
                            color = colorResource(id = R.color.purple),
                        )
                    }
                }
            }
        }
    }
}