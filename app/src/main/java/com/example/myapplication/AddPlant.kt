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
import com.example.myapplication.data.DataClassResponses.AddPlantRequest
import com.example.myapplication.data.IPFSResponse
import com.example.myapplication.data.PreferencesHelper
import com.example.myapplication.services.ApiService
import com.example.myapplication.services.RetrofitClient
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
    apiService: ApiService = RetrofitClient.apiService
) {
    val context = LocalContext.current
    var namaTanaman by remember { mutableStateOf("") }
    var namaLatin by remember { mutableStateOf("") }
    var komposisi by remember { mutableStateOf("") }
    var manfaat by remember { mutableStateOf("") }
    var dosis by remember { mutableStateOf("") }
    var caraPengolahan by remember { mutableStateOf("") }
    var efekSamping by remember { mutableStateOf("") }

    var gambarUri by remember { mutableStateOf<Uri?>(null) }
    var showError by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    // Ambil CID dari ViewModel
    val cid = viewModel.cid.value

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> gambarUri = uri }

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

                // Judul "Gambar Tanaman" dengan warna hitam
                Text("Gambar Tanaman", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Black)

                // Button untuk memilih gambar
                Button(
                    onClick = { pickImageLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pilih Gambar", color = Color.White) // Warna teks pada button
                }

                // Menampilkan preview gambar jika ada
                gambarUri?.let { uri ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Preview Gambar:", fontWeight = FontWeight.Medium, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = "Preview Gambar Tanaman",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(4.dp)
                    )
                }

                // Menampilkan error jika gambar tidak ada
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
                        if (gambarUri == null) {
                            showError = true
                            return@Button
                        }

                        // Cek URI gambar sebelum upload
                        Log.d("AddIPFS", "Selected image URI: ${gambarUri?.path}")

                        showError = false
                        isUploading = true

                        val jwtTokenRaw = PreferencesHelper.getJwtToken(context)
                        val jwtToken = "Bearer ${jwtTokenRaw ?: ""}"

                        // Tambahkan log untuk melihat JWT Token
                        Log.d("AddIPFS", "JWT Token: $jwtToken")

                        val maxSizeInBytes = 5 * 1024 * 1024 // 5 MB dalam byte
                        val tempFile = gambarUri?.let { uri ->
                            try {
                                val contentResolver = context.contentResolver
                                val mimeType = contentResolver.getType(uri)

                                // Validasi MIME hanya gambar yang diperbolehkan
                                if (mimeType?.startsWith("image/") != true) {
                                    Toast.makeText(context, "Tipe file tidak valid, hanya gambar yang diperbolehkan", Toast.LENGTH_SHORT).show()
                                    return@let null // Kembalikan null jika file bukan gambar
                                }

                                val inputStream = contentResolver.openInputStream(uri)
                                val fileName = "uploaded_image_${System.currentTimeMillis()}.jpg"
                                val tempFile = File(context.cacheDir, fileName)

                                inputStream?.use { input ->
                                    FileOutputStream(tempFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }

                                // Mengecek ukuran file
                                if (tempFile.length() > maxSizeInBytes) {
                                    Log.e("AddIPFS", "File terlalu besar, maksimal 5MB")
                                    Toast.makeText(context, "File terlalu besar, maksimal 5MB", Toast.LENGTH_SHORT).show()
                                    return@let null // Kembalikan null jika file terlalu besar
                                } else {
                                    Log.d("AddIPFS", "File berhasil dibuat: ${tempFile.absolutePath}")
                                }

                                // Cek apakah file berhasil dibuat
                                if (tempFile.exists()) {
                                    Log.d("AddIPFS", "File berhasil dibuat di: ${tempFile.absolutePath}")
                                } else {
                                    Log.e("AddIPFS", "File tidak ada setelah disalin")
                                }

                                tempFile
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Log.e("AddIPFS", "Error saat membuat file: ${e.message}")
                                null
                            }
                        }

                        // Pastikan file tidak null sebelum upload
                        tempFile?.let { file ->
                            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                            val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)

                            apiService.uploadImage(jwtToken, filePart).enqueue(object : Callback<IPFSResponse> {
                                override fun onResponse(call: Call<IPFSResponse>, response: Response<IPFSResponse>) {
                                    Log.d("AddIPFS", "Response received: ${response.code()} ${response.body()}")
                                    if (!response.isSuccessful) {
                                        Log.e("AddIPFS", "Error: ${response.errorBody()?.string()}")
                                    }
                                    isUploading = false
                                    if (response.isSuccessful) {
                                        response.body()?.let { ipfsResponse ->
                                            val newCid = ipfsResponse.cid
                                            viewModel.setCid(newCid)  // Menyimpan CID ke ViewModel
                                            Toast.makeText(context, "Gambar berhasil diupload!", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Gagal mengunggah gambar ke IPFS", Toast.LENGTH_SHORT).show()
                                    }
                                    file.delete()
                                }

                                override fun onFailure(call: Call<IPFSResponse>, t: Throwable) {
                                    isUploading = false
                                    Log.e("AddIPFS", "Upload failed: ${t.message}")
                                    Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                                }
                            })
                        } ?: run {
                            // Menangani kasus ketika file tidak berhasil dibuat
                            Toast.makeText(context, "Gagal membuat file dari URI", Toast.LENGTH_SHORT).show()
                            isUploading = false
                        }
                    },
                    enabled = !isUploading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (isUploading) "Sedang Upload..." else "Konfirmasi Gambar",
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tombol simpan
                Button(
                    onClick = {
                        if (namaTanaman.isBlank() || namaLatin.isBlank() ||
                            komposisi.isBlank() || manfaat.isBlank() ||
                            caraPengolahan.isBlank() || cid.isBlank()
                        ) {
                            showError = true
                            Toast.makeText(context, "Mohon lengkapi semua data dan lakukan konfirmasi gambar", Toast.LENGTH_SHORT).show()
                        } else {
                            showError = false

                            val jwtTokenRaw = PreferencesHelper.getJwtToken(context)
                            val jwtToken = jwtTokenRaw?.let {
                                if (it.startsWith("Bearer ")) it else "Bearer $it"
                            } ?: ""

                            val request = AddPlantRequest(
                                name = namaTanaman,
                                namaLatin = namaLatin,
                                komposisi = komposisi,
                                kegunaan = manfaat,
                                dosis = dosis,
                                caraPengolahan = caraPengolahan,
                                efekSamping = efekSamping,
                                ipfsHash = cid
                            )

                            viewModel.addPlant(
                                token = jwtToken,
                                request = request,
                                onSuccess = { response ->
                                    Toast.makeText(context, "Tanaman berhasil ditambahkan!", Toast.LENGTH_SHORT).show()

                                    viewModel.syncPlantToPublic(response.plantId)
                                    viewModel.fetchPlantsByPage(1)
                                    navController.navigate("home"){
                                        popUpTo("addplant") { inclusive = true }
                                    }
                                },
                                onError = { errorMessage ->
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Simpan", color = Color.White)
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