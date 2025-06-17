package com.example.myapplication

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // Import Modifier.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.myapplication.data.DataClassResponses.RatedPlant
import kotlin.math.roundToInt

@Composable
fun Home(
    navController: NavController,
    viewModel: PlantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState

    // State lokal hanya untuk kontrol UI seperti input teks
    var searchQuery by remember { mutableStateOf("") }
    var pageInput by remember { mutableStateOf(uiState.currentPage.toString()) }
    val focusManager = LocalFocusManager.current

    // Memuat data halaman pertama saat layar pertama kali muncul
    LaunchedEffect(Unit) {
        viewModel.fetchPlantsByPage(1)
    }

    // Efek untuk mengupdate input field halaman jika halaman berubah dari ViewModel
    LaunchedEffect(uiState.currentPage) {
        pageInput = uiState.currentPage.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEAF4E9))
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.plant),
                contentDescription = "Logo Tanaman",
                modifier = Modifier.size(70.dp).padding(end = 8.dp)
            )
            Column {
                Text(text = "Jelajahi Informasi", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFF498553))
                Text(text = "Tanaman Herbal", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF498553))
            }
        }

        // Pagination
        val pageSize = 10
        val totalPages = if (uiState.totalPlants == 0) 1 else (uiState.totalPlants + pageSize - 1) / pageSize

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            // Tombol Panah Kiri
            IconButton(
                onClick = {
                    val prevPage = (uiState.currentPage - 1).coerceAtLeast(1)
                    viewModel.fetchPlantsByPage(prevPage)
                },
                enabled = uiState.currentPage > 1
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Halaman Sebelumnya")
            }

            OutlinedTextField(
                value = pageInput,
                onValueChange = { pageInput = it.filter { char -> char.isDigit() } },
                label = { Text("Page", color = Color.Black) },
                singleLine = true,
                textStyle = TextStyle(color = Color.Black),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    focusManager.clearFocus()
                    val page = pageInput.toIntOrNull()?.coerceIn(1, totalPages) ?: 1
                    viewModel.fetchPlantsByPage(page)
                }),
                modifier = Modifier.width(80.dp)
            )
            Text(
                text = "of $totalPages",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 8.dp),
                color = Color.Black
            )
            // Tombol Panah Kanan
            IconButton(
                onClick = {
                    val nextPage = (uiState.currentPage + 1).coerceAtMost(totalPages)
                    viewModel.fetchPlantsByPage(nextPage)
                },
                enabled = uiState.currentPage < totalPages // Hanya aktif jika bukan halaman terakhir
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Halaman Berikutnya")

            }

            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    focusManager.clearFocus()
                    val page = pageInput.toIntOrNull()?.coerceIn(1, totalPages) ?: 1
                    viewModel.fetchPlantsByPage(page)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) { Text("Go", color = Color.White) }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari Info Tanaman...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(unfocusedContainerColor = Color.White),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                    if (searchQuery.isBlank()) {
                        viewModel.fetchPlantsByPage(1)
                    } else {
                        viewModel.searchPlants(name = searchQuery, namaLatin = searchQuery, komposisi = searchQuery, manfaat = searchQuery)
                    }
                }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tampilkan loading indicator atau daftar tanaman
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.plantList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Tanaman tidak ditemukan.",
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.plantList) { ratedPlant ->
                    PlantCard(ratedPlant, navController)
                }
            }
        }
    }
}

@Composable
fun PlantCard(ratedPlant: RatedPlant, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
//            .clickable { navController.navigate(Screen.Detail.createRoute(ratedPlant.plant.id)) },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ratedPlant.plant.name,
                    color = Color(0xFF2E7D32),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StarRating(rating = ratedPlant.averageRating)
                    Spacer(modifier = Modifier.width(16.dp))
                    LikesDisplay(likeCount = ratedPlant.plant.likeCount.toIntOrNull() ?: 0)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { navController.navigate(Screen.Detail.createRoute(ratedPlant.plant.id)) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Detail", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun StarRating(rating: Double) {
    val validRating = rating.takeIf { !it.isNaN() && !it.isInfinite() } ?: 0.0

    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { index ->
            Box {
                // Bintang kosong
                Icon(
                    imageVector = Icons.Default.StarBorder,
                    contentDescription = "Star Rating Background",
                    tint = Color(0xFFE0E0E0),
                    modifier = Modifier.size(20.dp)
                )

                // Bintang terisi
                val fillPercentage = when {
                    validRating >= index + 1 -> 1f // Penuh
                    validRating > index -> (validRating - index).toFloat() // Sebagian
                    else -> 0f // Kosong
                }

                if (fillPercentage > 0) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star Rating Filled",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier
                            .size(20.dp)
                            .clipToBounds()
                            .drawWithContent {
                                clipRect(right = size.width * fillPercentage) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "(${String.format("%.1f", validRating)})",
            fontSize = 12.sp,
            color = Color.DarkGray
        )
    }
}

@Composable
fun LikesDisplay(likeCount: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = "Like Count",
            tint = Color(0xFFE53935),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = likeCount.toString(),
            fontSize = 12.sp,
            color = Color.DarkGray
        )
    }
}