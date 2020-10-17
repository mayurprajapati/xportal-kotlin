package com.example.mayur.byteshare.fragments.apps

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.AsyncTask
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.mayur.byteshare.MainActivity
import com.example.mayur.byteshare.utils.FileUtils
import com.example.mayur.byteshare.R
import com.example.mayur.byteshare.connection.logger.Logger
import kotlinx.android.synthetic.main.files_layout_file_properties.*
import java.io.File
import java.text.DateFormat
import java.util.*

class AppsAdapter(
        mainActivity: MainActivity,
        recyclerView: RecyclerView,
        private val appsFragment: AppsFragment
) : RecyclerView.Adapter<AppsAdapter.AppHolder>(), SwipeRefreshLayout.OnRefreshListener {

    var cancelSelectionOnClickListener: View.OnClickListener
    var selectAllOnCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null
    var selectAllOnClickListener: View.OnClickListener
    private var packageInfoList: MutableList<PackageInfo>
    private val mainActivity: MainActivity
    val selectedApps = ArrayList<Int>()
    private val context: Context
    private val packageManager: PackageManager

    val selectedItemsCount: Int
        get() = selectedApps.size


    private val selectedPackages: List<ApplicationInfo>
        get() {
            val packages = ArrayList<ApplicationInfo>()
            for (i in selectedApps.indices) {
                val applicationInfo = packageInfoList[selectedApps[i]].applicationInfo
                packages.add(applicationInfo)
            }
            return packages
        }

    init {
        this.context = mainActivity
        this.mainActivity = mainActivity
        packageManager = context.packageManager
        packageInfoList = packageManager.getInstalledPackages(0)
        packageInfoList.sortWith { o1, o2 ->
            o1.applicationInfo.loadLabel(packageManager).toString().toLowerCase()
                    .compareTo(o2.applicationInfo.loadLabel(packageManager).toString().toLowerCase())
        }

        removeSystemPackages()
        cancelSelectionOnClickListener = View.OnClickListener {
            selectedApps.clear()
            notifyDataSetChanged()
            appsFragment.showCount(0)
            appsFragment.checkBoxSelectAll.isChecked = false
        }

        selectAllOnClickListener = View.OnClickListener {
            if (appsFragment.checkBoxSelectAll.isChecked) {
                selectedApps.clear()
                for (i in packageInfoList.indices) {
                    selectedApps.add(i)
                    notifyDataSetChanged()
                }
                appsFragment.showCount(selectedApps.size)
            } else {
                selectedApps.clear()
                appsFragment.showCount(selectedApps.size)
                notifyDataSetChanged()
            }
        }
    }


    private fun removeSystemPackages() {
        Thread(Runnable {
            val packageInfo = ArrayList(packageInfoList)
            for (p in packageInfo) {
                if (isSystemPackage(p))
                    packageInfoList.remove(p)
            }
        }).start()
    }

    private fun isSystemPackage(pkgInfo: PackageInfo): Boolean {
        return pkgInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppHolder {
        val myView =
            LayoutInflater.from(context).inflate(R.layout.apps_layout_item_apps, parent, false)
        val appHolder = AppHolder(myView)
        myView.setOnClickListener {
            if (selectedApps.contains(appHolder.adapterPosition)) {
                selectedApps.remove(Integer.valueOf(appHolder.adapterPosition))
                appHolder.selected.visibility = View.GONE
            } else {
                appHolder.selected.visibility = View.VISIBLE
                selectedApps.add(appHolder.adapterPosition)
            }


            if (selectedApps.size == packageInfoList.size) {
                appsFragment.checkBoxSelectAll.isChecked = true
            }

            if (selectedApps.size == packageInfoList.size - 1)
                appsFragment.checkBoxSelectAll.isChecked = false
            appsFragment.showCount(selectedApps.size)
        }

        myView.setOnLongClickListener {
            val pos = appHolder.layoutPosition
            val alertDialog = AlertDialog.Builder(mainActivity).create()
            if (alertDialog.window != null) {
                alertDialog.window!!.setBackgroundDrawableResource(R.drawable.background_dialogbox)
                val dialogView = LayoutInflater.from(mainActivity).inflate(
                    R.layout.files_layout_file_properties,
                    mainActivity.filePropertiesRoot as ViewGroup
                )
                alertDialog.setView(dialogView)

                alertDialog.setButton(
                    DialogInterface.BUTTON_NEGATIVE,
                    "Cancel"
                ) { dialog, _ -> dialog.dismiss() }

                val fileName = dialogView.findViewById<TextView>(R.id.filePropertiesFileName)
                val fileSize = dialogView.findViewById<TextView>(R.id.filePropertiesFileSize)
                val dateModified =
                    dialogView.findViewById<TextView>(R.id.filePropertiesDateModified)
                val filePath = dialogView.findViewById<TextView>(R.id.filePropertiesFilePath)

                val applicationInfo = packageInfoList[pos].applicationInfo

                val file = File(applicationInfo.publicSourceDir)
                if (file.exists()) {
                    fileName.text = applicationInfo.loadLabel(packageManager)
                    fileSize.text = FileUtils.formatFileSize(file.length())
                    val date = Date(file.lastModified())
                    var dateFormat = DateFormat.getDateInstance().format(date)
                    dateFormat += "\n" + DateFormat.getTimeInstance().format(date)
                    dateModified.text = dateFormat
                    filePath.text = file.absolutePath
                }
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Launch") { dialog, _ ->
                    dialog.dismiss()
                    val intent =
                        packageManager.getLaunchIntentForPackage(packageInfoList[pos].packageName)
                    if (intent != null)
                        mainActivity.startActivity(intent)
                }
                alertDialog.show()
            }
            true
        }
        return appHolder
    }


    override fun onBindViewHolder(holder: AppHolder, position: Int) {
        if (selectedApps.contains(position)) {
            holder.selected.visibility = View.VISIBLE
        } else
            holder.selected.visibility = View.GONE
        val applicationInfo = packageInfoList[position].applicationInfo
        if (holder.cardView.tag != null) {
            val imageLoader = holder.cardView.tag as ImageLoader
            imageLoader.cancel(true)
        }
        val imageLoader = ImageLoader(holder, applicationInfo, packageManager)
        imageLoader.execute()
        holder.cardView.tag = imageLoader
    }

    override fun getItemCount(): Int {
        return packageInfoList.size
    }

    fun clearCount() {
        selectedApps.clear()
        notifyDataSetChanged()
    }

    override fun onRefresh() {
        Thread(Runnable {
            packageInfoList = packageManager.getInstalledPackages(0)
            packageInfoList.sortWith(Comparator { o1, o2 ->
                o1.applicationInfo.loadLabel(
                    packageManager
                ).toString().toLowerCase()
                    .compareTo(o2.applicationInfo.loadLabel(packageManager).toString().toLowerCase())
            })
            removeSystemPackages()
            MainActivity.handler.postDelayed({
                notifyDataSetChanged()
                appsFragment.refreshCompleted()
            }, 500)
        }).start()

    }

    fun removeSelectedPackages() {
        Thread(Runnable {
            for (i in selectedApps.indices) {
                val packageInfo = packageInfoList[selectedApps[i]]
                val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE)
                intent.data = Uri.parse("package:" + packageInfo.packageName)
                mainActivity.startActivityForResult(intent, 1111)
            }
        }).start()

    }


    fun showProperties() {

        val alertDialog = android.app.AlertDialog.Builder(mainActivity).create()
        if (alertDialog.window != null)
            alertDialog.window!!.setBackgroundDrawableResource(R.drawable.background_dialogbox)
        val dialogView = LayoutInflater.from(mainActivity).inflate(
            R.layout.files_layout_file_properties,
            mainActivity.findViewById<View>(R.id.filePropertiesRoot) as ViewGroup
        )
        dialogView.findViewById<View>(R.id.textViewPropertiesLastModified).visibility = View.GONE
        dialogView.findViewById<View>(R.id.textViewPropertiesPath).visibility = View.GONE
        dialogView.findViewById<View>(R.id.filePropertiesFilePath).visibility = View.GONE
        dialogView.findViewById<View>(R.id.filePropertiesDateModified).visibility = View.GONE

        val fileSize = dialogView.findViewById<TextView>(R.id.filePropertiesFileSize)

        Thread(Runnable {
            val fileNames = StringBuilder()
            val len = longArrayOf(0)
            for (a in selectedPackages) {
                val file = File(a.publicSourceDir)
                if (file.exists()) {
                    fileNames.append(a.loadLabel(packageManager)).append("\n")
                    len[0] += file.length()
                }
            }

            MainActivity.handler.post {
                fileSize.text = FileUtils.formatFileSize(len[0])
                alertDialog.setTitle("Properties")
                (dialogView.findViewById<View>(R.id.filePropertiesFileName) as TextView).text =
                        fileNames
                alertDialog.setView(dialogView)

                alertDialog.setButton(
                    DialogInterface.BUTTON_POSITIVE,
                    "OKAY"
                ) { dialog, _ -> dialog.dismiss() }
                alertDialog.show()
            }
        }).start()
    }

    fun getAllSelectedApps(): List<ApkInfo> {
        val fl = ArrayList<ApkInfo>()
        for (i in selectedApps.indices) {
            val applicationInfo = packageInfoList[selectedApps[i]].applicationInfo
            var name = applicationInfo.loadLabel(packageManager) as String
            name = "$name.apk"
            Logger.log("APP NAME $name", this@AppsAdapter)
            val file = File(applicationInfo.publicSourceDir)

            if (file.exists()) {
                fl.add(ApkInfo(file, name))
            }
        }
        return fl
    }

    private class ImageLoader(
            private val appHolder: AppHolder,
            private val applicationInfo: ApplicationInfo,
            private val packageManager: PackageManager
    ) : AsyncTask<Void, Void, Void>() {

        private var drawable: Drawable? = null
        private var appName: String? = null
        private var appSize: String? = null

        override fun onPreExecute() {
            super.onPreExecute()
            if (!isCancelled) {
                appHolder.appIcon.setImageDrawable(null)
            }
        }

        override fun onPostExecute(aVoid: Void?) {
//            super.onPostExecute(aVoid)

            if (!isCancelled) {
                appHolder.appName.text = appName
                Glide.with(appHolder.appIcon)
                    .asDrawable()
                    .load(drawable)
                    .into(appHolder.appIcon)
                appHolder.appSize.text = appSize
            }
        }

        override fun doInBackground(vararg voids: Void): Void? {
            if (!isCancelled) {
                drawable = packageManager.getApplicationIcon(applicationInfo)
                appName = applicationInfo.loadLabel(packageManager).toString()
                appSize = FileUtils.formatFileSize(File(applicationInfo.publicSourceDir).length())
            }
            return null
        }
    }

    class ApkInfo(var f: File, var appName: String)

    class AppHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var appIcon: ImageView = itemView.findViewById(R.id.imgIcon)
        internal var appName: TextView = itemView.findViewById(R.id.txtAppName)
        internal var appSize: TextView = itemView.findViewById(R.id.appsSize)
        internal var cardView: CardView = itemView.findViewById(R.id.cardviewApp)
        internal var selected: ImageView = itemView.findViewById(R.id.imageViewCheckedApp)
    }
}


































