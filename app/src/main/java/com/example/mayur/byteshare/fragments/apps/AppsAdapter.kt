package com.example.mayur.byteshare.fragments.apps

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
import com.example.mayur.byteshare.bloc.apps.AppInfo
import com.example.mayur.byteshare.bloc.apps.AppsBloc
import com.example.mayur.byteshare.bloc.interfaces.getAllSelected
import com.example.mayur.byteshare.bloc.interfaces.unselectAll
import com.example.mayur.byteshare.bloc.interfaces.selectAll
import com.example.mayur.byteshare.bloc.interfaces.selectedCount
import com.example.mayur.byteshare.connection.logger.Logger
import com.example.mayur.byteshare.ioCoroutine
import com.example.mayur.byteshare.uiCoroutine
import com.google.android.material.card.MaterialCardView
import kotlinx.android.synthetic.main.apps_layout_item_apps.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.*

class AppsAdapter(
        val mainActivity: MainActivity,
        recyclerView: RecyclerView,
        private val appsFragment: AppsFragment
) : RecyclerView.Adapter<AppsAdapter.AppHolder>(), SwipeRefreshLayout.OnRefreshListener {
    val appsBloc = AppsBloc.init(mainActivity)

    var cancelSelectionOnClickListener: View.OnClickListener
    var selectAllOnCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null
    var selectAllOnClickListener: View.OnClickListener

    init {
        ioCoroutine.launch {
            appsBloc.getInstalledApps(includeSystemApps = true)
            ioCoroutine.launch { notifyDataSetChanged() }
        }
        cancelSelectionOnClickListener = View.OnClickListener {
            appsBloc.apps.unselectAll()
            appsFragment.showCount(0)
            appsFragment.checkBoxSelectAll.isChecked = false
            notifyDataSetChanged()
        }

        selectAllOnClickListener = View.OnClickListener {
            if (appsFragment.checkBoxSelectAll.isChecked) {
                appsBloc.apps.selectAll()
            } else {
                appsBloc.apps.unselectAll()
            }
            notifyDataSetChanged()
            appsFragment.showCount(appsBloc.apps.selectedCount())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppHolder {
        val myView =
                LayoutInflater.from(mainActivity).inflate(R.layout.apps_layout_item_apps, parent, false)
        val appHolder = AppHolder(myView)

        myView.setOnClickListener {
            appHolder.cardView.toggle()
            val app = appsBloc.apps[appHolder.adapterPosition]
            app.isSelected = !app.isSelected

            val selectedCount = appsBloc.apps.selectedCount()
            if (selectedCount == appsBloc.apps.size)
                appsFragment.checkBoxSelectAll.isChecked = true

            if (selectedCount == appsBloc.apps.size - 1)
                appsFragment.checkBoxSelectAll.isChecked = false
            appsFragment.showCount(selectedCount)
        }

        myView.setOnLongClickListener {
            val pos = appHolder.layoutPosition
            val alertDialog = AlertDialog.Builder(mainActivity).create()
            if (alertDialog.window != null) {
                alertDialog.window!!.setBackgroundDrawableResource(R.drawable.background_dialogbox)
                val dialogView = LayoutInflater.from(mainActivity).inflate(
                        R.layout.files_layout_file_properties,
                        mainActivity.findViewById(R.id.filePropertiesRoot) as ViewGroup?, true
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

                val app = appsBloc.apps[pos]

                val file = File(app.path)
                if (file.exists()) {
                    fileName.text = app.name
                    fileSize.text = FileUtils.formatFileSize(file.length())
                    val date = Date(file.lastModified())
                    var dateFormat = DateFormat.getDateInstance().format(date)
                    dateFormat += "\n" + DateFormat.getTimeInstance().format(date)
                    dateModified.text = dateFormat
                    filePath.text = file.absolutePath
                }
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Launch") { dialog, _ ->
                    dialog.dismiss()
                    appsBloc.launchApplication(app.packageName)
                }
                alertDialog.show()
            }
            true
        }
        return appHolder
    }

    override fun onBindViewHolder(holder: AppHolder, position: Int) {
        val app = appsBloc.apps[position]

        holder.cardView.isChecked = app.isSelected
        if (holder.cardView.tag != null) {
            val imageLoader = holder.cardView.tag as ImageLoader
            imageLoader.cancel(true)
        }
        holder.cardView.tag = ImageLoader(holder, app).execute()
    }

    override fun getItemCount(): Int {
        return appsBloc.apps.size
    }

    fun clearCount() {
        appsBloc.apps.unselectAll()
        notifyDataSetChanged()
    }

    override fun onRefresh() {
        ioCoroutine.launch {
            appsBloc.getInstalledApps(refresh = true, includeSystemApps = true)
            uiCoroutine.launch {
                notifyDataSetChanged()
                appsFragment.showCount(0)
                appsFragment.refreshCompleted()
            }
        }
    }

    suspend fun uninstallSelectedPackages() = withContext(Dispatchers.IO) {
        for (app in appsBloc.apps.getAllSelected()) {
            val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE)
            intent.data = Uri.parse("package:" + app.packageName)
            mainActivity.startActivityForResult(intent, 1111)
        }
    }

    fun showProperties() {
        val alertDialog = android.app.AlertDialog.Builder(mainActivity).create()
        if (alertDialog.window != null)
            alertDialog.window!!.setBackgroundDrawableResource(R.drawable.background_dialogbox)
        val dialogView = LayoutInflater.from(mainActivity).inflate(
                R.layout.files_layout_file_properties,
                mainActivity.findViewById<View>(R.id.filePropertiesRoot) as ViewGroup?
        )
        dialogView.findViewById<View>(R.id.textViewPropertiesLastModified).visibility = View.GONE
        dialogView.findViewById<View>(R.id.textViewPropertiesPath).visibility = View.GONE
        dialogView.findViewById<View>(R.id.filePropertiesFilePath).visibility = View.GONE
        dialogView.findViewById<View>(R.id.filePropertiesDateModified).visibility = View.GONE

        val fileSize = dialogView.findViewById<TextView>(R.id.filePropertiesFileSize)

        ioCoroutine.launch {
            val fileNames = StringBuilder()
            val len = longArrayOf(0)
            for (a in appsBloc.apps.getAllSelected()) {
                val file = File(a.path)
                if (file.exists()) {
                    fileNames.append(a.name).append("\n")
                    len[0] += file.length()
                }
            }

            uiCoroutine.launch {
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
        }.start()
    }

    fun getAllSelectedApps(): List<ApkInfo> {
        val fl = ArrayList<ApkInfo>()
        for (app in appsBloc.apps.getAllSelected()) {
            var name = app.name
            name = "$name.apk"
            Logger.log("APP NAME $name", this@AppsAdapter)
            val file = File(app.path)

            if (file.exists()) {
                fl.add(ApkInfo(file, name))
            }
        }
        return fl
    }

    private class ImageLoader(
            private val appHolder: AppHolder,
            private val applicationInfo: AppInfo
    ) : AsyncTask<Void, Void, Void>() {

        private var drawable: Drawable? = null
        private var appName: String? = null
        private var appSize: String? = null
        private var iconFile: File? = null

        override fun onPreExecute() {
            super.onPreExecute()
//            if (!isCancelled) {
//                appHolder.appIcon.setImageDrawable(null)
//            }
        }

        override fun onPostExecute(aVoid: Void?) {
//            super.onPostExecute(aVoid)

            if (!isCancelled && appHolder.appName.text != appName) {
                appHolder.appName.text = appName
                appHolder.appIcon.setImageURI(Uri.fromFile(iconFile))
//                Glide.with(appHolder.appIcon)
//                        .asDrawable()
//                        .load(drawable)
//                        .into(appHolder.appIcon)
                appHolder.appSize.text = appSize
            }
        }

        override fun doInBackground(vararg voids: Void): Void? {
            if (!isCancelled) {
                appName = applicationInfo.name
                appSize = FileUtils.formatFileSize(applicationInfo.size)
                iconFile = File(applicationInfo.icon)
            }
            return null
        }
    }

    class ApkInfo(var f: File, var appName: String)

    class AppHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var appIcon: ImageView = itemView.findViewById(R.id.imgIcon)
        internal var appName: TextView = itemView.findViewById(R.id.txtAppName)
        internal var appSize: TextView = itemView.findViewById(R.id.appsSize)
        internal var cardView: MaterialCardView = itemView.cardviewApp
//        internal var selected: ImageView = itemView.findViewById(R.id.imageViewCheckedApp)
    }
}


































