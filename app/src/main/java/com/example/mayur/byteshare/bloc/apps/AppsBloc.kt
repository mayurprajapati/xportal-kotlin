package com.example.mayur.byteshare.bloc.apps

import android.app.Activity
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import com.example.mayur.byteshare.bloc.interfaces.Selectable
import com.example.mayur.byteshare.ioCoroutine
import com.example.mayur.byteshare.utils.ensureFolderExists
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.util.*

data class AppInfo(val name: String, val size: Long, val icon: String, val packageName: String, val isSystemApp: Boolean, val path: String) : Selectable()

class AppsBloc private constructor(private val activity: Activity) {
    private val iconsFolder = File(activity.applicationContext.filesDir, "icons")
    val apps = mutableListOf<AppInfo>()

    companion object {
        var instance: AppsBloc? = null

        @Synchronized
        fun init(mainActivity: Activity): AppsBloc {
            if (instance == null) instance = AppsBloc(mainActivity)
            return instance!!
        }
    }

    suspend fun getInstalledApps(refresh: Boolean = false, includeSystemApps: Boolean = false) = withContext<List<AppInfo>>(Dispatchers.IO) {
        if (apps.isNotEmpty() && !refresh && !includeSystemApps) return@withContext apps

        val applicationInfoList = mutableListOf<AppInfo>()
        val packages = activity.packageManager.getInstalledPackages(0)
        if (!includeSystemApps)
            packages.removeAll { it.isSystemApp() }
        val jobs = mutableListOf<Job>()
        for (pkg in packages) {
            jobs += ioCoroutine.launch {
                applicationInfoList += pkg.toAppInfo()
            }
        }
        jobs.joinAll()

        apps.clear()
        apps.addAll(applicationInfoList)

        apps.sortWith { o1, o2 ->
            o1.name.toLowerCase(Locale.ROOT).compareTo(o2.name.toLowerCase(Locale.ROOT))
        }

        return@withContext applicationInfoList
    }

    private fun PackageInfo.toAppInfo(): AppInfo {
        val name = applicationInfo.loadLabel(activity.packageManager)
        val packageName = applicationInfo.packageName
        val size = File(applicationInfo.publicSourceDir).length()

        val iconFile = File(ensureFolderExists(iconsFolder), "${packageName}.png")
        if (!iconFile.exists()) {
            val icon = applicationInfo.loadIcon(activity.packageManager)
            val outStream = FileOutputStream(iconFile)
            drawableToBitmap(icon).compress(Bitmap.CompressFormat.PNG, 30, outStream)
            outStream.flush()
            outStream.close()
        }

        return AppInfo(name.toString(), size, iconFile.absolutePath, packageName, isSystemApp(), applicationInfo.publicSourceDir)
    }

    fun launchApplication(packageName: String) {
        val intent = activity.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null)
            activity.startActivity(intent)
    }

    private fun PackageInfo.isSystemApp(): Boolean {
        return applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap =
                Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        println("${canvas.width}x${canvas.height}")
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}