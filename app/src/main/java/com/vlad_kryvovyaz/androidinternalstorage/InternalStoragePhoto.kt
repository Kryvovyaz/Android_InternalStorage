package com.vlad_kryvovyaz.androidinternalstorage
import android.graphics.Bitmap
import android.net.Uri

data class InternalStoragePhoto(
    val name: String,
    val bmp: Bitmap
)
