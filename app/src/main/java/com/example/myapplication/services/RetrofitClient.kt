package com.example.myapplication.services

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // Base URL untuk API
    const val BASE_URL = "http://192.168.1.104:5000/api/"  // Semua route, termasuk IPFS

    // Konfigurasi Ethereum
    const val ETH_INFURA_API_KEY = "98144ec0a3b54b3582ccdd2e99921cf5"

    // Retrofit instance untuk API service
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(OkHttpClient())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
