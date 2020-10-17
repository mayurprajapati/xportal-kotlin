package com.example.mayur.byteshare.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import android.webkit.MimeTypeMap
import android.widget.Toast

import java.io.File
import java.text.DecimalFormat
import java.util.ArrayList


object FileUtils {

  fun deleteFolderOrFile(file: File) {
    if (!file.exists())
      return

    if (file.isDirectory) {
      for (f in file.listFiles()) {
        deleteFolderOrFile(f)
      }
    }
    file.delete()
  }

  fun getFiles(files: List<File>): List<File> {
    val fileList = ArrayList<File>()
    for (f in files) {
      if (f.isDirectory) {
        fileList.addAll(getFiles(f))
      } else {
        fileList.add(f)
      }
    }
    return fileList
  }

  fun getFiles(folder: File): List<File> {
    val files = ArrayList<File>()
    if (folder.exists() && folder.isDirectory) {
      val f = folder.listFiles()
      for (i in f.indices) {
        if (f[i].isDirectory)
          files.addAll(getFiles(f[i]))
        else {
          files.add(f[i])
          println(f[i].absolutePath)
        }
      }

      println("\n\n\n\n\n\n")
    }

    return files
  }

  fun getExt(f: File): String {

    if (f.isDirectory) {
      return "Folder"
    }

    return if (f.name.contains(".")) {
      try {
        f.name.substring(f.name.lastIndexOf(".") + 1, f.name.length)
      } catch (e: ArrayIndexOutOfBoundsException) {
        ""
      }

    } else
      ""
  }

  fun getExt(f: String): String {


    return if (f.contains(".")) {
      try {
        f.substring(f.lastIndexOf(".") + 1, f.length)
      } catch (e: ArrayIndexOutOfBoundsException) {
        "File"
      }

    } else
      "File"
  }

  fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1000.0)).toInt()
    return DecimalFormat("#,##0.##").format(
      size / Math.pow(
        1024.0,
        digitGroups.toDouble()
      )
    ) + " " + units[digitGroups]
  }

  fun fireIntent(f: File, activity: Activity) {

    if (!f.exists()) {
      Toast.makeText(activity, "File isn't exists", Toast.LENGTH_SHORT).show()
      return
    }
    val item_ext: String = try {
      f.name.substring(f.name.lastIndexOf(".") + 1, f.name.length)
    } catch (e: IndexOutOfBoundsException) {
      ""
    }

    var mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(item_ext)
    if (mime == null) {
      mime = ""
    }

    if (mime == "") {
      val intent = Intent(Intent.ACTION_VIEW)
//      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//        val uri = FileProvider.getUriForFile(
//          activity,
//          BuildConfig.APPLICATION_ID + ".provider",
//          f
//        )
//        intent.setDataAndType(uri, "*/*")
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
//      } else {
//        intent.setDataAndType(Uri.fromFile(f), "*/*")
//      }
      activity.startActivity(intent)
    } else {
      val intent = Intent(Intent.ACTION_VIEW)
//      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//        val uri = FileProvider.getUriForFile(
//          activity,
//          BuildConfig.APPLICATION_ID + ".provider",
//          f
//        )
//        intent.setDataAndType(uri, mime)
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
//      } else {
//        intent.setDataAndType(Uri.fromFile(f), mime)
//      }
      try {
        activity.startActivity(intent)
      } catch (e: ActivityNotFoundException) {
        Toast.makeText(activity, "Sorry, No app found to open file", Toast.LENGTH_SHORT)
          .show()
      }

    }
  }

}
