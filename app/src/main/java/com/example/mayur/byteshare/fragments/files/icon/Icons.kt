package com.example.mayur.xportal.fragments.files.icon

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.widget.ImageView

import com.bumptech.glide.Glide
import com.example.mayur.xportal.R

import java.io.File


object Icons {
    @SuppressLint("ResourceType")
    fun setIcon(f: File, packageManager: PackageManager, imageView: ImageView) {
        val icon: Int
        if (f.isDirectory) {
            Glide.with(imageView.context).load(R.raw.icon_folder).into(imageView)
            return
        }
        val i = f.name.lastIndexOf(".")
        var extension = ""
        if (i != -1)
            extension = f.name.substring(i + 1)
        when (extension) {
            "mp4" -> icon = R.raw.icon_mp4
            "mp3" -> icon = R.raw.icon_mp3
            "avi" -> icon = R.raw.icon_avi
            "zip", "gzip", "tar", "gz" -> icon = R.raw.icon_zip
            "txt" -> icon = R.raw.icon_txt
            "html" -> icon = R.raw.icon_html
            "htm" -> icon = R.raw.icon_html
            "jpg" -> icon = R.raw.icon_jpg
            "jpeg" -> icon = R.raw.icon_jpg
            "pdf" -> icon = R.raw.icon_pdf
            "png" -> icon = R.raw.icon_png
            "ppt" -> icon = R.raw.icon_ppt
            "apk" -> {
                MyTask(f, imageView, packageManager).execute()
                return
            }
            else -> icon = R.raw.icon_file
        }

        Glide.with(imageView.context).load(icon).into(imageView)
    }

    fun setIcon(
        f: String,
        packageManager: PackageManager? = null,
        imageView: ImageView,
        isDirectory: Boolean
    ) {
        val icon: Int
        if (isDirectory) {
            Glide.with(imageView.context).load(R.raw.icon_folder).into(imageView)
            return
        }
        val i = f.lastIndexOf(".")
        var extension = ""
        if (i != -1)
            extension = f.substring(i + 1)
        when (extension) {
            "mp4" -> icon = R.raw.icon_mp4
            "mp3" -> icon = R.raw.icon_mp3
            "avi" -> icon = R.raw.icon_avi
            "zip", "gzip", "tar", "gz" -> icon = R.raw.icon_zip
            "txt" -> icon = R.raw.icon_txt
            "html" -> icon = R.raw.icon_html
            "htm" -> icon = R.raw.icon_html
            "jpg" -> icon = R.raw.icon_jpg
            "jpeg" -> icon = R.raw.icon_jpg
            "pdf" -> icon = R.raw.icon_pdf
            "png" -> icon = R.raw.icon_png
            "ppt" -> icon = R.raw.icon_ppt
            //            case "apk":
            //                new MyTask(f, imageView, packageManager).execute();
            //                return;
            else -> icon = R.raw.icon_file
        }

        Glide.with(imageView.context).load(icon).into(imageView)
    }
}


internal class MyTask(
    private val f: File, @field:SuppressLint("StaticFieldLeak")
    private val imageView: ImageView, private val packageManager: PackageManager
) : AsyncTask<Void, Void, Void>() {
    private var packageInfo: PackageInfo? = null

    @SuppressLint("ResourceType")
    override fun onPostExecute(aVoid: Void) {
        super.onPostExecute(aVoid)
        if (packageInfo == null) {
            imageView.setImageResource(R.raw.icon_file)
            return
        }
        imageView.setImageDrawable(packageInfo!!.applicationInfo.loadIcon(packageManager))
    }

    override fun doInBackground(vararg voids: Void): Void? {
        val path = f.absolutePath
        packageInfo = packageManager.getPackageArchiveInfo(path, 0)
        if (packageInfo != null) {
            packageInfo!!.applicationInfo.sourceDir = path
            packageInfo!!.applicationInfo.publicSourceDir = path
        }
        return null
    }
}