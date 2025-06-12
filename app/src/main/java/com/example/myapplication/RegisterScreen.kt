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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.data.PreferencesHelper
import com.example.myapplication.data.RegistrationResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: MainViewModel,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current

    var fullName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var fullNameErrorLocal by remember { mutableStateOf<String?>(null) }
    var passwordErrorLocal by remember { mutableStateOf<String?>(null) }

    val uiState by viewModel.uiState.collectAsState()
    val registrationState = uiState.registrationState
    val isFormEnabled = registrationState !is RegistrationResult.Loading && !uiState.isConnecting


    LaunchedEffect(uiState.walletAddress) {
        val walletAddress = uiState.walletAddress
        if (!walletAddress.isNullOrEmpty()) {
            viewModel.fetchUserDataFromPrefs {
                Toast.makeText(context, "Wallet Anda belum terdaftar, silakan registrasi.", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(uiState.walletAddress, uiState.isGuest) {
        if (uiState.walletAddress.isNullOrEmpty() && !uiState.isGuest) {
            Toast.makeText(context, "Wallet tidak terhubung. Silakan hubungkan wallet Anda.", Toast.LENGTH_LONG).show()
            navController.navigate(Screen.WalletComponent.route) {
                popUpTo(Screen.Register.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // Handle hasil operasi registrasi dari ViewModel
    LaunchedEffect(registrationState) {
        when (registrationState) {
            is RegistrationResult.Success -> {
                Toast.makeText(context, "Registrasi berhasil!", Toast.LENGTH_SHORT).show()
                viewModel.resetRegistrationState()
            }
            is RegistrationResult.AlreadyRegistered -> {
                Toast.makeText(context, registrationState.message, Toast.LENGTH_LONG).show()
                onNavigateToLogin()
                viewModel.resetRegistrationState()
            }
            is RegistrationResult.Error -> {
                val errorMessage = registrationState.errorMessage
                val toastMessage = when {
                    errorMessage.contains("sudah terdaftar", ignoreCase = true) ->
                        "Akun dengan alamat wallet ini sudah terdaftar. Silakan login."
                    // Handle user cancellation
                    errorMessage.contains("User membatalkan", ignoreCase = true) ||
                            errorMessage.contains("Registrasi dibatalkan", ignoreCase = true) ->
                        "Registrasi dibatalkan."
                    else -> errorMessage
                }
                Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()

                if (errorMessage.contains("sudah terdaftar", ignoreCase = true)) {
                    onNavigateToLogin()
                }
                viewModel.resetRegistrationState()
            }
            is RegistrationResult.Loading -> {
                Log.d("RegisterScreen", "RegistrationState adalah Loading.")
            }
            is RegistrationResult.Idle -> {
                Log.d("RegisterScreen", "RegistrationState adalah Idle.")
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
            Text(
                text = "REGISTER",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.dark_green)
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
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(0.90f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            fullNameErrorLocal = null
                        },
                        label = { Text("Nama Lengkap", style = TextStyle(color = Color.Black)) },
                        placeholder = { Text("Masukkan Nama Lengkap", style = TextStyle(color = Color.Gray)) },
                        textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        isError = fullNameErrorLocal != null,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            disabledContainerColor = Color.LightGray,
                            errorContainerColor = Color.White,
                            unfocusedIndicatorColor = Color.Gray,
                            focusedIndicatorColor = colorResource(id = R.color.dark_green)
                        ),
                        enabled = isFormEnabled
                    )
                    if (fullNameErrorLocal != null) {
                        Text(fullNameErrorLocal!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start).padding(start = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordErrorLocal = null
                        },
                        label = { Text("Kata Sandi", style = TextStyle(color = Color.Black)) },
                        placeholder = { Text("Masukkan Kata Sandi", style = TextStyle(color = Color.Gray)) },
                        visualTransformation = PasswordVisualTransformation(),
                        textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
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
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {

                            fullNameErrorLocal = null
                            passwordErrorLocal = null
                            viewModel.resetRegistrationState()

                            var hasValidationError = false
                            if (fullName.isBlank()) {
                                fullNameErrorLocal = "Nama lengkap tidak boleh kosong."
                                hasValidationError = true
                            }

                            if (password.isBlank()) {
                                passwordErrorLocal = "Kata sandi tidak boleh kosong."
                                hasValidationError = true
                            }
                            else if (password.length < 6) {
                                passwordErrorLocal = "Kata sandi minimal 6 karakter."
                                hasValidationError = true
                            }
                            else {
                                val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+\$")
                                if (!password.matches(passwordRegex)) {
                                    passwordErrorLocal = "Kata sandi harus mengandung huruf besar, kecil, dan angka."
                                    hasValidationError = true
                                }
                            }

                            if (hasValidationError) {
                            } else {
                                Log.d("RegisterScreen_Keyboard", "Memanggil viewModel.performRegistration dari keyboard action 'Done'")
                                viewModel.performRegistration(fullName, password)
                            }
                        }),
                        enabled = isFormEnabled
                    )
                    if (passwordErrorLocal != null) {
                        Text(passwordErrorLocal!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start).padding(start = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            fullNameErrorLocal = null
                            passwordErrorLocal = null
                            viewModel.resetRegistrationState()

                            var hasValidationError = false
                            if (fullName.isBlank()) {
                                fullNameErrorLocal = "Nama lengkap tidak boleh kosong."
                                hasValidationError = true
                            }
                            if (password.isBlank()) {
                                passwordErrorLocal = "Kata sandi tidak boleh kosong."
                                hasValidationError = true
                            } else if (password.length < 6) {
                                passwordErrorLocal = "Kata sandi minimal 6 karakter."
                                hasValidationError = true
                            } else {
                                val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+\$")
                                if (!password.matches(passwordRegex)) {
                                    passwordErrorLocal = "Kata sandi harus mengandung huruf besar, kecil, dan angka."
                                    hasValidationError = true
                                }
                            }

                            if (hasValidationError) {
                                return@Button
                            }

                            Log.d("RegisterScreen", "Memanggil viewModel.performRegistration")
                            viewModel.performRegistration(fullName, password)
                        },
                        enabled = isFormEnabled,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
                    ) {
                        if (registrationState is RegistrationResult.Loading || uiState.isConnecting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Daftar", fontSize = 16.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = onNavigateToLogin, enabled = isFormEnabled) {
                        Text(
                            "Sudah memiliki akun? Masuk disini!",
                            fontSize = 12.sp,
                            color = colorResource(id = R.color.purple),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}