    package com.example.myapplication

    import android.util.Log
    import android.widget.Toast
    import androidx.compose.animation.animateColorAsState
    import androidx.compose.foundation.Image
    import androidx.compose.foundation.background
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.rememberScrollState
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.foundation.text.KeyboardActions
    import androidx.compose.foundation.text.KeyboardOptions
    import androidx.compose.foundation.verticalScroll
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.automirrored.filled.ArrowBack
    import androidx.compose.material.icons.automirrored.filled.ArrowForward
    import androidx.compose.material.icons.automirrored.filled.Send
    import androidx.compose.material.icons.filled.ArrowBack
    import androidx.compose.material.icons.filled.Edit
    import androidx.compose.material.icons.filled.Favorite
    import androidx.compose.material.icons.filled.FavoriteBorder
    import androidx.compose.material.icons.filled.History
    import androidx.compose.material.icons.filled.Star
    import androidx.compose.material.icons.outlined.StarBorder
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.graphics.ImageBitmap
    import androidx.compose.ui.graphics.graphicsLayer
    import androidx.compose.ui.layout.ContentScale
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.platform.LocalFocusManager
    import androidx.compose.ui.res.colorResource
    import androidx.compose.ui.text.TextStyle
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.input.ImeAction
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import androidx.hilt.navigation.compose.hiltViewModel
    import androidx.lifecycle.Lifecycle
    import androidx.lifecycle.LifecycleEventObserver
    import androidx.lifecycle.compose.LocalLifecycleOwner
    import androidx.lifecycle.viewModelScope
    import androidx.navigation.NavController
    import coil.compose.rememberAsyncImagePainter
    import com.example.myapplication.data.*
    import com.example.myapplication.data.DataClassResponses.Comment
    import com.example.myapplication.services.RetrofitClient
    import io.metamask.androidsdk.EthereumFlow
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.launch
    import java.text.SimpleDateFormat
    import java.util.*

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DetailScreen(
        plantId: String,
        onBack: () -> Unit,
        onEdit: () -> Unit,
        navController: NavController,
        viewModel: PlantViewModel = hiltViewModel()
    ) {
        val context = LocalContext.current
        val focusManager = LocalFocusManager.current

        val plantUiState by viewModel.uiState

        // Mendestrukturisasi state untuk kemudahan akses
        val plant = plantUiState.selectedPlant
        val avgRating = plantUiState.selectedRating
        val comments = plantUiState.plantComments
        val ownerFullName = plantUiState.ownerFullName

        val likePlantState = plantUiState.likePlantState
        val ratePlantState = plantUiState.ratePlantState
        val commentPlantState = plantUiState.commentPlantState

        val currentUserAddress = remember {
            viewModel.getCurrentUserAddress()
        }

        val lifecycleOwner = LocalLifecycleOwner.current

        var commentText by remember { mutableStateOf("") }
        var showRatingDialog by remember { mutableStateOf(false) }
        var selectedRatingState by remember { mutableStateOf(3f) }
        var bitmapState by remember { mutableStateOf<ImageBitmap?>(null) }

        val isLoggedIn = true

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        Log.d("DetailScreen", "App resumed - checking for data refresh")

                        // refresh saat kembali dari metamask
                        if (plant != null) {
                            Log.d("DetailScreen", "Force refreshing plant detail on resume")
                            viewModel.refreshPlantDetail(plantId, null)
                        }

                        // Handle stuck comment loading
                        if (commentPlantState is CommentPlantResult.Loading) {
                            Log.d("DetailScreen", "Comment loading detected on resume - applying timeout")
                            viewModel.viewModelScope.launch {
                                delay(2500) // Reduced timeout untuk faster response
                                if (viewModel.uiState.value.commentPlantState is CommentPlantResult.Loading) {
                                    Log.w("DetailScreen", "Comment timeout - resetting state and refreshing")
                                    viewModel.resetCommentPlantState()
                                    viewModel.refreshPlantDetail(plantId, null)
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

        // Handle untuk refresh data
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        Log.d("DetailScreen", "App resumed - checking for data refresh")
                        if (plant != null) {
                            viewModel.refreshPlantDetail(plantId, null)
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

        // Handle Like Plant
        LaunchedEffect(likePlantState) {
            when (likePlantState) {
                is LikePlantResult.Loading -> {
                    delay(10000) // 10 seconds timeout
                    if (likePlantState is LikePlantResult.Loading) {
                        Log.w("DetailScreen", "Like operation timeout - resetting state")
                        viewModel.resetLikePlantState()
                        viewModel.refreshPlantDetail(plantId, null)
                    }
                }
                is LikePlantResult.Success -> {
                    Toast.makeText(context, "Aksi like/unlike berhasil", Toast.LENGTH_SHORT).show()
                    viewModel.resetLikePlantState()
                    delay(2500) // Menununggu 3 detik
                    viewModel.refreshPlantDetail(plantId, null)
                }
                is LikePlantResult.Error -> {
                    val errorMessage = likePlantState.errorMessage
                    val toastMessage = when {
                        errorMessage.contains("User membatalkan", ignoreCase = true) ||
                                errorMessage.contains("dibatalkan oleh user", ignoreCase = true) ->
                            "Like tanaman dibatalkan."
                        else -> errorMessage
                    }
                    Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
                    viewModel.resetLikePlantState()
                }
                else -> {}
            }
        }

        // Handle Rate Plant
        LaunchedEffect(ratePlantState) {
            when (ratePlantState) {
                is RatePlantResult.Loading -> {
                    delay(10000) // 10 seconds timeout
                    if (ratePlantState is RatePlantResult.Loading) {
                        Log.w("DetailScreen", "Rate operation timeout - resetting state")
                        viewModel.resetRatePlantState()
                        viewModel.refreshPlantDetail(plantId, null)
                    }
                }
                is RatePlantResult.Success -> {
                    Toast.makeText(context, "Rating berhasil dikirim", Toast.LENGTH_SHORT).show()
                    viewModel.resetRatePlantState()
                    delay(3500)
                    viewModel.refreshPlantDetail(plantId, null)
                }
                is RatePlantResult.Error -> {
                    val errorMessage = ratePlantState.errorMessage
                    val toastMessage = when {
                        errorMessage.contains("User membatalkan", ignoreCase = true) ||
                                errorMessage.contains("dibatalkan oleh user", ignoreCase = true) ->
                            "Rating tanaman dibatalkan."
                        else -> errorMessage
                    }
                    Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
                    viewModel.resetRatePlantState()
                }
                else -> {}
            }
        }

        // Handle Comment Plant State
        LaunchedEffect(commentPlantState) {
            when (commentPlantState) {
                is CommentPlantResult.Success -> {
                    Toast.makeText(context, "Komentar berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    commentText = ""
                    focusManager.clearFocus()
                    viewModel.resetCommentPlantState()
                    delay(3500)
                    viewModel.refreshPlantDetail(plantId, null)
                }
                is CommentPlantResult.Loading -> {
                    launch {
                        delay(10000) // 10 detik timeout
                        if (commentPlantState is CommentPlantResult.Loading) {
                            Log.w("DetailScreen", "Comment loading timeout - resetting state")
                            viewModel.resetCommentPlantState()
                            viewModel.refreshPlantDetail(plantId, null)
                        }
                    }
                }
                is CommentPlantResult.Error -> {
                    val errorMessage = commentPlantState.errorMessage
                    val toastMessage = when {
                        errorMessage.contains("User membatalkan", ignoreCase = true) ||
                                errorMessage.contains("dibatalkan oleh user", ignoreCase = true) ->
                            "Comment tanaman dibatalkan."
                        else -> errorMessage
                    }
                    Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
                    viewModel.resetCommentPlantState()
                }
                else -> {}
            }
        }

        // Handle data tanaman
        LaunchedEffect(plantId) {
            if (plant == null || plant.id != plantId) {
                viewModel.refreshPlantDetail(plantId, null)
            }
        }

        // Memuat gambar dari IPFS
        LaunchedEffect(plant?.ipfsHash) {
            plant?.ipfsHash?.let { cid ->
                if (cid.isNotBlank()) {
                    bitmapState = viewModel.fetchImageBitmapFromIpfs(cid)
                }
            }
        }

        if (showRatingDialog) {
            RatingDialog(
                selectedRating = selectedRatingState,
                onRatingChange = { selectedRatingState = it },
                onDismiss = { showRatingDialog = false },
                onConfirm = {
                    showRatingDialog = false
                    viewModel.performRatePlant(plantId, selectedRatingState.toInt())
                }
            )
        }

        Scaffold(
            topBar = {
                DetailTopAppBar(
                    isLiked = plant?.isLikedByUser == true,
                    isLiking = likePlantState is LikePlantResult.Loading,
                    isOwner = plant?.owner?.equals(currentUserAddress, ignoreCase = true) == true,
                    isLoggedIn = isLoggedIn,
                    onBack = onBack,
                    onEdit = onEdit,
                    onLike = {
                        viewModel.performLikePlant(plantId)
                    }
                )
            },
            bottomBar = {
                CommentInputSection(
                    comment = commentText,
                    onCommentChange = { commentText = it },
                    onSendComment = {
                        if (commentText.isNotBlank()) {
                            viewModel.performCommentPlant(plantId, commentText)
                        }
                    },
                    isEnabled = isLoggedIn,
                    isLoading = commentPlantState is CommentPlantResult.Loading
                )
            }
        ) { innerPadding ->
            if (plantUiState.isLoading && plant == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (plant != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorResource(id = R.color.soft_green))
                        .verticalScroll(rememberScrollState())
                        .padding(innerPadding)
                        .padding(16.dp)
                ) {
                    Text(plant.name, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Owner: $ownerFullName (${plant.owner})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(16.dp))
                    DetailItem("Nama Latin", plant.namaLatin)
                    DetailItem("Komposisi", plant.komposisi)
                    DetailItem("Manfaat", plant.manfaat)
                    DetailItem("Dosis", plant.dosis)
                    DetailItem("Cara Pengolahan", plant.caraPengolahan)
                    DetailItem("Efek Samping", plant.efekSamping)

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Rating Rata-rata: ${String.format("%.1f", avgRating)} ⭐", fontWeight = FontWeight.Bold)

                    if (isLoggedIn) {
                        Button(
                            onClick = { showRatingDialog = true },
                            modifier = Modifier.padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF498553)),
                            enabled = ratePlantState !is RatePlantResult.Loading
                        ) {
                            if (ratePlantState is RatePlantResult.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                // Text yang menunjukkan user bisa update rating
                                Text(if (plant.isRatedByUser) "Update Rating" else "Beri Rating")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    bitmapState?.let {bitmap ->
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Gambar Tanaman",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .graphicsLayer {
                                    scaleX = -1f
                                },
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
                    Text("Disukai oleh ${plant.likeCount} pengguna", fontSize = 14.sp)

                    // Button lihat history
                    Button(
                        onClick = {
                            plant?.id?.let { plantId ->
                                navController.navigate("transaction_history/${plantId}")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History Icon",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lihat History Data Tanaman",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    CommentsSection(
                        comments = comments,
                        plantId = plantId,
                        viewModel = viewModel
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tanaman tidak ditemukan.")
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DetailTopAppBar(
        isLiked: Boolean,
        isLiking: Boolean,
        isOwner: Boolean,
        isLoggedIn: Boolean,
        onBack: () -> Unit,
        onEdit: () -> Unit,
        onLike: () -> Unit
    ) {
        TopAppBar(
            title = { Text("Detail Tanaman", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Kembali")
                }
            },
            actions = {
                val animatedTint by animateColorAsState(
                    targetValue = if (isLiked) Color.Red else Color.Gray,
                    label = "likeColorAnimation"
                )
                IconButton(onClick = onLike, enabled = !isLiking && isLoggedIn) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLiking) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Like",
                                tint = animatedTint
                            )
                        }
                    }
                }
                if (isOwner) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit,
                            contentDescription = "Edit Tanaman",
                            tint = Color.DarkGray
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colorResource(id = R.color.green))
        )
    }

    @Composable
    private fun RatingDialog(
        selectedRating: Float,
        onRatingChange: (Float) -> Unit,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onClick = onConfirm) { Text("Kirim") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
            title = { Text("Beri Rating Tanaman") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Pilih Rating:")
                    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        for (i in 1..5) {
                            IconButton(onClick = { onRatingChange(i.toFloat()) }) {
                                Icon(
                                    imageVector = if (i <= selectedRating.toInt()) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Bintang $i",
                                    tint = if (i <= selectedRating.toInt()) Color(0xFFFFC107) else Color.Gray,
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

    @Composable
    private fun DetailItem(label: String, value: String) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text("$label:", fontWeight = FontWeight.Bold)
            Text(value)
        }
    }

    @Composable
    private fun CommentsSection(
        comments: List<Comment>,
        plantId: String,
        viewModel: PlantViewModel
    ) {
        var currentPage by remember { mutableStateOf(1) }
        val commentsPerPage = 5
        val totalPages = if (comments.isEmpty()) 1 else (comments.size + commentsPerPage - 1) / commentsPerPage

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Komentar Pengguna:", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                // Pagination controls untuk komentar
                if (comments.size > commentsPerPage) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { currentPage = (currentPage - 1).coerceAtLeast(1) },
                            enabled = currentPage > 1
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous")
                        }

                        Text(
                            "$currentPage / $totalPages",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        IconButton(
                            onClick = { currentPage = (currentPage + 1).coerceAtMost(totalPages) },
                            enabled = currentPage < totalPages
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (comments.isEmpty()) {
                Text("Belum ada komentar.", modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray)
            } else {
                val startIndex = (currentPage - 1) * commentsPerPage
                val endIndex = (startIndex + commentsPerPage).coerceAtMost(comments.size)
                val paginatedComments = comments.subList(startIndex, endIndex)

                paginatedComments.forEach { comment ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "${comment.fullName} (${comment.publicKey})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                comment.comment,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                formatTimestamp(comment.timestamp),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Show total comments info
                if (comments.size > commentsPerPage) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Menampilkan ${startIndex + 1}-${endIndex} dari ${comments.size} komentar",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    @Composable
    private fun CommentInputSection(
        comment: String,
        onCommentChange: (String) -> Unit,
        onSendComment: () -> Unit,
        isEnabled: Boolean,
        isLoading: Boolean = false
    ) {
        Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 3.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = comment,
                    onValueChange = onCommentChange,
                    placeholder = { Text(if (isEnabled) "Tambahkan Komentar..." else "Login untuk berkomentar", color = Color.Gray) },
                    textStyle = TextStyle(color = Color.Black),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSendComment() }),
                    modifier = Modifier.weight(1f),
                    enabled = isEnabled && !isLoading
                )
                IconButton(onClick = onSendComment, enabled = isEnabled && !isLoading) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Kirim Komentar",
                            tint = if (isEnabled) colorResource(id = R.color.dark_green) else Color.Gray
                        )
                    }
                }
            }
        }
    }

    private fun formatTimestamp(timestamp: String): String {
        return try {
            val millis = timestamp.toLongOrNull()?.times(1000) ?: return timestamp
            val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))
            sdf.format(Date(millis))
        } catch (e: Exception) {
            timestamp
        }
    }