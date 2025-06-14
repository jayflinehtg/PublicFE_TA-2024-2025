package com.example.myapplication

import android.net.Uri
import android.widget.Toast
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.myapplication.MainViewModel
import com.example.myapplication.data.AddPlantResult
import com.example.myapplication.data.DataClassResponses.AddPlantRequest
import com.example.myapplication.data.IPFSResponse
import com.example.myapplication.data.IPFSUploadResult
import com.example.myapplication.data.PreferencesHelper
import com.example.myapplication.services.ApiService
import com.example.myapplication.services.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

@Composable
fun AddPlant(
    navController: NavController,
    viewModel: PlantViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var namaTanaman by remember { mutableStateOf("") }
    var namaLatin by remember { mutableStateOf("") }
    var komposisi by remember { mutableStateOf("") }
    var manfaat by remember { mutableStateOf("") }
    var dosis by remember { mutableStateOf("") }
    var caraPengolahan by remember { mutableStateOf("") }
    var efekSamping by remember { mutableStateOf("") }

    var gambarUri by remember { mutableStateOf<Uri?>(null) }
    var ipfsCid by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val plantUiState by viewModel.uiState
    val mainUiState by mainViewModel.uiState.collectAsState()
    val addPlantState = plantUiState.addPlantState

    val ipfsUploadState = plantUiState.ipfsUploadState

    val isLoggedIn = mainUiState.isLoggedIn
    val isFormEnabled = !plantUiState.isLoading && isLoggedIn

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        gambarUri = uri
        ipfsCid = ""
        // Reset IPFS state saat pilih gambar baru
        viewModel.resetIPFSUploadState()
    }

    LaunchedEffect(addPlantState) {
        when (addPlantState) {
            is AddPlantResult.Success -> {
                Toast.makeText(context, addPlantState.message, Toast.LENGTH_LONG).show()
                navController.navigate("home") {
                    popUpTo("addplant") { inclusive = true }
                }
                viewModel.resetAddPlantState()
            }
            is AddPlantResult.Error -> {
                val errorMessage = addPlantState.errorMessage
                val toastMessage = when {
                    errorMessage.contains("User membatalkan", ignoreCase = true) ||
                            errorMessage.contains("dibatalkan oleh user", ignoreCase = true) ->
                        "Penambahan tanaman dibatalkan."
                    else -> errorMessage
                }
                Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
                viewModel.resetAddPlantState()
            }
            else -> {}
        }
    }

    // Handle IPFS upload state
    LaunchedEffect(ipfsUploadState) {
        when (ipfsUploadState) {
            is IPFSUploadResult.Success -> {
                ipfsCid = ipfsUploadState.cid
                Toast.makeText(context, "Gambar berhasil dikonfirmasi!", Toast.LENGTH_SHORT).show()
            }
            is IPFSUploadResult.Error -> {
                Toast.makeText(context, "Upload gagal: ${ipfsUploadState.errorMessage}", Toast.LENGTH_LONG).show()
                viewModel.resetIPFSUploadState()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE6F1E9))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.plant),
                contentDescription = "PlantResponse Logo",
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Tambah Data Tanaman",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Text("Tanaman Herbal", fontSize = 16.sp, color = Color(0xFF4CAF50))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Form fields dengan teks hitam
                FormField("Nama Tanaman", namaTanaman, showError && namaTanaman.isBlank()) { namaTanaman = it }
                FormField("Nama Latin", namaLatin, showError && namaLatin.isBlank()) { namaLatin = it }
                FormField("Komposisi", komposisi, showError && komposisi.isBlank()) { komposisi = it }
                FormField("Manfaat", manfaat, showError && manfaat.isBlank()) { manfaat = it }
                FormField("Dosis", dosis, showError && dosis.isBlank()) { dosis = it }
                FormField("Cara Pengolahan", caraPengolahan, showError && caraPengolahan.isBlank()) { caraPengolahan = it }
                FormField("Efek Samping", efekSamping, showError && efekSamping.isBlank()) { efekSamping = it }

                Spacer(modifier = Modifier.height(12.dp))

                // Judul "Gambar Tanaman"
                Text(
                    "Gambar Tanaman",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )

                // Button untuk memilih gambar
                Button(
                    onClick = { pickImageLauncher.launch("image/*") },
                    enabled = isFormEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pilih Gambar", color = Color.White)
                }

                // Menampilkan preview gambar kalau ada
                gambarUri?.let { uri ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when (ipfsUploadState) {
                            is IPFSUploadResult.Success -> "Preview Gambar (Terkonfirmasi):"
                            is IPFSUploadResult.Loading -> "Preview Gambar (Sedang Upload):"
                            else -> "Preview Gambar:"
                        },
                        fontWeight = FontWeight.Medium,
                        color = when (ipfsUploadState) {
                            is IPFSUploadResult.Success -> Color(0xFF4CAF50)
                            is IPFSUploadResult.Loading -> Color(0xFFFF9800)
                            else -> Color.Black
                        }
                    )
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = "Preview Gambar",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(4.dp)
                            .then(
                                // Visual indicator untuk status
                                when (ipfsUploadState) {
                                    is IPFSUploadResult.Success -> Modifier.background(
                                        Color(0xFF4CAF50).copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    else -> Modifier
                                }
                            )
                    )
                }

                // Menampilkan error kalau gambar tidak ada
                if (showError && gambarUri == null) {
                    Text(
                        text = "Gambar tidak boleh kosong",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Button Konfirmasi
                Button(
                    onClick = {
                        // Memastikan gambar sudah dipilih
                        gambarUri?.let { uri ->
                            scope.launch {
                                try {
                                    viewModel.performUploadImage(uri)
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message ?: "Gagal mengunggah gambar.", Toast.LENGTH_LONG).show()
                                }
                            }
                        } ?: Toast.makeText(context, "Pilih gambar terlebih dahulu.", Toast.LENGTH_SHORT).show()
                    },
                    enabled = isFormEnabled && gambarUri != null && ipfsCid.isEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier.fillMaxWidth().height(50.dp).padding(top = 8.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    when (ipfsUploadState) {
                        is IPFSUploadResult.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        }
                        is IPFSUploadResult.Success -> {
                            Text("Gambar Terkonfirmasi", color = Color.White)
                        }
                        else -> {
                            Text("Konfirmasi Gambar", color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tombol simpan
                Button(
                    onClick = {
                        // Validasi semua field, termasuk CID yang sudah dikonfirmasi
                        if (namaTanaman.isBlank() || namaLatin.isBlank() || komposisi.isBlank() ||
                            manfaat.isBlank() || caraPengolahan.isBlank() || ipfsCid.isBlank()) {
                            showError = true
                            Toast.makeText(context, "Mohon lengkapi semua data dan konfirmasi gambar", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        showError = false

                        val request = AddPlantRequest(
                            name = namaTanaman,
                            namaLatin = namaLatin,
                            komposisi = komposisi,
                            manfaat = manfaat,
                            dosis = dosis,
                            caraPengolahan = caraPengolahan,
                            efekSamping = efekSamping,
                            ipfsHash = ipfsCid
                        )
                        viewModel.performAddPlant(request)
                    },
                    enabled = isFormEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    if (addPlantState is AddPlantResult.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("Simpan", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun FormField(
    label: String,
    value: String,
    isError: Boolean,
    onChange: (String) -> Unit
) {
    // Menentukan maxChar berdasarkan label di dalam fungsi
    val maxChar = when (label) {
        "Cara Pengolahan" -> 1000
        "Manfaat" -> 1000
        "Efek Samping" -> 1000
        "Komposisi" -> 750
        else -> 255
    }

    val isExceedLimit = value.length > maxChar
    val errorText = when {
        isError -> "$label tidak boleh kosong"
        isExceedLimit -> "$label tidak boleh lebih dari $maxChar karakter"
        else -> ""
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black // Warna label hitam
        )
        OutlinedTextField(
            value = value,
            onValueChange = { newText ->
                // Hanya menerima input jika belum melebihi batas karakter
                if (newText.length <= maxChar) {
                    onChange(newText)
                }
            },
            singleLine = false,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
            textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
            isError = isError || isExceedLimit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            shape = RoundedCornerShape(12.dp),
            supportingText = {
                Text(
                    text = "${value.length}/$maxChar",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    fontSize = 12.sp,
                    color = if (isExceedLimit) Color.Red else Color.Gray
                )
            }
        )
        if (errorText.isNotEmpty()) {
            Text(
                text = errorText,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}