package com.example.myapplication.services

import com.example.myapplication.data.DataClassResponses
import com.example.myapplication.data.DataClassResponses.AddPlantRequest
import com.example.myapplication.data.DataClassResponses.AddPlantResponse
import com.example.myapplication.data.DataClassResponses.AverageRatingResponse
import com.example.myapplication.data.DataClassResponses.CommentListResponse
import com.example.myapplication.data.DataClassResponses.CommentRequest
import com.example.myapplication.data.DataClassResponses.EditPlantRequest
import com.example.myapplication.data.DataClassResponses.EditPlantResponse
import com.example.myapplication.data.DataClassResponses.LikeRequest
import com.example.myapplication.data.DataClassResponses.LoginResponse
import com.example.myapplication.data.DataClassResponses.LogoutResponse
import com.example.myapplication.data.DataClassResponses.RatePlantRequest
import com.example.myapplication.data.DataClassResponses.RatePlantResponse
import com.example.myapplication.data.DataClassResponses.RegisterResponse
import com.example.myapplication.data.DataClassResponses.PublicResponse
import com.example.myapplication.data.DataClassResponses.SimpleResponse
import com.example.myapplication.data.DataClassResponses.UserInfoResponse
import com.example.myapplication.data.IPFSResponse
import com.example.myapplication.data.LoginRequest
import com.example.myapplication.data.PaginatedPlantResponse
import com.example.myapplication.data.PlantListResponse
import com.example.myapplication.data.PlantResponse
import com.example.myapplication.data.User
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    /* ================================ Autentikasi ================================ */
    @POST("auth/register")
    fun registerUser(@Body user: User): Call<RegisterResponse>

    @POST("auth/login")
    fun loginUser(@Body loginRequest: LoginRequest): Call<LoginResponse>

    @GET("auth/user/{walletAddress}")
    suspend fun getUserInfo(@Path("walletAddress") walletAddress: String): UserInfoResponse

    @POST("auth/logout")
    fun logoutUser(@Header("Authorization") authorization: String): Call<LogoutResponse>

    /* ================================ Tanaman ================================ */
    @POST("plants/add")
    suspend fun addPlant(
        @Header("Authorization") token: String,
        @Body request: AddPlantRequest
    ): AddPlantResponse

    @POST("plants/syncPublic")
    suspend fun syncPlantToPublic(
        @Header("Authorization") token: String,
        @Body plantId: String
    ): PublicResponse

    @PUT("plants/edit/{plantId}")
    suspend fun editPlant(
        @Header("Authorization") token: String,
        @Path("plantId") plantId: String,
        @Body request: EditPlantRequest
    ): EditPlantResponse

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
        @Query("kegunaan") kegunaan: String = ""
    ): PlantListResponse

    @GET("plants/plant/averageRating/{plantId}")
    suspend fun getAverageRating(
        @Path("plantId") plantId: String
    ): AverageRatingResponse

    @POST("plants/like")
    suspend fun likePlant(
        @Header("Authorization") token: String,
        @Body request: LikeRequest
    ): SimpleResponse

    @POST("plants/comment")
    suspend fun commentPlant(
        @Header("Authorization") token: String,
        @Body request: CommentRequest
    ): SimpleResponse

    @GET("plants/{plantId}/comments")
    suspend fun getComments(
        @Path("plantId") plantId: String
    ): CommentListResponse

    @POST("plants/rate")
    suspend fun ratePlant(
        @Header("Authorization") token: String,
        @Body request: RatePlantRequest
    ): RatePlantResponse

    /* ================================ IPFS ================================ */
    @Multipart
    @POST("ipfs/upload")
    fun uploadImage(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part
    ): Call<IPFSResponse>

    @GET("ipfs/getFile/{cid}")
    suspend fun getFileFromIPFS(
        @Path("cid") cid: String
    ): ResponseBody
}
