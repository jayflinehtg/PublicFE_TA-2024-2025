package com.example.myapplication.data

import com.google.gson.annotations.SerializedName

data class IPFSResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("cid") val cid: String? = null,
    @SerializedName("message") val message: String? = null
)
