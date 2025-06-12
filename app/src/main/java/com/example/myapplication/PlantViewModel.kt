package com.example.myapplication

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AddPlantResult
import com.example.myapplication.data.CommentPlantResult
import com.example.myapplication.data.DataClassResponses
import com.example.myapplication.data.DataClassResponses.AddPlantRequest
import com.example.myapplication.data.DataClassResponses.CommentRequest
import com.example.myapplication.data.DataClassResponses.EditPlantRequest
import com.example.myapplication.data.DataClassResponses.LikeRequest
import com.example.myapplication.data.DataClassResponses.RatePlantRequest
import com.example.myapplication.data.DataClassResponses.RatedPlant
import com.example.myapplication.data.EditPlantResult
import com.example.myapplication.data.LikePlantResult
import com.example.myapplication.data.PaginatedPlantResponse
import com.example.myapplication.data.PlantResponse
import com.example.myapplication.data.PlantUiState
import com.example.myapplication.data.PreferencesHelper
import com.example.myapplication.data.RatePlantResult
import com.example.myapplication.data.UiEvent
import com.example.myapplication.exceptions.ViewModelValidationException
import com.example.myapplication.services.ApiService
import com.example.myapplication.services.RetrofitClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.metamask.androidsdk.EthereumFlow
import io.metamask.androidsdk.EthereumMethod
import io.metamask.androidsdk.EthereumRequest
import io.metamask.androidsdk.Result
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

