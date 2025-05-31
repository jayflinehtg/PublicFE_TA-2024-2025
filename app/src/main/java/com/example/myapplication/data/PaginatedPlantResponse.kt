package com.example.myapplication.data

data class PaginatedPlantResponse(
    val success: Boolean,
    val total: String,
    val currentPage: Int,
    val pageSize: Int,
    val plants: List<PlantResponse>
)
