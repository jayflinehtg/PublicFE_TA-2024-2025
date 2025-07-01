package com.example.myapplication.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object PreferencesHelper {

    private const val PREFS_NAME = "AppPreferences"
    private const val KEY_IS_META_MASK_CONNECTED = "isMetaMaskConnected"
    private const val KEY_IS_USER_REGISTERED = "isUserRegistered"
    private const val KEY_WALLET_ADDRESS = "walletAddress"
    private const val KEY_JWT_TOKEN = "jwtToken"
    private const val KEY_USER_FULL_NAME = "userFullName" // Tambahkan key untuk fullName

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveMetaMaskConnectionStatus(context: Context, isConnected: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_IS_META_MASK_CONNECTED, isConnected) }
    }

    fun isMetaMaskConnected(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_META_MASK_CONNECTED, false)
    }

    fun saveUserRegistrationStatus(context: Context, isRegistered: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_IS_USER_REGISTERED, isRegistered) }
    }

    fun isUserRegistered(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_USER_REGISTERED, false)
    }

    fun saveWalletAddress(context: Context, walletAddress: String) {
        getPrefs(context).edit { putString(KEY_WALLET_ADDRESS, walletAddress) }
    }

    fun getWalletAddress(context: Context): String? {
        return getPrefs(context).getString(KEY_WALLET_ADDRESS, null)
    }

    fun saveJwtToken(context: Context, jwtToken: String) {
        getPrefs(context).edit { putString(KEY_JWT_TOKEN, jwtToken) }
    }

    fun getJwtToken(context: Context): String? {
        return getPrefs(context).getString(KEY_JWT_TOKEN, null)
    }

    fun clearJwtToken(context: Context) {
        getPrefs(context).edit { remove(KEY_JWT_TOKEN) }
    }

    // Fungsi untuk menyimpan fullName
    fun saveUserFullName(context: Context, fullName: String) {
        getPrefs(context).edit { putString(KEY_USER_FULL_NAME, fullName) }
    }

    // Fungsi untuk mengambil fullName
    fun getUserFullName(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_FULL_NAME, null)
    }

    // Fungsi untuk menghapus fullName (opsional)
    fun clearUserFullName(context: Context) {
        getPrefs(context).edit { remove(KEY_USER_FULL_NAME) }
    }

    // Fungsi untuk menghapus wallet address
    fun clearWalletAddress(context: Context) {
        getPrefs(context).edit { remove(KEY_WALLET_ADDRESS) }
    }
}