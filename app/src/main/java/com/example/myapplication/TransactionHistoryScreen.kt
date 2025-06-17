package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.myapplication.data.DataClassResponses
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    plantId: String,
    navController: NavController,
    viewModel: PlantViewModel = hiltViewModel()
) {
    val backgroundColor = Color(0xFFEAF4E9)
    val darkGreen = Color(0xFF498553)
    val cardColor = Color.White

    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Observe data dari ViewModel
    val transactionHistory by viewModel.transactionHistory
    val pagination by viewModel.historyPagination
    var isLoading by remember { mutableStateOf(true) }

    // Load data saat screen pertama kali dibuka
    LaunchedEffect(plantId) {
        viewModel.fetchPlantTransactionHistory(
            plantId = plantId,
            page = 1,
            limit = 10,
            onSuccess = {
                isLoading = false
            },
            onError = { errorMessage ->
                isLoading = false
                scope.launch {
                    snackbarHostState.showSnackbar("Error: $errorMessage")
                }
            }
        )
    }

    // Detect scroll to bottom untuk pagination
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val totalItems = transactionHistory.size
                if (lastVisibleIndex != null &&
                    lastVisibleIndex >= totalItems - 2 &&
                    pagination?.hasNextPage == true &&
                    !isLoading) {

                    val nextPage = (pagination?.currentPage ?: 1) + 1
                    viewModel.fetchPlantTransactionHistory(
                        plantId = plantId,
                        page = nextPage,
                        limit = 10,
                        onSuccess = {},
                        onError = { errorMessage ->
                            scope.launch {
                                snackbarHostState.showSnackbar("Error loading more: $errorMessage")
                            }
                        }
                    )
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "History Data Tanaman",
                        fontSize = 20.sp,
                        color = darkGreen,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            tint = darkGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE8F5E8),
                    titleContentColor = darkGreen,
                    navigationIconContentColor = darkGreen
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Header info
            Text(
                text = "Riwayat perubahan data tanaman:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = darkGreen,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Pagination info
            pagination?.let { pag ->
                Text(
                    text = "Halaman ${pag.currentPage} dari ${pag.totalPages} • Total: ${pag.totalRecords} transaksi",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (isLoading && transactionHistory.isEmpty()) {
                // Loading state untuk data pertama
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = darkGreen)
                }
            } else if (transactionHistory.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada riwayat transaksi untuk tanaman ini",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                // Transaction list
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(transactionHistory) { record ->
                        TransactionHistoryItem(
                            record = record,
                            onCopyTxHash = { txHash ->
                                clipboardManager.setText(AnnotatedString(txHash))
                                scope.launch {
                                    snackbarHostState.showSnackbar("Transaction hash berhasil disalin!")
                                }
                            },
                            cardColor = cardColor,
                            darkGreen = darkGreen
                        )
                    }

                    // Loading indicator untuk pagination
                    if (pagination?.hasNextPage == true) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = darkGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Scroll untuk memuat lebih banyak...",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionHistoryItem(
    record: DataClassResponses.TransactionHistoryItem,
    onCopyTxHash: (String) -> Unit,
    cardColor: Color,
    darkGreen: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Transaction type dengan icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = record.icon,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = record.transactionType,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkGreen
                )
            }

            // TX Hash dengan copy button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tx Hash:",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = record.publicTxHash,
                        fontSize = 14.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = { onCopyTxHash(record.publicTxHash) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy Transaction Hash",
                        tint = darkGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Date
            Row(
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = "Tanggal: ",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = record.formattedTimestamp,
                    fontSize = 14.sp,
                    color = Color.Black
                )
            }

            // User Address
            Row {
                Text(
                    text = "Oleh: ",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${record.userAddress.take(10)}...${record.userAddress.takeLast(8)}",
                    fontSize = 14.sp,
                    color = Color.Black
                )
            }
        }
    }
}
