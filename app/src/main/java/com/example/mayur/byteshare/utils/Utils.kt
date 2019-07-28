package io.github.mayurprajapati.sharing.utils

import android.graphics.Bitmap

object Utils {
  fun compressBitmap(bitmap:Bitmap, compressionPercentage: Int): Bitmap {
    val newHeight = bitmap.height * compressionPercentage / 100
    val newWidth = bitmap.width * compressionPercentage / 100

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
  }
}