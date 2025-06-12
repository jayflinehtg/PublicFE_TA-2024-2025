package com.example.myapplication.services

import com.example.myapplication.data.DataClassResponses
import com.example.myapplication.data.DataClassResponses.AverageRatingResponse
import com.example.myapplication.data.DataClassResponses.CheckWalletRequest
import com.example.myapplication.data.DataClassResponses.CheckWalletResponse
import com.example.myapplication.data.DataClassResponses.LoginApiResponse
import com.example.myapplication.data.DataClassResponses.PrepareRegistrationRequest
import com.example.myapplication.data.DataClassResponses.RatePlantRequest
import com.example.myapplication.data.DataClassResponses.RatePlantResponse
import com.example.myapplication.data.DataClassResponses.ServerLogoutResponse
import com.example.myapplication.data.DataClassResponses.SimpleResponse
import com.example.myapplication.data.DataClassResponses.UserInfoResponse
import com.example.myapplication.data.IPFSResponse
import com.example.myapplication.data.LoginRequest
import com.example.myapplication.data.PaginatedPlantResponse
import com.example.myapplication.data.PlantListResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    /* ================================ Autentikasi ================================ */

    @POST("auth/register")
    suspend fun prepareRegistration(
        @Body request: PrepareRegistrationRequest
    ): DataClassResponses.PrepareTransactionApiResponse

    @POST("auth/login")
    suspend fun loginUser(
        @Body loginRequest: LoginRequest
    ): LoginApiResponse

    @GET("auth/user/{walletAddress}")
    suspend fun getUserInfo(@Path("walletAddress") walletAddress: String): UserInfoResponse

    @POST("auth/logout")
    suspend fun logoutUserFromServer(
        @Header("Authorization") authorization: String
    ): ServerLogoutResponse

    /* ================================ Tanaman ================================ */
    @POST("plants/add")
    suspend fun prepareAddPlant( // Nama diubah untuk kejelasan
        @Header("Authorization") token: String,
        @Body request: DataClassResponses.AddPlantRequest
    ): DataClassResponses.PrepareTransactionApiResponse

    @PUT("plants/edit/{plantId}")
    suspend fun prepareEditPlant( // Nama diubah untuk kejelasan
        @Header("Authorization") token: String,
        @Path("plantId") plantId: String,
        @Body request: DataClassResponses.EditPlantRequest
    ): DataClassResponses.PrepareTransactionApiResponse

    @POST("plants/like")
    suspend fun prepareLikePlant( // Nama diubah untuk kejelasan
        @Header("Authorization") token: String,
        @Body request: DataClassResponses.LikeRequest
    ): DataClassResponses.PrepareTransactionApiResponse

    @POST("plants/rate")
    suspend fun prepareRatePlant( // Nama diubah untuk kejelasan
        @Header("Authorization") token: String,
        @Body request: DataClassResponses.RatePlantRequest
    ): DataClassResponses.PrepareTransactionApiResponse

    @POST("plants/comment")
    suspend fun prepareCommentPlant( // Nama diubah untuk kejelasan
        @Header("Authorization") token: String,
        @Body request: DataClassResponses.CommentRequest
    ): DataClassResponses.PrepareTransactionApiResponse

    @GET("plants/all")
    suspend fun getPaginatedPlants(
        @Query("page") page: Int,
        @Query("limit") limit: Int = 10
    ): PaginatedPlantResponse

    @GET("plants/{plantId}")
    suspend fun getPlantById(
        @Path("plantId") plantId: String,
        @Header("Authorization") token: String? = null
    ): DataClassResponses.GetPlantByIdResponse

    @GET("plants/search")
    suspend fun searchPlants(
        @Query("name") name: String = "",
        @Query("namaLatin") namaLatin: String = "",
        @Query("komposisi") komposisi: String = "",
        @Query("manfaat") manfaat: String = ""
    ): PlantListResponse

    @GET("plants/plant/averageRating/{plantId}")
    suspend fun getAverageRating(
        @Path("plantId") plantId: String
    ): AverageRatingResponse

    @GET("plants/{plantId}/comments")
    suspend fun getPaginatedComments( // Nama diubah untuk kejelasan
        @Path("plantId") plantId: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int = 10
    ): DataClassResponses.PaginatedCommentResponse

    /* ================================ IPFS ================================ */
    @Multipart
    @POST("ipfs/upload")
    suspend fun uploadImage(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part
    ): IPFSResponse

    @GET("ipfs/getFile/{cid}")
    suspend fun getFileFromIPFS(
        @Path("cid") cid: String
    ): ResponseBody
}
