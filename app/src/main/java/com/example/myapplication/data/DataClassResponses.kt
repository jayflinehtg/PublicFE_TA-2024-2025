package com.example.myapplication.data

import com.google.gson.annotations.SerializedName

class DataClassResponses {

    data class TransactionData(
        @SerializedName("transactionData") val transactionData: String
    )

    data class PrepareTransactionApiResponse(
        @SerializedName("success") val success: Boolean,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: TransactionData?
    )

    data class CheckWalletRequest(
        val walletAddress: String
    )

    data class CheckWalletResponse(
        val success: Boolean,
        val message: String? = null,
        val data: CheckWalletData? = null
    )

    data class CheckWalletData(
        val isRegistered: Boolean,
        val walletAddress: String
    )

    data class PrepareRegistrationRequest(
        @SerializedName("fullName") val fullName: String,
        @SerializedName("password") val password: String,
        @SerializedName("walletAddress") val walletAddress: String

    )

    // Data class untuk respons login dari middleware (setelah verifikasi password)
    data class LoginApiResponse( // Ganti nama dari LoginResponse lama agar lebih jelas
        @SerializedName("success") val success: Boolean,
        @SerializedName("token") val token: String?,
        @SerializedName("userData") val userData: UserData?,
        @SerializedName("loginTransactionData") val loginTransactionData: String?, // encodedABI untuk login on-chain
        @SerializedName("message") val message: String?
    )

    data class ServerLogoutResponse(
        @SerializedName("success") val success: Boolean,
        @SerializedName("message") val message: String?,
        @SerializedName("logoutTransactionData") val logoutTransactionData: String?,
        @SerializedName("publicKey") val publicKey: String?
    )

    data class UserData(
        @SerializedName("fullName") val fullName: String?,
        @SerializedName("isRegistered") val isRegistered: Boolean,
        @SerializedName("isLoggedIn") val isLoggedIn: Boolean
    )

    data class UserInfoResponse(
        @SerializedName("success") val success: Boolean,
        @SerializedName("userData") val userData: UserData
    )

    // Komentar dari pengguna
    data class Comment(
        @SerializedName("publicKey") val publicKey: String,
        @SerializedName("fullName") val fullName: String,
        @SerializedName("comment") val comment: String,
        @SerializedName("timestamp") val timestamp: String
    )

    // Respons berpaginasi untuk daftar komentar
    data class PaginatedCommentResponse(
        @SerializedName("success") val success: Boolean,
        @SerializedName("total") val total: Int,
        @SerializedName("currentPage") val currentPage: Int,
        @SerializedName("pageSize") val pageSize: Int,
        @SerializedName("totalPages") val totalPages: Int,
        @SerializedName("comments") val comments: List<Comment>
    )

    data class GetPlantByIdResponse(
        val success: Boolean,
        val plant: PlantResponse
    )

    // Data Class untuk AddPlant Request
    data class AddPlantRequest(
        @SerializedName("name") val name: String,
        @SerializedName("namaLatin") val namaLatin: String,
        @SerializedName("komposisi") val komposisi: String,
        @SerializedName("manfaat") val manfaat: String,
        @SerializedName("dosis") val dosis: String,
        @SerializedName("caraPengolahan") val caraPengolahan: String,
        @SerializedName("efekSamping") val efekSamping: String,
        @SerializedName("ipfsHash") val ipfsHash: String
    )

    // Data Class untuk EditPlant Request
    data class EditPlantRequest(
        @SerializedName("plantId") val plantId: String,
        @SerializedName("name") val name: String,
        @SerializedName("namaLatin") val namaLatin: String,
        @SerializedName("komposisi") val komposisi: String,
        @SerializedName("manfaat") val manfaat: String,
        @SerializedName("dosis") val dosis: String,
        @SerializedName("caraPengolahan") val caraPengolahan: String,
        @SerializedName("efekSamping") val efekSamping: String,
        @SerializedName("ipfsHash") val ipfsHash: String
    )

    data class AverageRatingResponse(
        @SerializedName("success") val success: Boolean,
        @SerializedName("averageRating") val averageRating: String
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

    data class RatedPlant(
        val plant: PlantResponse,
        val averageRating: Double
    )

    // Request untuk memberi like
    data class LikeRequest(
        @SerializedName("plantId") val plantId: String
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

}