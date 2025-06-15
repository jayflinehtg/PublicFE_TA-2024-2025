package com.example.myapplication

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.myapplication.data.PreferencesHelper
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.ArrowBack
import com.example.myapplication.data.DataClassResponses.EditPlantRequest
import com.example.myapplication.data.EditPlantResult
import com.example.myapplication.data.IPFSUploadResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlantScreen(
    navController: NavController,
    plantId: String,
    viewModel: PlantViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Observasi state dari ViewModel
    val plantUiState by viewModel.uiState
    val plantToEdit = plantUiState.selectedPlant
    val editPlantState = plantUiState.editPlantState
    val isFormEnabled = !plantUiState.isLoading

    val ipfsUploadState = plantUiState.ipfsUploadState

    // State untuk form
    var namaTanaman by remember { mutableStateOf("") }
    var namaLatin by remember { mutableStateOf("") }
    var komposisi by remember { mutableStateOf("") }
    var manfaat by remember { mutableStateOf("") }
    var dosis by remember { mutableStateOf("") }
    var caraPengolahan by remember { mutableStateOf("") }
    var efekSamping by remember { mutableStateOf("") }

    var ipfsHash by remember { mutableStateOf("") }

    // State lokal untuk gambar baru
    var newImageUri by remember { mutableStateOf<Uri?>(null) }

    // State lokal untuk validasi UI
    var namaTanamanError by remember { mutableStateOf<String?>(null) }
    var namaLatinError by remember { mutableStateOf<String?>(null) }
    var komposisiError by remember { mutableStateOf<String?>(null)}
    var manfaatError by remember { mutableStateOf<String?>(null)}
    var dosisError by remember { mutableStateOf<String?>(null)}
    var caraPengolahanError by remember { mutableStateOf<String?>(null)}
    var efekSampingError by remember { mutableStateOf<String?>(null)}
    var ipfsHashError by remember { mutableStateOf<String?>(null)}

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            newImageUri = uri
            // Reset IPFS state saat pilih gambar baru
            if (uri != null) {
                viewModel.resetIPFSUploadState()
            }
        }
    )

    // Efek untuk memuat data awal saat layar dibuka
    LaunchedEffect(plantId) {
        if (plantToEdit?.id != plantId) {
            viewModel.fetchPlantDetail(plantId, null)
        }
    }

    // Efek untuk mengisi form ketika data dari ViewModel berubah
    LaunchedEffect(plantToEdit) {
        plantToEdit?.let {
            namaTanaman = it.name
            namaLatin = it.namaLatin
            komposisi = it.komposisi
            manfaat = it.manfaat
            dosis = it.dosis
            caraPengolahan = it.caraPengolahan
            efekSamping = it.efekSamping
            ipfsHash = it.ipfsHash
        }
    }

    // Efek untuk menangani hasil operasi edit dari ViewModel
    LaunchedEffect(editPlantState) {
        when (editPlantState) {
            is EditPlantResult.Success -> {
                Toast.makeText(context, editPlantState.message, Toast.LENGTH_LONG).show()
                viewModel.resetEditPlantState()
                navController.popBackStack()
            }
            is EditPlantResult.Error -> {
                val errorMessage = editPlantState.errorMessage
                val toastMessage = when {
                    errorMessage.contains("User membatalkan", ignoreCase = true) ||
                            errorMessage.contains("dibatalkan oleh user", ignoreCase = true) ->
                        "Edit tanaman dibatalkan."
                    else -> errorMessage
                }
                Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
                viewModel.resetEditPlantState()
            }
            else -> {}
        }
    }

    // Hadle IPFS Upload state
    LaunchedEffect(ipfsUploadState) {
        when (ipfsUploadState) {
            is IPFSUploadResult.Success -> {
                ipfsHash = ipfsUploadState.cid
                Toast.makeText(context, "Gambar baru berhasil dikonfirmasi!", Toast.LENGTH_SHORT).show()
            }
            is IPFSUploadResult.Error -> {
                Toast.makeText(context, "Upload gagal: ${ipfsUploadState.errorMessage}", Toast.LENGTH_LONG).show()
                viewModel.resetIPFSUploadState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.plant),
                            contentDescription = "Plant Logo",
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Edit Data Tanaman",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )

                            Text("Tanaman Herbal", color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Kembali", tint = Color(0xFF2E7D32))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE6F1E9)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE6F1E9))
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Form Fields
                    FormField(label = "Nama Tanaman", value = namaTanaman, errorText = namaTanamanError) { namaTanaman = it; namaTanamanError = null }
                    FormField(label = "Nama Latin", value = namaLatin, errorText = namaLatinError) { namaLatin = it; namaLatinError = null }
                    FormField(label = "Komposisi", value = komposisi, errorText = komposisiError) { komposisi = it; komposisiError = null }
                    FormField(label = "Manfaat", value = manfaat, errorText = manfaatError) { manfaat = it; manfaatError = null }
                    FormField(label = "Dosis", value = dosis, errorText = dosisError) { dosis = it; dosisError = null }
                    FormField(label = "Cara Pengolahan", value = caraPengolahan, errorText = caraPengolahanError) { caraPengolahan = it; caraPengolahanError = null }
                    FormField(label = "Efek Samping", value = efekSamping, errorText = efekSampingError) { efekSamping = it; efekSampingError = null }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Gambar Tanaman Baru (Opsional)", color = Color.Black)
                    Button(
                        onClick = { pickImageLauncher.launch("image/*") },
                        enabled = isFormEnabled,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pilih Gambar Baru", color = Color.White)
                    }

                    // Tampilkan preview gambar baru jika dipilih
                    newImageUri?.let { uri ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when (ipfsUploadState) {
                                is IPFSUploadResult.Success -> "Preview Gambar Baru (Terkonfirmasi):"
                                is IPFSUploadResult.Loading -> "Preview Gambar Baru (Sedang Upload):"
                                else -> "Preview Gambar Baru:"
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
                            contentDescription = "Preview Gambar Baru",
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

                    Spacer(modifier = Modifier.height(12.dp))

                    newImageUri?.let { uri ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        viewModel.performUploadImage(uri)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, e.message ?: "Gagal mengunggah gambar.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = isFormEnabled && ipfsUploadState !is IPFSUploadResult.Success,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier.fillMaxWidth().height(50.dp),
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
                                    Text("Konfirmasi Gambar Baru", color = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- TOMBOL SIMPAN PERUBAHAN ---
                    Button(
                        onClick = {
                            var hasError = false

                            if (namaTanaman.isBlank()) {
                                namaTanamanError = "Nama Tanaman tidak boleh kosong."
                                hasError = true
                            }
                            if (namaLatin.isBlank()) {
                                namaLatinError = "Nama Latin tidak boleh kosong."
                                hasError = true
                            }
                            if (komposisi.isBlank()) {
                                komposisiError = "Komposisi tidak boleh kosong."
                                hasError = true
                            }
                            if (manfaat.isBlank()) {
                                manfaatError = "Manfaat tidak boleh kosong."
                                hasError = true
                            }
                            if (dosis.isBlank()) {
                                dosisError = "Dosis tidak boleh kosong."
                                hasError = true
                            }
                            if (caraPengolahan.isBlank()) {
                                caraPengolahanError = "Cara Pengolahan tidak boleh kosong."
                                hasError = true
                            }
                            if (efekSamping.isBlank()) {
                                efekSampingError = "Efek Samping tidak boleh kosong."
                                hasError = true
                            }

                            if (ipfsHash.isBlank()) {
                                ipfsHashError = "Gambar tanaman tidak boleh kosong."
                                hasError = true
                                Toast.makeText(context, "Gambar tanaman tidak boleh kosong.", Toast.LENGTH_LONG).show()
                            }

                            if (hasError) {
                                Toast.makeText(context, "Harap lengkapi semua data yang wajib diisi.", Toast.LENGTH_LONG).show()
                                return@Button
                            }

                            val request = EditPlantRequest(
                                plantId = plantId,
                                name = namaTanaman,
                                namaLatin = namaLatin,
                                komposisi = komposisi,
                                manfaat = manfaat,
                                dosis = dosis,
                                caraPengolahan = caraPengolahan,
                                efekSamping = efekSamping,
                                ipfsHash = ipfsHash
                            )
                            viewModel.performEditPlant(request)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        if (editPlantState is EditPlantResult.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Simpan Perubahan", color = Color.White)
                        }
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
    errorText: String?,
    onChange: (String) -> Unit
) {
    val maxChar = when (label) {
        "Cara Pengolahan" -> 1000
        "Manfaat" -> 1000
        "Efek Samping" -> 1000
        "Komposisi" -> 750
        else -> 255
    }

    val isExceedLimit = value.length > maxChar
    val displayErrorText = when {
        errorText != null -> errorText
        isExceedLimit -> "$label tidak boleh lebih dari $maxChar karakter"
        else -> null
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
        OutlinedTextField(
            value = value,
            onValueChange = { newText ->
                if (newText.length <= maxChar) {
                    onChange(newText)
                }
            },
            singleLine = false,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
            textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
            isError = displayErrorText != null, // Cek jika ada error yang perlu ditampilkan
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
        if (displayErrorText != null) { // Tampilkan error jika ada
            Text(
                text = displayErrorText,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}