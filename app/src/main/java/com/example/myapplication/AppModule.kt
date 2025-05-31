package com.example.myapplication

import android.content.Context
import com.example.myapplication.services.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.metamask.androidsdk.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import com.example.myapplication.services.RetrofitClient // Mengimpor RetrofitClient

@Module
@InstallIn(SingletonComponent::class)
internal object AppModule {

    /**
     * Menyediakan instance DappMetadata untuk aplikasi.
     */
    @Provides
    fun provideDappMetadata(@ApplicationContext context: Context): DappMetadata {
        return DappMetadata(
            name = context.applicationInfo.name,
            url = "https://${context.applicationInfo.name}.com",
            iconUrl = "https://cdn.sstatic.net/Sites/stackoverflow/Img/apple-touch-icon.png"
        )
    }

    @Provides
    fun provideEthereumFlow(@ApplicationContext context: Context, dappMetadata: DappMetadata): EthereumFlow {
        // Mengambil konfigurasi dari RetrofitClient dan memilih jaringan yang aktif
        val infuraApiKey = RetrofitClient.ETH_INFURA_API_KEY // Mengambil Infura API Key dari RetrofitClient

        // Pilih jaringan yang digunakan di sini, tinggal uncomment yang digunakan
        val readonlyRPCMap = mapOf(
            // Ganache
            "Ganache" to "http://192.168.1.104:7545" // RPC URL untuk Ganache
            // Tea-Sepolia
            // "Tea-Sepolia" to RetrofitClient.ETH_RPC_URL // RPC URL untuk Tea-Sepolia
        )

        // Membuat SDKOptions dengan Infura API Key dan readonlyRPCMap
        val sdkOptions = SDKOptions(
            infuraAPIKey = infuraApiKey,
            readonlyRPCMap = readonlyRPCMap
        )

        // Membuat objek Ethereum
        val ethereum = Ethereum(context, dappMetadata, sdkOptions)

        // Mengembalikan EthereumFlow
        return EthereumFlow(ethereum)
    }

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(RetrofitClient.BASE_URL) // Menggunakan BASE_URL yang ada di RetrofitClient
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}