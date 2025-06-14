package com.example.myapplication.data

import com.google.gson.annotations.SerializedName

data class PlantResponse(
    @SerializedName("plantId") val id: String = "",

    @SerializedName("name") val name: String,

    @SerializedName("namaLatin") val namaLatin: String,

    @SerializedName("komposisi") val komposisi: String,

    @SerializedName("manfaat") val manfaat: String,

    @SerializedName("dosis") val dosis: String,

    @SerializedName("caraPengolahan") val caraPengolahan: String,

    @SerializedName("efekSamping") val efekSamping: String,

    @SerializedName("ipfsHash") val ipfsHash: String,

    @SerializedName("ratingTotal") val ratingTotal: String = "0",

    @SerializedName("ratingCount") val ratingCount: String = "0",

    @SerializedName("likeCount") val likeCount: String = "0",

    @SerializedName("owner") val owner: String = "",

    @SerializedName("isLikedByUser") val isLikedByUser: Boolean = false,

    @SerializedName("isRatedByUser") val isRatedByUser: Boolean
)
