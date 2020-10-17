package com.example.mayur.byteshare.hider

import android.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import android.content.DialogInterface
import android.os.FileObserver
import com.google.android.material.snackbar.Snackbar
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.*
import com.example.mayur.byteshare.MainActivity
import com.example.mayur.byteshare.utils.FileUtils
import com.example.mayur.byteshare.R
import com.example.mayur.byteshare.fragments.files.FileSizeGenerator
import com.example.mayur.byteshare.fragments.files.icon.Icons
import java.io.File
import java.io.FilenameFilter
import java.io.IOException
import java.text.DateFormat
import java.util.*

class HiddenFilesAdapter(
        private val hiderFragment: HiderFragment,
        private val hiderActivity: HiderActivity
) : RecyclerView.Adapter<HiddenFilesAdapter.HiddenViewHolder>(), LifecycleObserver {

    private val fileObserver: FileObserver
    private var hiddenFiles: MutableList<File>? = null
    private val removeTempFiles: FilenameFilter
    var cancelSelectionClickListener: View.OnClickListener
    var selectAllOnClickListener: View.OnClickListener
    val selectedFiles: MutableList<Int>

    val selectedItemsCount: Int
        get() = selectedFiles.size

    init {
        this.hiddenFiles = ArrayList()
        selectedFiles = ArrayList()
        removeTempFiles = FilenameFilter { _, name -> !name.endsWith(".temp") }

        cancelSelectionClickListener = View.OnClickListener {
            clearCount()
            hiderFragment.showCount(0)
            hiderFragment.checkBoxSelectAll.isChecked = false
        }

        selectAllOnClickListener = View.OnClickListener {
            if (hiderFragment.checkBoxSelectAll.isChecked) {
                clearCount()
                for (i in hiddenFiles!!.indices)
                    selectedFiles.add(i)
                notifyDataSetChanged()
                hiderFragment.showCount(selectedFiles.size)
            } else {
                clearCount()
                hiderFragment.showCount(selectedFiles.size)
                notifyDataSetChanged()
            }
        }

        hiddenFiles = Arrays.asList(*Hider.filesRoot.listFiles(removeTempFiles))

        sortFiles()

        fileObserver = object : FileObserver(Hider.filesRoot.absolutePath) {
            override fun onEvent(event: Int, path: String?) {
                when (event) {
                    FileObserver.DELETE, FileObserver.CREATE -> MainActivity.handler.post {
                        hiddenFiles = Arrays.asList(*Hider.filesRoot.listFiles())
                        sortFiles()
                        notifyDataSetChanged()
                    }
                }
            }
        }
    }

    fun removeSelectedItemsFromList() {
        val temp = hiddenFiles
    }

    fun deleteSelectedFiles() {
        val temp = hiddenFiles
        val selectedFiles = getAllSelectedFiles()
        for (f in selectedFiles) {
            if (f.exists())
                f.delete()
            temp!!.remove(f)
        }

        hiddenFiles = temp
        MainActivity.handler.post { notifyDataSetChanged() }
        //        AlertDialog alertDialog = new AlertDialog.Builder(hiderActivity).create();
        //        alertDialog.setTitle("Are you sure?\nFollowing files will be deleted.");
        //        if (alertDialog.getWindow() != null) {
        //            alertDialog.getWindow().setBackgroundDrawableResource(R.drawable.background_dialogbox);
        //            StringBuilder fileNames = new StringBuilder();
        //
        //            for (File f : selectedFiles) {
        //                fileNames.append(f.getName()).append("\n");
        //            }
        //
        //            alertDialog.setMessage(fileNames);
        //            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Delete", new DialogInterface.OnClickListener() {
        //                @Override
        //                public void onClick(DialogInterface dialog, int which) {
        //                    dialog.dismiss();
        //
        //                    for (File file : selectedFiles) {
        //                        FileUtils.deleteFolderOrFile(file);
        //                    }
        //                    Toast.makeText(hiderActivity, "File(s) deleted successfully. ", Toast.LENGTH_SHORT).show();
        ////                    onRefresh();
        //                    clearCount();
        //                }
        //            });
        //
        //            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
        //                @Override
        //                public void onClick(DialogInterface dialog, int which) {
        //                    dialog.dismiss();
        //                }
        //            });
        //
        //            alertDialog.show();
        //        }

    }


    fun getAllSelectedFiles(): List<File> {
        val stringBuilder = StringBuilder()
        val temp = ArrayList<File>()
        for (i in selectedFiles.indices) {
            val f = hiddenFiles!![selectedFiles[i]]
            if (f.exists())
                temp.add(f)
            else
                stringBuilder.append(f.name).append("\n")
        }

        if (stringBuilder.isNotEmpty()) {
            stringBuilder.append("File(s) doesn't exists.")
            Toast.makeText(hiderActivity, stringBuilder, Toast.LENGTH_SHORT).show()
        }
        return temp
    }

    fun clearCount() {
        notifyDataSetChanged()
        selectedFiles.clear()
        hiderFragment.showCount(selectedFiles.size)
    }


    private fun sortFiles() {
        hiderFragment.filesAvailable(hiddenFiles!!.size)
        Collections.sort(hiddenFiles, Comparator { o1, o2 ->
            if (o1.isDirectory && !o2.isDirectory)
                return@Comparator -1
            if (!o1.isDirectory && o2.isDirectory) 1 else o1.name.toLowerCase()
                .compareTo(o2.name.toLowerCase())
        })
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): HiddenViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.files_layout_file_item, viewGroup, false)
        val hiddenViewHolder = HiddenViewHolder(view)

        hiddenViewHolder.cardView.setOnClickListener(View.OnClickListener { v ->
            val pos = hiddenViewHolder.adapterPosition
            if (selectedFiles.size > 0) {
                if (selectedFiles.contains(pos)) {
                    selectedFiles.remove(Integer.valueOf(pos))
                    hiddenViewHolder.checkBox.isChecked = false
                    hiddenViewHolder.selected.visibility = View.GONE
                    return@OnClickListener
                }
                hiddenViewHolder.checkBox.isChecked = true
                if (!selectedFiles.contains(pos))
                    selectedFiles.add(hiddenViewHolder.layoutPosition)
                hiddenViewHolder.selected.visibility = View.VISIBLE
                return@OnClickListener
            }
            val file = hiddenFiles!![pos]
            if (file.isDirectory) {
                val temp = Arrays.asList(*file.listFiles(removeTempFiles))
                if (temp.size > 0) {
                    hiddenFiles = temp
                    sortFiles()
                    //                        prevPositions.push(linearLayoutManager.findLastVisibleItemPosition());
                    notifyDataSetChanged()
                } else {
                    Snackbar.make(v, "Folder is empty.", Snackbar.LENGTH_LONG)
                        .setAction("Delete") { v ->
                            if (file.delete()) {
                                notifyItemRemoved(pos)
                                //                                    onRefresh();
                                Snackbar.make(
                                    v,
                                    "Folder deleted successfully.",
                                    Snackbar.LENGTH_LONG
                                ).show()
                            } else
                                Snackbar.make(
                                    v,
                                    "Couldn't delete folder.",
                                    Snackbar.LENGTH_LONG
                                ).show()
                        }.show()
                }
            } else {
                //                    FileUtils.fireIntent(file, hiderActivity);
                val original = hiddenFiles!![hiddenViewHolder.adapterPosition]
                var fileName = StringBuilder(original.name)
                if (fileName.toString().endsWith(".xpo"))
                    fileName = StringBuilder(fileName.toString().replace(".xpo", ""))
                val ext = FileUtils.getExt(fileName.toString())
                val b = fileName.toString().toByteArray()
                fileName = StringBuilder()
                for (aB in b) {
                    fileName.append(java.lang.Byte.toString(aB))
                }

                fileName.append(".").append(ext)
                val file1 = File(hiderActivity.externalCacheDir, fileName.toString())
                try {
                    file1.createNewFile()
                    Thread(Runnable { Hider.decrypt(file1, original, hiderActivity) }).start()
                    FileUtils.fireIntent(file1, hiderActivity)
                    file1.deleteOnExit()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        })

        hiddenViewHolder.cardView.setOnLongClickListener(View.OnLongClickListener {
            val f = hiddenFiles!![hiddenViewHolder.adapterPosition]
            val alertDialog = AlertDialog.Builder(hiderActivity).create()
            if (alertDialog.window != null)
                alertDialog.window!!.setBackgroundDrawableResource(R.drawable.background_dialogbox)
            val dialogView = LayoutInflater.from(hiderActivity).inflate(
                R.layout.files_layout_file_properties,
                hiderActivity.findViewById<View>(R.id.filePropertiesRoot) as ViewGroup
            )
            alertDialog.setTitle("Properties")
            val date = Date(f.lastModified())
            (dialogView.findViewById<View>(R.id.filePropertiesFileName) as TextView).text =
                    f.name.replace(".xpo", "")
            (dialogView.findViewById<View>(R.id.filePropertiesFilePath) as TextView).text =
                    f.absolutePath
            var dateFormat = DateFormat.getDateInstance().format(date)
            dateFormat += "\n" + DateFormat.getTimeInstance().format(date)
            (dialogView.findViewById<View>(R.id.filePropertiesDateModified) as TextView).text =
                    dateFormat
            val fileSize = dialogView.findViewById<TextView>(R.id.filePropertiesFileSize)
            fileSize.text = FileUtils.formatFileSize(f.length())
            alertDialog.setView(dialogView)
            if (f.isDirectory) {
                val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBarFileSize)
                progressBar.visibility = View.VISIBLE
                val fileList = ArrayList<File>()
                fileList.add(f)
                val sizeGenerator = FileSizeGenerator(fileList,
                    object: FileSizeGenerator.OnProgressUpdateListener {
                        override fun onProgressUpdate(size: Long) {
                            fileSize.text = FileUtils.formatFileSize(size)
                            progressBar.visibility = View.GONE
                        }
                    })
                sizeGenerator.execute()
                alertDialog.setButton(
                    DialogInterface.BUTTON_POSITIVE,
                    "OKAY"
                ) { dialog, _ -> dialog.dismiss() }
                alertDialog.setOnDismissListener { sizeGenerator.cancel(true) }
                alertDialog.show()
                return@OnLongClickListener true
            }
            alertDialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                "OKAY"
            ) { dialog, _ -> dialog.dismiss() }
            alertDialog.show()
            true
        })


        hiddenViewHolder.checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                if (!selectedFiles.contains(hiddenViewHolder.adapterPosition))
                    selectedFiles.add(hiddenViewHolder.layoutPosition)
                hiddenViewHolder.selected.visibility = View.VISIBLE
            } else {
                hiddenViewHolder.selected.visibility = View.GONE
                selectedFiles.remove(Integer.valueOf(hiddenViewHolder.layoutPosition))
            }

            if (selectedFiles.size == hiddenFiles!!.size) {
                hiderFragment.checkBoxSelectAll.isChecked = true
            }

            if (selectedFiles.size == hiddenFiles!!.size - 1)
                hiderFragment.checkBoxSelectAll.isChecked = false
            hiderFragment.showCount(selectedFiles.size)
        }
        return hiddenViewHolder
    }

    fun showProperties() {
        val alertDialog = AlertDialog.Builder(hiderFragment.context).create()
        if (alertDialog.window != null)
            alertDialog.window!!.setBackgroundDrawableResource(R.drawable.background_dialogbox)
        val dialogView =
            View.inflate(hiderFragment.context, R.layout.files_layout_file_properties, null)
        dialogView.findViewById<View>(R.id.textViewPropertiesLastModified).visibility = View.GONE
        dialogView.findViewById<View>(R.id.textViewPropertiesPath).visibility = View.GONE
        dialogView.findViewById<View>(R.id.filePropertiesFilePath).visibility = View.GONE
        dialogView.findViewById<View>(R.id.filePropertiesDateModified).visibility = View.GONE


        val fileSize = dialogView.findViewById<TextView>(R.id.filePropertiesFileSize)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBarFileSize)
        progressBar.visibility = View.VISIBLE
        val fileNames = StringBuilder()
        val len = longArrayOf(0)
        val folderList = ArrayList<File>()
        for (file in getAllSelectedFiles()) {
            fileNames.append(file.name).append("\n")

            if (file.isDirectory) {
                folderList.add(file)
            } else {
                len[0] += file.length()
            }
        }

        fileSize.text = FileUtils.formatFileSize(len[0])

        val sizeGenerator = FileSizeGenerator(folderList,object: FileSizeGenerator.OnProgressUpdateListener{
                override fun onProgressUpdate(size: Long) {
                    len[0] += size
                    fileSize.text = FileUtils.formatFileSize(len[0])
                    progressBar.visibility = View.GONE
                }
            })
        alertDialog.setTitle("Properties")
        (dialogView.findViewById<View>(R.id.filePropertiesFileName) as TextView).text = fileNames
        alertDialog.setView(dialogView)


        sizeGenerator.execute()
        alertDialog.setButton(
            DialogInterface.BUTTON_POSITIVE,
            "OKAY"
        ) { dialog, _ -> dialog.dismiss() }
        alertDialog.setOnDismissListener { sizeGenerator.cancel(true) }
        alertDialog.show()
    }


    override fun onBindViewHolder(holder: HiddenViewHolder, pos: Int) {
        val file = hiddenFiles!![pos]
        var fileName = file.name
        if (fileName.startsWith("."))
            fileName = fileName.replace(".", "")
        if (fileName.endsWith(".xpo"))
            fileName = fileName.replace(".xpo", "")

        holder.fileName.text = fileName

        holder.fileType.text =
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(FileUtils.getExt(file))

        if (file.isDirectory) {
            holder.fileType.visibility = View.GONE
            holder.fileSize.visibility = View.GONE
        } else {
            holder.fileType.visibility = View.VISIBLE
            holder.fileSize.visibility = View.VISIBLE
            holder.fileSize.text = FileUtils.formatFileSize(file.length())
        }

        if (selectedFiles.contains(pos)) {
            holder.checkBox.isChecked = true
            holder.selected.visibility = View.VISIBLE
        } else {
            holder.selected.visibility = View.GONE
            holder.checkBox.isChecked = false
        }

        if (selectedFiles.size == hiddenFiles!!.size) {
            hiderFragment.checkBoxSelectAll.isChecked = true
        }

        if (selectedFiles.size == hiddenFiles!!.size - 1)
            hiderFragment.checkBoxSelectAll.isChecked = false
        hiderFragment.showCount(selectedFiles.size)

        Icons.setIcon(fileName, hiderActivity.packageManager, holder.icon, file.isDirectory)

    }

    override fun getItemCount(): Int {
        return hiddenFiles!!.size
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun startFileObserver() {
        fileObserver.startWatching()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun stopFileObserver() {
        fileObserver.stopWatching()
    }

    //------------------------------------------------------------------------//
    class HiddenViewHolder(internal var cardView: View) : RecyclerView.ViewHolder(cardView) {
        internal var icon: ImageView = cardView.findViewById(R.id.fileIcon)
        internal var fileName: TextView = cardView.findViewById(R.id.fileName)
        internal var fileSize: TextView = cardView.findViewById(R.id.fileSize)
        internal var fileType: TextView = cardView.findViewById(R.id.fileType)
        internal var checkBox: CheckBox = cardView.findViewById(R.id.checkBox)
        internal var selected: ImageView = cardView.findViewById(R.id.fileItemSelected)
    }
}