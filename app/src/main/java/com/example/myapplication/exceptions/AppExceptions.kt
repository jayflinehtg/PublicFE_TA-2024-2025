package com.example.myapplication.exceptions

/**
 * Exception kustom untuk menandakan error validasi atau pre-flight check di dalam ViewModel
 * sebelum melakukan panggilan ke SDK atau jaringan.
 *
 * @param message Pesan error.
 * @param internalCode Kode error internal opsional untuk identifikasi lebih lanjut.
 */
class ViewModelValidationException(
    override val message: String,
    val internalCode: Int? = null
) : Exception(message)