package com.example.myapplication

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.myapplication.data.DataClassResponses
import com.example.myapplication.services.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    plantId: String,
    token: String,
    onBack: () -> Unit,
    onEdit: () -> Unit, // Fungsi untuk menavigasi ke screen edit
    viewModel: PlantViewModel = hiltViewModel()
) {
    val backgroundColor = Color(0xFFEAF4E9)
    val textColor = Color.Black
    val cardColor = Color.White
    val darkGreen = Color(0xFF498553)
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val plant by viewModel.selectedPlant
    val avgRating by viewModel.selectedRating
    val comments by viewModel.plantComments
    val ownerFullName by viewModel.ownerFullName
    val userAddress by viewModel.userAddress // Ambil userAddress dari viewModel

    var comment by remember { mutableStateOf("") }
    var likeCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var selectedRating by remember { mutableStateOf(3f) }
    val isLiked by remember(plant) {
        derivedStateOf { plant?.isLikedByUser == true }
    }
    var isLiking by remember { mutableStateOf(false) }

    val bitmapState = remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    fun formatTimestamp(timestamp: String): String {
        return try {
            val millis = timestamp.toLongOrNull()?.times(1000) ?: return timestamp
            val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id"))
            sdf.format(Date(millis))
        } catch (e: Exception) {
            timestamp
        }
    }

    suspend fun fetchBitmapFromIPFS(cid: String, apiService: ApiService): androidx.compose.ui.graphics.ImageBitmap? {
        return try {
            val responseBody = apiService.getFileFromIPFS(cid)
            val bytes = responseBody.bytes()
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    LaunchedEffect(plantId) {
        viewModel.fetchPlantDetail(plantId, token)
        viewModel.getPlantComments(plantId)
        plant?.owner?.let { ownerAddress ->
            // Mengambil fullName owner berdasarkan wallet address (owner)
            viewModel.fetchOwnerFullName(ownerAddress)
        }
    }

    LaunchedEffect(plant) {
        plant?.let {
            likeCount = it.likeCount.toIntOrNull() ?: 0
            isLoading = false
        }
    }

    LaunchedEffect(plant?.ipfsHash) {
        plant?.ipfsHash?.let { cid ->
            bitmapState.value = withContext(Dispatchers.IO) {
                fetchBitmapFromIPFS(cid, viewModel.apiServiceInstance)
            }
        }
    }

    if (showRatingDialog) {
        AlertDialog(
            onDismissRequest = { showRatingDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showRatingDialog = false
                    viewModel.ratePlant(token, plantId, selectedRating.toInt(),
                        onSuccess = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Rating berhasil dikirim!")
                                // Panggil refreshPlantDetail untuk mendapatkan data terbaru
                                viewModel.refreshPlantDetail(plantId, token)
                            }
                        },
                        onError = { errorMessage ->
                            scope.launch {
                                val message = when {
                                    errorMessage.contains("rating", ignoreCase = true) -> {
                                        "Rating berhasil diperbarui!"
                                    }
                                    errorMessage.contains("login", ignoreCase = true) -> {
                                        "Harap login terlebih dahulu untuk memberikan rating."
                                    }
                                    else -> {
                                        "Terjadi kesalahan, coba lagi nanti."
                                    }
                                }
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    )
                }) {
                    Text("Kirim")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRatingDialog = false }) {
                    Text("Batal")
                }
            },
            title = { Text("Beri Rating Tanaman") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pilih Rating:")
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        for (i in 1..5) {
                            IconButton(onClick = { selectedRating = i.toFloat() }) {
                                Icon(
                                    imageVector = if (i <= selectedRating.toInt()) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Bintang $i",
                                    tint = if (i <= selectedRating.toInt()) Color(0xFFFFC107) else Color.Gray, // Warna bintang aktif
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Rating: ${selectedRating.toInt()} ⭐")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Tanaman", fontSize = 20.sp, color = darkGreen, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = darkGreen)
                    }
                },
                actions = {
                    val animatedTint by animateColorAsState(
                        targetValue = if (isLiked) Color.Gray else Color.Red,
                        label = "likeColor"
                    )

                    IconButton(
                        onClick = {
                            if (!isLiking) {
                                isLiking = true
                                viewModel.likePlant(
                                    token = token,
                                    plantId = plantId,
                                    onSuccess = {
                                        scope.launch {
                                            viewModel.toggleLikeLocally() // Ubah state local dulu biar UI cepat berubah
                                            snackbarHostState.showSnackbar(
                                                if (isLiked) "Berhasil batal menyukai tanaman"
                                                else "Berhasil menyukai tanaman"
                                            )
                                            viewModel.refreshPlantDetail(plantId, token) // Baru sync server
                                            isLiking = false
                                        }
                                    },
                                    onError = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Harap login terlebih dahulu sebelum memberikan Like")
                                            isLiking = false
                                        }
                                    }
                                )
                            }
                        },
                        enabled = !isLiking
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isLiked) "Batal Like" else "Like",
                                tint = animatedTint,
                                modifier = Modifier.size(24.dp)
                            )
                            if (isLiking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 2.dp,
                                    color = animatedTint
                                )
                            }
                        }
                    }

                    // Menambahkan tombol Edit jika pemilik tanaman
                    if (plant?.owner?.lowercase() == userAddress?.lowercase()) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Tanaman", tint = Color.Blue)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(id = R.color.green),
                    titleContentColor = darkGreen,
                    navigationIconContentColor = darkGreen,
                    actionIconContentColor = Color.Red
                )
            )
        },
        bottomBar = {
            CommentInputSection(
                comment = comment,
                onCommentChange = { comment = it },
                onSendComment = {
                    if (comment.isNotBlank()) {
                        viewModel.commentPlant(token, plantId, comment,
                            onSuccess = {
                                comment = ""
                                focusManager.clearFocus()
                                viewModel.refreshPlantDetail(plantId, token) // refresh semuanya
                                scope.launch {
                                    snackbarHostState.showSnackbar("Komentar berhasil ditambahkan.")
                                }
                            },
                            onError = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Harap login terlebih dahulu sebelum memberikan komentar")
                                }
                            }
                        )
                    } else {
                        focusManager.clearFocus()
                    }
                },
                backgroundColor = backgroundColor
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = darkGreen)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                plant?.let {
                    Text(it.name, fontSize = 26.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = textColor)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Owner: $ownerFullName (${it.owner})", color = textColor, fontWeight = FontWeight.Bold)

                    DetailItem("Nama Latin", it.namaLatin, textColor)
                    DetailItem("Komposisi", it.komposisi, textColor)
                    DetailItem("Kegunaan", it.kegunaan, textColor)
                    DetailItem("Dosis", it.dosis, textColor)
                    DetailItem("Cara Pengolahan", it.caraPengolahan, textColor)
                    DetailItem("Efek Samping", it.efekSamping, textColor)

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Rating Rata-rata: $avgRating", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = textColor)

                    if (plant?.isRatedByUser == false) {
                        Button(
                            onClick = { showRatingDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = darkGreen),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Beri Rating", color = Color.White)
                        }
                    } else {
                        Text("Kamu sudah memberi rating", color = Color.Gray, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    bitmapState.value?.let { bitmap ->
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Gambar Tanaman",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } ?: Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Memuat gambar...", color = Color.DarkGray)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Disukai oleh $likeCount pengguna", color = textColor, fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Komentar Pengguna:", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 18.sp, color = textColor)
                    Spacer(modifier = Modifier.height(8.dp))

                    comments.forEach { c ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = cardColor)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("${c.fullName} (${c.publicKey})", fontWeight = FontWeight.Bold, color = textColor)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(c.comment, color = textColor)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Waktu: ${formatTimestamp(c.timestamp)}", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                } ?: Text("Tanaman tidak ditemukan.", color = textColor)
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, color: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("$label:", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = color)
        Text(value, color = color)
    }
}

@Composable
fun CommentInputSection(
    comment: String,
    onCommentChange: (String) -> Unit,
    onSendComment: () -> Unit,
    backgroundColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = comment,
                onValueChange = onCommentChange,
                placeholder = { Text("Tambahkan Komentar Anda...", color = Color.Gray) },
                textStyle = TextStyle(color = Color.Black),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSendComment() }),
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Transparent)
            )
            IconButton(onClick = onSendComment) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Kirim Komentar",
                    tint = colorResource(id = R.color.dark_green)
                )
            }
        }
    }
}