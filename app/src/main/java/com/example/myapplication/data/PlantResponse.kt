package com.example.myapplication.data

import com.google.gson.annotations.SerializedName

data class PlantListResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("plants")
    val plants: List<PlantResponse>
)

data class PlantResponse(
    @SerializedName("plantId")
    val id: String = "",  // Pastikan ID di sini adalah string, sesuai dengan pengembalian data

    @SerializedName("name")
    val name: String,  // Nama tanaman

    @SerializedName("namaLatin")
    val namaLatin: String,  // Nama latin tanaman

    @SerializedName("komposisi")
    val komposisi: String,  // Komposisi tanaman

    @SerializedName("kegunaan")
    val kegunaan: String,  // Kegunaan tanaman

    @SerializedName("dosis")
    val dosis: String,  // Dosis penggunaan tanaman

    @SerializedName("caraPengolahan")
    val caraPengolahan: String,  // Cara pengolahan tanaman

    @SerializedName("efekSamping")
    val efekSamping: String,  // Efek samping dari tanaman

    @SerializedName("ipfsHash")
    val ipfsHash: String,  // Hash dari IPFS yang menyimpan informasi tambahan

    @SerializedName("ratingTotal")
    val ratingTotal: String = "0",  // Total rating tanaman

    @SerializedName("ratingCount")
    val ratingCount: String = "0",  // Jumlah rating yang diterima tanaman

    @SerializedName("likeCount")
    val likeCount: String = "0",  // Jumlah like yang diterima tanaman

    @SerializedName("owner")
    val owner: String = "",  // Pemilik tanaman

    @SerializedName("isLikedByUser")
    val isLikedByUser: Boolean = false,  // Status apakah user telah memberikan like

    @SerializedName("isRatedByUser")
    val isRatedByUser: Boolean  // Status apakah user telah memberikan rating
)
