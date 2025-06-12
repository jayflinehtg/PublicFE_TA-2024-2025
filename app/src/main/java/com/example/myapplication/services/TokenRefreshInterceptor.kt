package com.example.myapplication.services

import android.content.Context
import android.util.Log
import com.example.myapplication.data.PreferencesHelper
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class TokenRefreshInterceptor @Inject constructor(
    private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)

        val newToken = response.header("X-New-Token")
        if (!newToken.isNullOrEmpty()) {
            Log.d("TokenRefresh", "Token refreshed automatically by backend")
            PreferencesHelper.saveJwtToken(context, newToken)
        }

        return response
    }
}