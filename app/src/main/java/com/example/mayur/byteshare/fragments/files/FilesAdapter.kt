package com.example.mayur.xportal.fragments.files

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Environment
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.*
import com.example.mayur.xportal.MainActivity
import com.example.mayur.xportal.R
import com.example.mayur.xportal.fragments.files.icon.Icons
import com.example.mayur.xportal.util.FileUtils
import java.io.File
import java.io.FilenameFilter
import java.text.DateFormat
import java.util.*

class FilesAdapter constructor(
    private val mainActivity: MainActivity,
    private val filesFragment: FilesFragment
) : RecyclerView.Adapter<FilesViewHolder>(), SwipeRefreshLayout.OnRefreshListener {

    private val root: File
    var cancelSelectionClickListener: View.OnClickListener
    var selectAllOnClickListener: View.OnClickListener
    private var fileParent: File
    private var filesList: List<File>
    private lateinit var recyclerView: RecyclerView
    private val fileNameFilter: FilenameFilter
    private val prevPositions: Stack<Int>
    private val fileListStack: Stack<List<File>>
    private val context: Context
    private val selectedFiles: MutableList<Int>


    val selectedItemsCount: Int
        get() = selectedFiles.size

    init {
        selectedFiles = ArrayList()
        context = mainActivity.applicationContext
        fileNameFilter = FilenameFilter { _, name -> !name.startsWith(".") }
        prevPositions = Stack()
        fileListStack = Stack()
        root = Environment.getExternalStorageDirectory()
        fileParent = root
        this.filesList = Arrays.asList(*root.listFiles(fileNameFilter))
        sortFiles()

        cancelSelectionClickListener = View.OnClickListener {
            selectedFiles.clear()
            notifyDataSetChanged()
            filesFragment.showCount(0)
            filesFragment.checkBoxSelectAll.isChecked = false
        }

        selectAllOnClickListener = View.OnClickListener {
            if (filesFragment.checkBoxSelectAll.isChecked) {
                selectedFiles.clear()
                for (i in filesList.indices)
                    selectedFiles.add(i)
                notifyDataSetChanged()
                filesFragment.showCount(selectedFiles.size)
            } else {
                selectedFiles.clear()
                filesFragment.showCount(selectedFiles.size)
                notifyDataSetChanged()
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilesViewHolder {
        val filesViewHolder = FilesViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.files_layout_file_item, parent, false)
        )
        filesViewHolder.cardView.setOnClickListener(View.OnClickListener { v ->
            val pos = filesViewHolder.layoutPosition
            if (selectedFiles.size > 0) {
                if (selectedFiles.contains(pos)) {
                    selectedFiles.remove(Integer.valueOf(pos))
                    filesViewHolder.checkBox.isChecked = false
                    filesViewHolder.selected.visibility = View.GONE
                    return@OnClickListener
                }
                filesViewHolder.checkBox.isChecked = true
                if (!selectedFiles.contains(pos))
                    selectedFiles.add(filesViewHolder.layoutPosition)
                filesViewHolder.selected.visibility = View.VISIBLE
                return@OnClickListener
            }
            val file = filesList[pos]
            if (file.isDirectory) {
                val temp = Arrays.asList(*file.listFiles(fileNameFilter))
                if (temp.size > 0) {
                    fileParent = file
                    filesFragment.updateCurrentPath(file)
                    fileListStack.push(filesList)
                    filesList = temp
                    sortFiles()
                    val linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager
                    prevPositions.push(linearLayoutManager.findLastVisibleItemPosition())
                    notifyDataSetChanged()
                } else {
                    Snackbar.make(v, "Folder is empty.", Snackbar.LENGTH_LONG)
                        .setAction("Delete") { v ->
                            if (file.delete()) {
                                notifyItemRemoved(pos)
                                onRefresh()
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
                FileUtils.fireIntent(file, mainActivity)
            }
        })

        filesViewHolder.cardView.setOnLongClickListener(View.OnLongClickListener {
            val f = filesList[filesViewHolder.adapterPosition]
            val alertDialog = AlertDialog.Builder(mainActivity).create()
            alertDialog.window?.setBackgroundDrawableResource(R.drawable.background_dialogbox)
            val dialogView = LayoutInflater.from(mainActivity).inflate(
                R.layout.files_layout_file_properties,
                mainActivity.findViewById<View>(R.id.filePropertiesRoot) as ViewGroup
            )
            alertDialog.setTitle("Properties")
            val date = Date(f.lastModified())
            (dialogView.findViewById<View>(R.id.filePropertiesFileName) as TextView).text = f.name
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
                    object : FileSizeGenerator.OnProgressUpdateListener {
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


        filesViewHolder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!selectedFiles.contains(filesViewHolder.adapterPosition))
                    selectedFiles.add(filesViewHolder.layoutPosition)
                //                    filesViewHolder.cardView.setAlpha((float) 0.75);
                filesViewHolder.selected.visibility = View.VISIBLE
            } else {
                //                    filesViewHolder.cardView.setAlpha(1);
                filesViewHolder.selected.visibility = View.GONE
                selectedFiles.remove(Integer.valueOf(filesViewHolder.layoutPosition))
            }

            if (selectedFiles.size == filesList.size) {
                filesFragment.checkBoxSelectAll.isChecked = true
            }

            if (selectedFiles.size == filesList.size - 1)
                filesFragment.checkBoxSelectAll.isChecked = false
            filesFragment.showCount(selectedFiles.size)
        }
        return filesViewHolder
    }


    override fun onBindViewHolder(filesViewHolder: FilesViewHolder, position: Int) {
        val f = filesList[position]
        val name = f.name
        filesViewHolder.fileName.text = name

        filesViewHolder.fileType.text =
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(FileUtils.getExt(f))

        if (f.isDirectory) {
            filesViewHolder.fileType.visibility = View.GONE
            filesViewHolder.fileSize.visibility = View.GONE
        } else {
            filesViewHolder.fileType.visibility = View.VISIBLE
            filesViewHolder.fileSize.visibility = View.VISIBLE
            filesViewHolder.fileSize.text = FileUtils.formatFileSize(f.length())
        }

        Icons.setIcon(filesList[position], context.packageManager, filesViewHolder.imageView)
        if (selectedFiles.contains(position)) {
            filesViewHolder.checkBox.isChecked = true
            filesViewHolder.selected.visibility = View.VISIBLE
        } else {
            filesViewHolder.selected.visibility = View.GONE
            filesViewHolder.checkBox.isChecked = false
        }

        if (selectedFiles.size == filesList.size) {
            filesFragment.checkBoxSelectAll.isChecked = true
        }

        if (selectedFiles.size == filesList.size - 1)
            filesFragment.checkBoxSelectAll.isChecked = false
        filesFragment.showCount(selectedFiles.size)

    }

    override fun getItemCount(): Int {
        filesFragment.filesAvailable(filesList.size)
        return filesList.size
    }

    internal fun onBackPressed(): Boolean {
        return if (fileParent == root)
            true
        else {
            filesList = fileListStack.pop()
            fileParent = filesList[0].parentFile
            recyclerView.scrollToPosition(prevPositions.pop())
            selectedFiles.clear()
            filesFragment.checkBoxSelectAll.isChecked = false
            filesFragment.showCount(selectedFiles.size)
            notifyDataSetChanged()
            false
        }
    }

    fun setRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    private fun sortFiles() {
        filesFragment.filesAvailable(filesList.size)
        Collections.sort(filesList, Comparator { o1, o2 ->
            if (o1.isDirectory && !o2.isDirectory)
                return@Comparator -1
            if (!o1.isDirectory && o2.isDirectory) 1 else o1.name.toLowerCase()
                .compareTo(o2.name.toLowerCase())
        })
    }

    fun getAllSelectedFiles(): List<File> {
        val stringBuilder = StringBuilder()
        val temp = ArrayList<File>()
        for (i in selectedFiles.indices) {
            val f = filesList[selectedFiles[i]]
            if (f.exists())
                temp.add(f)
            else
                stringBuilder.append(f.name).append("\n")
        }

        if (stringBuilder.isNotEmpty()) {
            stringBuilder.append("File(s) doesn't exists.")
            Toast.makeText(mainActivity, stringBuilder, Toast.LENGTH_SHORT).show()
        }
        return temp
    }

    fun clearCount() {
        notifyDataSetChanged()
        selectedFiles.clear()
        filesFragment.showCount(selectedFiles.size)
    }

    fun onCurrentPathButtonClicked(tag: File) {
        filesList = Arrays.asList(*tag.listFiles(fileNameFilter))
        sortFiles()
        notifyDataSetChanged()
    }

    override fun onRefresh() {
        filesFragment.swipeRefreshLayout.isRefreshing = true
        Thread(Runnable {
            filesList = Arrays.asList(*fileParent.listFiles(fileNameFilter))
            sortFiles()
            MainActivity.handler.postDelayed({
                notifyDataSetChanged()
                filesFragment.refreshCompleted()
            }, 500)
        }).start()
    }

    fun deleteSelectedFiles() {
        val selectedFiles = getAllSelectedFiles()
        val alertDialog = AlertDialog.Builder(mainActivity).create()
        alertDialog.setTitle("Are you sure?\nFollowing files will be deleted.")
        alertDialog.window?.setBackgroundDrawableResource(R.drawable.background_dialogbox)
        val fileNames = StringBuilder()

        for (f in selectedFiles) {
            fileNames.append(f.name).append("\n")
        }

        alertDialog.setMessage(fileNames)
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Delete") { dialog, _ ->
            dialog.dismiss()

            for (file in selectedFiles) {
                FileUtils.deleteFolderOrFile(file)
            }
            Toast.makeText(mainActivity, "File(s) deleted successfully. ", Toast.LENGTH_SHORT)
                .show()
            onRefresh()
            clearCount()
        }

        alertDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE,
            "Cancel"
        ) { dialog, _ -> dialog.dismiss() }

        alertDialog.show()

    }

    fun showProperties() {

        val alertDialog = AlertDialog.Builder(mainActivity).create()
        alertDialog.window?.setBackgroundDrawableResource(R.drawable.background_dialogbox)
        val dialogView = LayoutInflater.from(mainActivity).inflate(
            R.layout.files_layout_file_properties,
            mainActivity.findViewById<View>(R.id.filePropertiesRoot) as ViewGroup
        )
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

        val sizeGenerator = FileSizeGenerator(folderList,
            object : FileSizeGenerator.OnProgressUpdateListener {
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
}

class FilesViewHolder(var cardView: View) : RecyclerView.ViewHolder(cardView) {

    var fileName: TextView = cardView.findViewById(R.id.fileName)
    var fileSize: TextView = cardView.findViewById(R.id.fileSize)
    var fileType: TextView = cardView.findViewById(R.id.fileType)
    var imageView: ImageView = cardView.findViewById(R.id.fileIcon)
    var checkBox: CheckBox = cardView.findViewById(R.id.checkBox)
    var selected: ImageView = cardView.findViewById(R.id.fileItemSelected)
}