@HiltViewModel
class PlantViewModel @Inject constructor(
    private val apiService: ApiService,
    private val ethereum: EthereumFlow,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = mutableStateOf(PlantUiState())
    val uiState: State<PlantUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    // Fungsi reset state untuk dipanggil dari UI setelah menangani hasil
    fun resetAddPlantState() { _uiState.value = _uiState.value.copy(addPlantState = AddPlantResult.Idle) }
    fun resetEditPlantState() { _uiState.value = _uiState.value.copy(editPlantState = EditPlantResult.Idle) }
    fun resetLikePlantState() { _uiState.value = _uiState.value.copy(likePlantState = LikePlantResult.Idle) }
    fun resetRatePlantState() { _uiState.value = _uiState.value.copy(ratePlantState = RatePlantResult.Idle) }
    fun resetCommentPlantState() { _uiState.value = _uiState.value.copy(commentPlantState = CommentPlantResult.Idle) }

    val apiServiceInstance get() = apiService

    private fun sendUiMessage(message: String) {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.Message(message))
        }
    }

    // Fungi untuk Transaksi On-Chain
    private suspend fun sendPlantTransaction(
        transactionDataHex: String,
        actionNameForLog: String
    ): Result {
        val userWalletAddress = PreferencesHelper.getWalletAddress(context) ?: ethereum.selectedAddress
        if (userWalletAddress.isEmpty()) throw ViewModelValidationException("Alamat wallet tidak tersedia.")

        val contractAddress = RetrofitClient.SMART_CONTRACT_ADDRESS
        if (contractAddress.isBlank()) {
            throw ViewModelValidationException("Alamat smart contract belum dikonfigurasi.")
        }
        if (transactionDataHex.isBlank() || transactionDataHex == "0x") {
            throw ViewModelValidationException("Data transaksi dari server tidak valid.")
        }

        val txParams = mapOf("from" to userWalletAddress, "to" to contractAddress, "data" to transactionDataHex)
        val request = EthereumRequest(method = EthereumMethod.ETH_SEND_TRANSACTION.value, params = listOf(txParams))
        Log.d("PlantViewModel_$actionNameForLog", "Mengirim permintaan transaksi: $request")
        return ethereum.sendRequest(request)
    }

    // Fungsi untuk mengambil fullName berdasarkan wallet address (owner)
    fun fetchOwnerFullName(ownerAddress: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getUserInfo(ownerAddress)
                if (response.success) {
                    _uiState.value = _uiState.value.copy(ownerFullName = response.userData.fullName ?: "Pengguna Tidak Dikenal")
                } else {
                    _uiState.value = _uiState.value.copy(ownerFullName = "Tidak Dapat Mengambil Nama")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(ownerFullName = "Error Mengambil Nama")
            }
        }
    }

    suspend fun fetchImageBitmapFromIpfs(cid: String): ImageBitmap? {
        // Pindahkan operasi jaringan dan decoding ke background thread (IO)
        return withContext(Dispatchers.IO) {
            try {
                Log.d("PlantViewModel_IPFS", "Fetching image from IPFS with CID: $cid")
                val responseBody = apiService.getFileFromIPFS(cid)
                // Decode byte stream menjadi bitmap, lalu ubah ke ImageBitmap untuk Compose
                BitmapFactory.decodeStream(responseBody.byteStream())?.asImageBitmap()
            } catch (e: Exception) {
                Log.e("PlantViewModel_IPFS", "Gagal memuat gambar dari IPFS: ${e.message}")
                null // Kembalikan null jika terjadi error
            }
        }
    }

    // ==================== Tambah Tanaman ====================
    fun performAddPlant(jwtToken: String, request: AddPlantRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(addPlantState = AddPlantResult.Loading, isLoading = true)
            val outcome = runCatching {

                val prepareResponse = apiService.prepareAddPlant(jwtToken, request)
                if (!prepareResponse.success || prepareResponse.data?.transactionData == null) {
                    throw ViewModelValidationException(prepareResponse.message ?: "Gagal mempersiapkan data tambah tanaman.")
                }

                val transactionResult = sendPlantTransaction(prepareResponse.data.transactionData, "AddPlant")

                when(val specificResult = transactionResult) {
                    is Result.Success.Item -> {
                        val txHash = specificResult.value
                        if(txHash.isNotEmpty()) AddPlantResult.Success(txHash, "ID_BARU_TIDAK_DIKETAHUI") else throw Exception("Transaksi tambah tanaman sukses tapi txHash tidak valid.")
                    }
                    is Result.Error -> throw Exception("Transaksi tambah tanaman ke blockchain gagal: ${specificResult.error.message}")
                    else -> throw Exception("Tipe hasil sukses tidak terduga: $specificResult")
                }
            }.fold(
                onSuccess = { it },
                onFailure = { AddPlantResult.Error(it.message ?: "Error tidak dikenal saat menambah tanaman.") }
            )
            _uiState.value = _uiState.value.copy(addPlantState = outcome, isLoading = false)
        }
    }

    // ==================== Upload Gambar ====================
    suspend fun performUploadImage(jwtToken: String, imageUri: Uri): String {
        var tempFile: File? = null
        try {
            tempFile = with(context) {
                val contentResolver = contentResolver
                val inputStream = contentResolver.openInputStream(imageUri)
                val fileName = "upload_${System.currentTimeMillis()}.jpg"
                val file = File(cacheDir, fileName)
                inputStream?.use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }

                val maxSizeInBytes = 2 * 1024 * 1024 // 5 MB
                val minSizeInBytes = 150 * 1024  // 150 kb
                if (file.length() > maxSizeInBytes) {
                    throw ViewModelValidationException("Ukuran gambar maksimal 5MB.")
                }
                if (file.length() > minSizeInBytes) {
                    throw ViewModelValidationException("Ukuran gambar minimal 150kb.")
                }
                file
            }

            val requestBody = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", tempFile.name, requestBody)

            Log.d("PlantViewModel_IPFS", "Mengunggah gambar...")
            val response = apiService.uploadImage(jwtToken, filePart) // Panggil suspend fun
            Log.d("PlantViewModel_IPFS", "Upload sukses, CID: ${response.cid}")
            return response.cid // Kembalikan CID jika sukses

        } catch (e: Exception) {
            Log.e("PlantViewModel_IPFS", "Error saat upload ke IPFS: ${e.message}", e)
            throw e // Lemparkan lagi exception agar bisa ditangkap oleh pemanggil
        } finally {
            tempFile?.delete()
        }
    }

    // ==================== Edit Tanaman =======================
    fun performEditPlant(jwtToken: String, request: EditPlantRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(editPlantState = EditPlantResult.Loading, isLoading = true)
            val outcome = runCatching {
                val prepareResponse = apiService.prepareEditPlant(jwtToken, request.plantId, request)
                if (!prepareResponse.success || prepareResponse.data?.transactionData == null) {
                    throw ViewModelValidationException(prepareResponse.message ?: "Gagal mempersiapkan data edit tanaman.")
                }

                val transactionResult = sendPlantTransaction(prepareResponse.data.transactionData, "EditPlant")

                when(val specificResult = transactionResult) {
                    is Result.Success.Item -> {
                        val txHash = specificResult.value
                        if(txHash.isNotEmpty()) EditPlantResult.Success(txHash, request.plantId) else throw Exception("Transaksi edit sukses tapi txHash tidak valid.")
                    }
                    is Result.Error -> throw Exception("Transaksi edit gagal: ${specificResult.error.message}")
                    else -> throw Exception("Tipe hasil sukses tidak terduga: $specificResult")
                }
            }.fold(
                onSuccess = { it },
                onFailure = { EditPlantResult.Error(it.message ?: "Error tidak dikenal saat edit tanaman.") }
            )
            _uiState.value = _uiState.value.copy(editPlantState = outcome, isLoading = false)
            if (outcome is EditPlantResult.Success) fetchPlantDetail(request.plantId, jwtToken)
        }
    }

    // ==================== Search Tanaman ========================
    fun searchPlants(
        name: String = "",
        namaLatin: String = "",
        komposisi: String = "",
        manfaat: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiService.searchPlants(name, namaLatin, komposisi, manfaat)
                val ratedResults = response.plants.map { plant ->
                    val ratingResponse = try { apiService.getAverageRating(plant.id) } catch (e: Exception) { null }
                    val avgRating = ratingResponse?.averageRating?.toDoubleOrNull() ?: 0.0
                    RatedPlant(plant, avgRating)
                }
                // Update _uiState
                _uiState.value = _uiState.value.copy(
                    plantList = ratedResults,
                    totalPlants = ratedResults.size,
                    currentPage = 1,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("SearchPlant", "Gagal mencari tanaman: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false, plantList = emptyList())
            }
        }
    }

    // ==================== Pagination Tanaman ====================

    fun fetchPlantsByPage(page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true) // Set loading
            try {
                // Panggil API untuk mendapatkan data halaman yang diminta
                val response = apiService.getPaginatedPlants(page, 10) // Asumsi pageSize = 10

                if (response.success) {
                    // Proses setiap tanaman untuk mendapatkan rating rata-ratanya
                    val ratedPlants = response.plants.map { plant ->
                        val ratingResponse = try {
                            apiService.getAverageRating(plant.id)
                        } catch (e: Exception) {
                            Log.e("FetchPlants", "Gagal mendapatkan rating untuk plantId: ${plant.id}", e)
                            null
                        }
                        val avgRating = ratingResponse?.averageRating?.toDoubleOrNull() ?: 0.0
                        RatedPlant(plant, avgRating)
                    }

                    _uiState.value = _uiState.value.copy(
                        plantList = ratedPlants,
                        totalPlants = response.total,   // Update totalPlants
                        currentPage = response.currentPage, // Update currentPage
                        isLoading = false
                    )

                } else {
                    Log.e("Pagination", "Gagal mengambil tanaman (success=false)")
                    // Reset daftar jika gagal
                    _uiState.value = _uiState.value.copy(isLoading = false, plantList = emptyList())
                }
            } catch (e: Exception) {
                Log.e("Pagination", "Error saat mengambil tanaman: ${e.message}")
                // Reset daftar jika terjadi error
                _uiState.value = _uiState.value.copy(isLoading = false, plantList = emptyList())
            }
        }
    }

    fun fetchPlantDetail(plantId: String, token: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true) // Mulai loading
            try {
                // Mencari di daftar yang sudah ada terlebih dahulu untuk menghindari panggilan API
                val localPlant = _uiState.value.plantList.find { it.plant.id == plantId }

                if (localPlant != null) {
                    // Jika ditemukan di daftar, langsung update UI dari data yang ada
                    _uiState.value = _uiState.value.copy(
                        selectedPlant = localPlant.plant,
                        selectedRating = localPlant.averageRating
                    )
                    // Panggil juga fetchOwnerFullName
                    fetchOwnerFullName(localPlant.plant.owner)
                    Log.d("DetailViewModel", "Berhasil load detail tanaman dari data lokal.")

                } else {
                    // Jika tidak ditemukan, ambil dari API
                    val bearerToken = token?.let { if (!it.startsWith("Bearer ")) "Bearer $it" else it }
                    val response = apiService.getPlantById(plantId, bearerToken)

                    if (response.success) {
                        val plantData = response.plant
                        // Hitung rata-rata rating
                        val ratingTotal = plantData.ratingTotal.toDoubleOrNull() ?: 0.0
                        val ratingCount = plantData.ratingCount.toDoubleOrNull() ?: 0.0
                        val average = if (ratingCount > 0) ratingTotal / ratingCount else 0.0

                        // Update state dengan data dari API
                        _uiState.value = _uiState.value.copy(
                            selectedPlant = plantData,
                            selectedRating = average
                        )
                        // Ambil nama lengkap pemilik setelah mendapatkan data tanaman
                        fetchOwnerFullName(plantData.owner)
                        Log.d("DetailViewModel", "Berhasil load detail tanaman dari API.")
                    } else {
                        Log.e("DetailViewModel", "Gagal load detail tanaman dari API (success=false)")
                        _uiState.value = _uiState.value.copy(selectedPlant = null)
                    }
                }
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error saat memuat detail tanaman: ${e.message}")
                _uiState.value = _uiState.value.copy(selectedPlant = null) // Set null jika ada error
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false) // Selalu hentikan loading
            }
        }
    }

    // Untuk Like tanaman
    fun performLikePlant(token: String, plantId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(likePlantState = LikePlantResult.Loading)
            toggleLikeLocally() // Optimistic UI update
            val outcome = runCatching {
                val prepareResponse = apiService.prepareLikePlant(token, LikeRequest(plantId))
                if (!prepareResponse.success || prepareResponse.data?.transactionData == null) {
                    throw ViewModelValidationException(prepareResponse.message ?: "Gagal mempersiapkan data like.")
                }

                val transactionResult = sendPlantTransaction(prepareResponse.data.transactionData, "LikePlant")

                when(val specificResult = transactionResult) {
                    is Result.Success.Item -> {
                        val txHash = specificResult.value
                        if(txHash.isNotEmpty()) LikePlantResult.Success(txHash, plantId) else throw Exception("Transaksi like sukses tapi txHash tidak valid.")
                    }
                    is Result.Error -> throw Exception("Transaksi like gagal: ${specificResult.error.message}")
                    else -> throw Exception("Tipe hasil sukses tidak terduga: $specificResult")
                }
            }.fold(
                onSuccess = { it },
                onFailure = {
                    toggleLikeLocally() // Kembalikan state UI jika transaksi gagal
                    LikePlantResult.Error(it.message ?: "Error tidak dikenal.")
                }
            )
            _uiState.value = _uiState.value.copy(likePlantState = outcome)
            // Selalu refresh data setelahnya untuk memastikan data on-chain terbaru
            fetchPlantDetail(plantId, token)
        }
    }

    fun toggleLikeLocally() {
        val currentPlant = _uiState.value.selectedPlant ?: return
        val isCurrentlyLiked = currentPlant.isLikedByUser
        val currentLikeCount = currentPlant.likeCount.toIntOrNull() ?: 0
        val newLikeCount = if (isCurrentlyLiked) currentLikeCount - 1 else currentLikeCount + 1

        _uiState.value = _uiState.value.copy(
            selectedPlant = currentPlant.copy(
                isLikedByUser = !isCurrentlyLiked,
                likeCount = newLikeCount.toString()
            )
        )
    }

    // Untuk Memberi Komentar Pada Tanaman
    fun performCommentPlant(token: String, plantId: String, comment: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(commentPlantState = CommentPlantResult.Loading)
            val outcome = runCatching {
                val prepareResponse = apiService.prepareCommentPlant(token, CommentRequest(plantId, comment))
                if (!prepareResponse.success || prepareResponse.data?.transactionData == null) {
                    throw ViewModelValidationException(prepareResponse.message ?: "Gagal mempersiapkan data komentar.")
                }

                val transactionResult = sendPlantTransaction(prepareResponse.data.transactionData, "CommentPlant")

                when(val specificResult = transactionResult) {
                    is Result.Success.Item -> {
                        val txHash = specificResult.value
                        if(txHash.isNotEmpty()) CommentPlantResult.Success(txHash, plantId) else throw Exception("Transaksi komentar sukses tapi txHash tidak valid.")
                    }
                    is Result.Error -> throw Exception("Transaksi komentar gagal: ${specificResult.error.message}")
                    else -> throw Exception("Tipe hasil sukses tidak terduga: $specificResult")
                }
            }.fold(
                onSuccess = { it },
                onFailure = { CommentPlantResult.Error(it.message ?: "Error tidak dikenal.") }
            )
            _uiState.value = _uiState.value.copy(commentPlantState = outcome)
            if (outcome is CommentPlantResult.Success) refreshPlantDetail(plantId, token)
        }
    }

    // Untuk mendapatkan komentar pada tanaman
    fun getPlantComments(plantId: String, page: Int = 1, limit: Int = 100) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiService.getPaginatedComments(plantId, page, limit)
                if (response.success) {
                    _uiState.value = _uiState.value.copy(plantComments = response.comments)
                } else {
                    Log.w("GetComments", "Gagal mengambil komentar (success=false). Respons: $response")
                    _uiState.value = _uiState.value.copy(plantComments = emptyList())
                }
            } catch (e: Exception) {
                Log.e("GetComments", "Error: ${e.message}")
                _uiState.value = _uiState.value.copy(plantComments = emptyList())
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    // Untuk Menambahkan Rating pada tanaman
    fun performRatePlant(token: String, plantId: String, rating: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ratePlantState = RatePlantResult.Loading)
            val outcome = runCatching {
                val prepareResponse = apiService.prepareRatePlant(token, RatePlantRequest(plantId, rating))
                if (!prepareResponse.success || prepareResponse.data?.transactionData == null) {
                    throw ViewModelValidationException(prepareResponse.message ?: "Gagal mempersiapkan data rating.")
                }

                val transactionResult = sendPlantTransaction(prepareResponse.data.transactionData, "RatePlant")

                when(val specificResult = transactionResult) {
                    is Result.Success.Item -> {
                        val txHash = specificResult.value
                        if(txHash.isNotEmpty()) RatePlantResult.Success(txHash, plantId) else throw Exception("Transaksi rating sukses tapi txHash tidak valid.")
                    }
                    is Result.Error -> throw Exception("Transaksi rating gagal: ${specificResult.error.message}")
                    else -> throw Exception("Tipe hasil sukses tidak terduga: $specificResult")
                }
            }.fold(
                onSuccess = { it },
                onFailure = { RatePlantResult.Error(it.message ?: "Error tidak dikenal.") }
            )
            _uiState.value = _uiState.value.copy(ratePlantState = outcome)
            if (outcome is RatePlantResult.Success) fetchPlantDetail(plantId, token)
        }
    }

    fun refreshPlantDetail(plantId: String, token: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            fetchPlantDetail(plantId, token)
            getPlantComments(plantId)
        }
    }
}
