package com.example.myapplication

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.DataClassResponses
import com.example.myapplication.data.DataClassResponses.AddPlantRequest
import com.example.myapplication.data.DataClassResponses.AddPlantResponse
import com.example.myapplication.data.DataClassResponses.RatePlantRequest
import com.example.myapplication.data.DataClassResponses.RatedPlant
import com.example.myapplication.data.PlantResponse
import com.example.myapplication.data.PaginatedPlantResponse
import com.example.myapplication.data.PreferencesHelper
import com.example.myapplication.services.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class PlantViewModel @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Menyimpan alamat pengguna yang sedang login
    private val _userAddress = mutableStateOf<String?>(null)
    val userAddress: State<String?> = _userAddress

    init {
        // Mengambil alamat pengguna dari SharedPreferences (PreferencesHelper)
        _userAddress.value = PreferencesHelper.getWalletAddress(context)
    }

    // Menyimpan full name pemilik tanaman
    private val _ownerFullName = mutableStateOf("")
    val ownerFullName: State<String> = _ownerFullName

    val apiServiceInstance get() = apiService

    // Fungsi untuk mengambil fullName berdasarkan wallet address (owner)
    fun fetchOwnerFullName(ownerAddress: String) {
        viewModelScope.launch {
            try {
                // Panggil API secara langsung tanpa menggunakan execute()
                val response = apiService.getUserInfo(ownerAddress)

                // Periksa hasil response
                if (response.success) {
                    val fullName = response.userData.fullName ?: "Unknown"
                    _ownerFullName.value = fullName
                } else {
                    _ownerFullName.value = "Tidak dapat mengambil nama"
                }
            } catch (e: Exception) {
                _ownerFullName.value = "Error mengambil data"
            }
        }
    }

    // ==================== CID ====================
    private val _cid = mutableStateOf("")
    val cid: State<String> = _cid

    fun setCid(newCid: String) {
        _cid.value = newCid
    }

    // ==================== Tambah Tanaman ====================
    fun addPlant(
        token: String,
        request: AddPlantRequest,
        onSuccess: (AddPlantResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("AddPlant", "Sending request to backend with: $request")
                Log.d("AddPlant", "Authorization Token: $token")

                // Mengirim request ke API untuk menambahkan tanaman
                val response = apiService.addPlant(token, request)
                Log.d("AddPlant", "Success! Response: ${response.message}")
                onSuccess(response)

            } catch (e: IOException) {
                Log.e("AddPlant", "IOException: ${e.message}", e)
                onError("Terjadi kesalahan jaringan: ${e.message}")
            } catch (e: Exception) {
                Log.e("AddPlant", "Exception: ${e.message}", e)
                onError("Terjadi kesalahan: ${e.message}")
            }
        }
    }

    fun syncPlantToPublic(plantId: String) {
        viewModelScope.launch {
            try {
                // Ambil token JWT dari preferences
                val jwtTokenRaw = PreferencesHelper.getJwtToken(context)
                val jwtToken = "Bearer ${jwtTokenRaw ?: ""}"

                // Memanggil API backend untuk sinkronisasi
                val response = apiService.syncPlantToPublic(
                    token = jwtToken,
                    plantId = plantId
                )

                if (response.success) {
                    // Menampilkan pesan sukses
                    Log.d("syncPlant", "Sinkronisasi berhasil dengan txHash: ${response.publicTx}")
                    Toast.makeText(context, "Tanaman berhasil disinkronkan ke jaringan publik!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("syncPlant", "Gagal sinkronkan tanaman")
                    Toast.makeText(context, "Gagal sinkronkan tanaman ke jaringan publik", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("syncPlant", "Error: ${e.message}")
                Toast.makeText(context, "Terjadi kesalahan saat sinkronisasi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ==================== Edit Tanaman =======================
    fun editPlant(
        token: String,
        request: DataClassResponses.EditPlantRequest,
        onSuccess: (DataClassResponses.EditPlantResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("EditPlant", "Mengedit tanaman ID: ${request.plantId}")
                Log.d("EditPlant", "Request data: $request")

                val response = apiService.editPlant(
                    token = token,
                    plantId = request.plantId,
                    request = request
                )

                if (response.success) {
                    Log.d("EditPlant", "Edit berhasil! TX Hash: ${response.txHash}")
                    // Refresh data tanaman yang diedit
                    fetchPlantDetail(request.plantId, token)
                    onSuccess(response)
                } else {
                    Log.e("EditPlant", "Gagal edit: ${response.message}")
                    onError(response.message)
                }
            } catch (e: IOException) {
                val errorMsg = "Error jaringan: ${e.message}"
                Log.e("EditPlant", errorMsg, e)
                onError(errorMsg)
            } catch (e: Exception) {
                val errorMsg = "Error: ${e.message}"
                Log.e("EditPlant", errorMsg, e)
                onError(errorMsg)
            }
        }
    }

    // ==================== Search Tanaman ========================
    fun searchPlants(
        name: String = "",
        namaLatin: String = "",
        komposisi: String = "",
        kegunaan: String = ""
    ) {
        viewModelScope.launch {
            try {
                val response = apiService.searchPlants(name, namaLatin, komposisi, kegunaan)
                val ratedResults = response.plants.map { plant ->
                    val ratingResponse = try {
                        apiService.getAverageRating(plant.id)
                    } catch (e: Exception) {
                        null
                    }
                    val avgRating = ratingResponse?.averageRating?.toDoubleOrNull() ?: 0.0
                    RatedPlant(plant, avgRating)
                }
                _ratedPlantList.value = ratedResults
                _totalPlants.value = ratedResults.size
                _currentPage.value = 1
                Log.d("SearchPlant", "Berhasil mencari ${ratedResults.size} tanaman.")
            } catch (e: Exception) {
                Log.e("SearchPlant", "Gagal mencari tanaman: ${e.message}")
            }
        }
    }

    // ==================== Pagination Tanaman ====================
    private val _ratedPlantList = mutableStateOf<List<RatedPlant>>(emptyList())
    val ratedPlantList: State<List<RatedPlant>> = _ratedPlantList

    private val _totalPlants = mutableStateOf(0)
    val totalPlants: State<Int> = _totalPlants

    private val _currentPage = mutableStateOf(1)
    val currentPage: State<Int> = _currentPage

    private val pageSize = 10

    fun fetchPlantsByPage(page: Int = 1) {
        viewModelScope.launch {
            try {
                Log.d("Pagination", "Fetching page $page")
                val response: PaginatedPlantResponse = apiService.getPaginatedPlants(page, pageSize)
                if (response.success) {
                    val ratedPlants = response.plants.map { plant ->
                        val ratingResponse = try {
                            apiService.getAverageRating(plant.id)
                        } catch (e: Exception) {
                            null
                        }
                        val avgRating = ratingResponse?.averageRating?.toDoubleOrNull() ?: 0.0
                        RatedPlant(plant, avgRating)
                    }

                    _ratedPlantList.value = ratedPlants
                    _totalPlants.value = response.total.toInt()
                    _currentPage.value = response.currentPage
                    Log.d("Pagination", "Fetched ${ratedPlants.size} rated plants on page $page")
                } else {
                    Log.e("Pagination", "Failed to fetch plants: success=false")
                }
            } catch (e: Exception) {
                Log.e("Pagination", "Error fetching plants: ${e.message}")
            }
        }
    }

    // Untuk menampilkan detail tanaman
    private val _selectedPlant = mutableStateOf<PlantResponse?>(null)
    val selectedPlant: State<PlantResponse?> = _selectedPlant

    private val _selectedRating = mutableStateOf(0.0)
    val selectedRating: State<Double> = _selectedRating

    fun fetchPlantDetail(plantId: String, token: String? = null) {
        viewModelScope.launch {
            try {
                val local = ratedPlantList.value.find { it.plant.id == plantId }
                if (local != null) {
                    _selectedPlant.value = local.plant
                    _selectedRating.value = local.averageRating
                } else {
                    val response = apiService.getPlantById(plantId, token?.let { "Bearer $it" })
                    if (response.success) {
                        _selectedPlant.value = response.plant

                        val ratingTotal = response.plant.ratingTotal.toDoubleOrNull() ?: 0.0
                        val ratingCount = response.plant.ratingCount.toDoubleOrNull() ?: 0.0
                        val average = if (ratingCount > 0) ratingTotal / ratingCount else 0.0

                        _selectedRating.value = average
                    } else {
                        _selectedPlant.value = null
                    }
                }
                Log.d("DetailViewModel", "Berhasil load detail tanaman")
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Gagal load detail tanaman: ${e.message}")
                _selectedPlant.value = null
            }
        }
    }

    // Untuk Like tanaman
    fun likePlant(token: String, plantId: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = apiService.likePlant(token, DataClassResponses.LikeRequest(plantId))
                if (response.success) {
                    // Setelah sukses, ambil data terbaru
                    try {
                        val detailResponse = apiService.getPlantById(plantId, token)
                        _selectedPlant.value = detailResponse.plant

                        val ratingResponse = apiService.getAverageRating(plantId)
                        _selectedRating.value = ratingResponse.averageRating.toDoubleOrNull() ?: 0.0

                        onSuccess(response.txHash ?: "Like berhasil")
                    } catch (e: Exception) {
                        Log.e("LikePlant", "Error refreshing data: ${e.message}")
                        onSuccess(response.txHash ?: "Like berhasil")
                    }
                } else {
                    onError("Gagal menyukai tanaman")
                }
            } catch (e: Exception) {
                Log.e("LikePlant", "Error: ${e.message}")
                onError("Error: ${e.message}")
            }
        }
    }

    fun toggleLikeLocally() {
        _selectedPlant.value = _selectedPlant.value?.copy(
            isLikedByUser = !(_selectedPlant.value?.isLikedByUser ?: false),
            likeCount = (_selectedPlant.value?.likeCount?.toIntOrNull() ?: 0)
                .let { if (_selectedPlant.value?.isLikedByUser == true) it - 1 else it + 1 }
                .toString()
        )
    }

    // Untuk Memberi Komentar Pada Tanaman
    fun commentPlant(token: String, plantId: String, comment: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = apiService.commentPlant(token, DataClassResponses.CommentRequest(plantId, comment))
                if (response.success) {
                    onSuccess(response.txHash ?: "Komentar berhasil")
                } else {
                    onError("Gagal menambahkan komentar")
                }
            } catch (e: Exception) {
                Log.e("CommentPlant", "Error: ${e.message}")
                onError("Error: ${e.message}")
            }
        }
    }

    // Untuk mendapatkan komentar pada tanaman
    private val _plantComments = mutableStateOf<List<DataClassResponses.Comment>>(emptyList())
    val plantComments: State<List<DataClassResponses.Comment>> = _plantComments

    fun getPlantComments(plantId: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getComments(plantId)
                if (response.success) {
                    _plantComments.value = response.comments
                }
            } catch (e: Exception) {
                Log.e("GetComments", "Error: ${e.message}")
            }
        }
    }

    // Untuk Menambahkan Rating pada tanaman
    fun ratePlant(
        token: String,
        plantId: String,
        rating: Int,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = apiService.ratePlant(token, RatePlantRequest(plantId, rating))
                if (response.success) {
                    // Setelah sukses, ambil data terbaru langsung
                    try {
                        val detailResponse = apiService.getPlantById(plantId, token)
                        _selectedPlant.value = detailResponse.plant

                        val ratingResponse = apiService.getAverageRating(plantId)
                        _selectedRating.value = ratingResponse.averageRating.toDoubleOrNull() ?: 0.0

                        onSuccess(response.txHash ?: "Berhasil memberi rating.")
                    } catch (e: Exception) {
                        Log.e("RatePlant", "Error refreshing data: ${e.message}")
                        onSuccess(response.txHash ?: "Berhasil memberi rating.")
                    }
                } else {
                    onError(response.message)
                }
            } catch (e: Exception) {
                onError("Gagal memberi rating: ${e.message}")
            }
        }
    }

    fun refreshPlantDetail(plantId: String, token: String) {
        viewModelScope.launch {
            try {
                fetchPlantDetail(plantId, token)
                getPlantComments(plantId)
            } catch (e: Exception) {
                Log.e("RefreshDetail", "Error: ${e.message}")
            }
        }
    }
}
