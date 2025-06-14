package com.example.myapplication.data

import com.example.myapplication.data.DataClassResponses.Comment
import com.example.myapplication.data.DataClassResponses.RatedPlant

data class PlantUiState(
    val isLoading: Boolean = false,
    val selectedPlant: PlantResponse? = null,
    val plantList: List<RatedPlant> = emptyList(),
    val totalPlants: Int = 0,
    val currentPage: Int = 1,
    val selectedRating: Double = 0.0,
    val ownerFullName: String = "",

    val addPlantState: AddPlantResult = AddPlantResult.Idle,
    val editPlantState: EditPlantResult = EditPlantResult.Idle,
    val likePlantState: LikePlantResult = LikePlantResult.Idle,
    val ratePlantState: RatePlantResult = RatePlantResult.Idle,
    val commentPlantState: CommentPlantResult = CommentPlantResult.Idle,

    val ipfsUploadState: IPFSUploadResult = IPFSUploadResult.Idle,

    val plantComments: List<Comment> = emptyList(),
    val commentCurrentPage: Int = 1,
    val canLoadMoreComments: Boolean = true,
    val isLoadingMoreComments: Boolean = false,

    val searchResults: List<PlantResponse> = emptyList(),
    val isSearching: Boolean = false
)

sealed interface PlantDataResult {
    object Idle : PlantDataResult
    object Loading : PlantDataResult
    data class Success(val plant: PlantResponse) : PlantDataResult
    data class Error(val errorMessage: String) : PlantDataResult
}

sealed interface AddPlantResult {
    object Idle : AddPlantResult
    object Loading : AddPlantResult
    data class Success(val txHash: String, val plantId: String, val message: String = "Tanaman berhasil ditambahkan!") : AddPlantResult
    data class Error(val errorMessage: String) : AddPlantResult
}

sealed interface EditPlantResult {
    object Idle : EditPlantResult
    object Loading : EditPlantResult
    data class Success(val txHash: String, val plantId: String, val message: String = "Perubahan berhasil disimpan!") : EditPlantResult
    data class Error(val errorMessage: String) : EditPlantResult
}

sealed interface LikePlantResult {
    object Idle : LikePlantResult
    object Loading : LikePlantResult
    data class Success(val txHash: String, val plantId: String, val message: String = "Berhasil menyukai tanaman!") : LikePlantResult
    data class Error(val errorMessage: String) : LikePlantResult
}

sealed interface RatePlantResult {
    object Idle : RatePlantResult
    object Loading : RatePlantResult
    data class Success(val txHash: String, val plantId: String, val message: String = "Rating berhasil dikirim!") : RatePlantResult
    data class Error(val errorMessage: String) : RatePlantResult
}

sealed interface IPFSUploadResult {
    object Idle : IPFSUploadResult
    object Loading : IPFSUploadResult
    data class Success(val cid: String, val message: String = "File berhasil diupload!") : IPFSUploadResult
    data class Error(val errorMessage: String) : IPFSUploadResult
}


sealed interface CommentPlantResult {
    object Idle : CommentPlantResult
    object Loading : CommentPlantResult
    data class Success(val txHash: String, val plantId: String, val message: String = "Komentar berhasil ditambahkan!") : CommentPlantResult
    data class Error(val errorMessage: String) : CommentPlantResult
}

