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
import com.example.myapplication.services.TokenRefreshInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
internal object AppModule {

    /**
     * Menyediakan instance DappMetadata untuk aplikasi.
     */
    @Provides
    fun provideDappMetadata(@ApplicationContext context: Context): DappMetadata {
        // Anda bisa menyesuaikan nama dan URL ini agar lebih deskriptif untuk DApp Anda
        val appName = context.applicationInfo.loadLabel(context.packageManager).toString().replace(" ", "").toLowerCase()
        return DappMetadata(
            name = context.applicationInfo.loadLabel(context.packageManager).toString(), // Nama aplikasi Anda
            url = "https://${appName}.tea.xyz", // Ganti dengan URL DApp Anda yang sebenarnya
            iconUrl = "https://cdn.sstatic.net/Sites/stackoverflow/Img/apple-touch-icon.png" // Ganti dengan URL ikon DApp Anda
        )
    }

    @Provides
    fun provideEthereumFlow(@ApplicationContext context: Context, dappMetadata: DappMetadata): EthereumFlow {
        val infuraApiKey: String? = null

        val readonlyRPCMap = mapOf(
            "Public" to RetrofitClient.PUBLIC_RPC_URL
        )

        // Membuat SDKOptions
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
    fun provideTokenRefreshInterceptor(
        @ApplicationContext context: Context
    ): TokenRefreshInterceptor {
        return TokenRefreshInterceptor(context)
    }

    @Provides
    @Singleton
    fun provideRetrofitClient(
        @ApplicationContext context: Context,
        tokenRefreshInterceptor: TokenRefreshInterceptor
    ): RetrofitClient {
        return RetrofitClient(context, tokenRefreshInterceptor)
    }

    @Provides
    @Singleton
    fun provideApiService(retrofitClient: RetrofitClient): ApiService {
        return retrofitClient.getRetrofit().create(ApiService::class.java)
    }
}