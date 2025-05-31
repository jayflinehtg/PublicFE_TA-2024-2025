package com.example.myapplication.data

import com.google.gson.annotations.SerializedName

class DataClassResponses {

    // Data class untuk respons registrasi
    data class RegisterResponse(
        val success: Boolean,
        val txHash: String,
        val message: String
    )

    // Data class untuk respons login
    data class LoginResponse(
        val success: Boolean,
        val token: String,
        val message: String
    )

    data class LogoutResponse(
        val success: Boolean,
        val message: String,
        val txHash: String? = null
    )

    data class UserData(
        @SerializedName("fullName") val fullName: String,
        @SerializedName("isRegistered") val isRegistered: Boolean,
        @SerializedName("isLoggedIn") val isLoggedIn: Boolean
    )

    data class GetPlantByIdResponse(
        val success: Boolean,
        val plant: PlantResponse
    )

    data class UserInfoResponse(
        @SerializedName("success") val success: Boolean,
        @SerializedName("userData") val userData: UserData
    )

    // Data Class untuk AddPlant Request
    data class AddPlantRequest(
        @SerializedName("name") val name: String,
        @SerializedName("namaLatin") val namaLatin: String,
        @SerializedName("komposisi") val komposisi: String,
        @SerializedName("kegunaan") val kegunaan: String,
        @SerializedName("dosis") val dosis: String,
        @SerializedName("caraPengolahan") val caraPengolahan: String,
        @SerializedName("efekSamping") val efekSamping: String,
        @SerializedName("ipfsHash") val ipfsHash: String
    )

    // Data Class untuk AddPlant Response
    data class AddPlantResponse(
        @SerializedName("success") val success: Boolean,
        @SerializedName("message") val message: String,
        @SerializedName("txHash") val txHash: String,
        @SerializedName("plantId") val plantId: String
    )

    // Data Class untuk EditPlant Request
    data class EditPlantRequest(
        @SerializedName("plantId") val plantId: String,
        @SerializedName("name") val name: String,
        @SerializedName("namaLatin") val namaLatin: String,
        @SerializedName("komposisi") val komposisi: String,
        @SerializedName("kegunaan") val kegunaan: String,
        @SerializedName("dosis") val dosis: String,
        @SerializedName("caraPengolahan") val caraPengolahan: String,
        @SerializedName("efekSamping") val efekSamping: String,
        @SerializedName("ipfsHash") val ipfsHash: String
    )

    data class PublicResponse(
        @SerializedName("success") val success: Boolean,
        @SerializedName("message") val message: String,
        @SerializedName("publicTx") val publicTx: String? // Add publicTx to capture the transaction hash
    )

    // Data Class untuk EditPlant Response
    data class EditPlantResponse(
        @SerializedName("success") val success: Boolean,
        @SerializedName("message") val message: String,
        @SerializedName("txHash") val txHash: String,
        @SerializedName("plantId") val plantId: String
    )

    data class AverageRatingResponse(
        @SerializedName("success") val success: Boolean,
        @SerializedName("averageRating") val averageRating: String
    )

    data class RatedPlant(
        val plant: PlantResponse,
        val averageRating: Double
    )

    // Request untuk memberi like
    data class LikeRequest(
        @SerializedName("plantId") val plantId: String
    )

    data class IsLikedResponse(
        val success: Boolean,
        val liked: Boolean
    )

    // Request untuk komentar
    data class CommentRequest(
        @SerializedName("plantId") val plantId: String,
        @SerializedName("comment") val comment: String
    )

    // Response umum untuk like dan komentar
    data class SimpleResponse(
        @SerializedName("success") val success: Boolean,
        @SerializedName("message") val message: String,
        @SerializedName("txHash") val txHash: String,
        @SerializedName("plantId") val plantId: String
    )

    // Komentar dari pengguna
    data class Comment(
        @SerializedName("publicKey") val publicKey: String,
        @SerializedName("fullName") val fullName: String,
        @SerializedName("comment") val comment: String,
        @SerializedName("timestamp") val timestamp: String
    )

    data class CommentListResponse(
        @SerializedName("success") val success: Boolean,
        @SerializedName("comments") val comments: List<Comment>
    )

    data class RatePlantRequest(
        @SerializedName("plantId") val plantId: String,
        @SerializedName("rating") val rating: Int
    )

    data class RatePlantResponse(
        @SerializedName("success") val success: Boolean,
        @SerializedName("message") val message: String,
        @SerializedName("txHash") val txHash: String?
    )
}