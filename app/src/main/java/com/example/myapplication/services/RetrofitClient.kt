package com.example.myapplication.services

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitClient @Inject constructor(
    private val context: Context,
    private val tokenRefreshInterceptor: TokenRefreshInterceptor
) {

    companion object {
        const val BASE_URL = "http://192.168.1.101:5000/api/"
        const val SMART_CONTRACT_ADDRESS = "0xC1e7C226F8B259B21e1462EECA4eE1d264C3dAA4"
        const val PUBLIC_RPC_URL = "https://tea-sepolia.g.alchemy.com/public"
    }

    private val retrofitInstance: Retrofit by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(tokenRefreshInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getRetrofit(): Retrofit = retrofitInstance
}