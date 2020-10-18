package com.example.mayur.byteshare.utils

import android.graphics.Bitmap
import androidx.core.app.ActivityCompat
import com.example.mayur.byteshare.MainActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.random.Random

object Utils {
    fun compressBitmap(bitmap: Bitmap, compressionPercentage: Int): Bitmap {
        val newHeight = bitmap.height * compressionPercentage / 100
        val newWidth = bitmap.width * compressionPercentage / 100

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    suspend fun MainActivity.hasPermissions(vararg permissions: String): Boolean = suspendCancellableCoroutine { continuation ->
        val requestCode = Random.nextInt(0, 65000)
        permissionResultListeners[requestCode] = {
            permissionResultListeners.remove(requestCode)
            continuation.resumeWith(Result.success(it))
        }
        ActivityCompat.requestPermissions(this, permissions, requestCode)
    }
}

/**
 * Creates folder if doesn't exists
 */
fun ensureFolderExists(folder: File): File {
    if (!folder.exists()) folder.mkdirs()
  return folder
}