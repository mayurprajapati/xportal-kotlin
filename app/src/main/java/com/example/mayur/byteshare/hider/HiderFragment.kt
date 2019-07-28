package com.example.mayur.xportal.hider


import android.content.DialogInterface
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.mayur.xportal.MainActivity
import com.example.mayur.xportal.R
import com.example.mayur.xportal.connection.FileInfo
import com.example.mayur.xportal.connection.hotspot.TransferHotspot
import com.example.mayur.xportal.connection.wifi.TransferWifi
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

/**
 * A simple [Fragment] subclass.
 */
class HiderFragment : Fragment() {

    internal lateinit var cancelSelection: ImageButton
    internal lateinit var checkBoxSelectAll: CheckBox
    private lateinit var mainActivity: MainActivity
    private lateinit var hiddenFilesAdapter: HiddenFilesAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var cardViewCount: CardView
    private lateinit var fileProperties: ImageButton
    private lateinit var fileDelete: ImageButton
    private lateinit var textViewCount: TextView
    private lateinit var btnSend: Button
    private lateinit var noFilesToShow: View


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val view = inflater.inflate(R.layout.hider_fragment_hider, container, false)
        noFilesToShow = view.findViewById(R.id.noHiddenFilesFound)
        recyclerView = view.findViewById(R.id.hiddenFilesRecyclerView)
        mainActivity = activity?.parent as MainActivity
        hiddenFilesAdapter = HiddenFilesAdapter(this, activity as HiderActivity)
        Objects.requireNonNull<FragmentActivity>(activity).getLifecycle()
            .addObserver(hiddenFilesAdapter)
        recyclerView.adapter = hiddenFilesAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        cardViewCount = view.findViewById(R.id.cardViewHiddenFiles)

        fileProperties = cardViewCount.findViewById(R.id.fileProperties)
        fileDelete = cardViewCount.findViewById(R.id.btnFileDelete)
        textViewCount = cardViewCount.findViewById(R.id.txtSelectionCount)
        cancelSelection = cardViewCount.findViewById(R.id.cancelSelectionImageButton)
        checkBoxSelectAll = cardViewCount.findViewById(R.id.selectAllCheckBox)
        cancelSelection.setOnClickListener(hiddenFilesAdapter.cancelSelectionClickListener)
        checkBoxSelectAll.setOnClickListener(hiddenFilesAdapter.selectAllOnClickListener)
        btnSend = cardViewCount.findViewById(R.id.btnSend)
        btnSend.setOnClickListener {
            if (MainActivity.isConnected) {
                if (MainActivity.isSender) {
                    val sender = TransferHotspot.getSender(mainActivity, false)
                    val s = hiddenFilesAdapter.getAllSelectedFiles()
                    if (sender != null) {
                        val fileInfos = ArrayList<FileInfo>()
                        for (i in 0 until hiddenFilesAdapter.selectedItemsCount) {
                            fileInfos.add(FileInfo(s[i], s[i].name.replace(".xpo", ""), true))
                        }
                        sender.sendFiles(fileInfos)

                        MainActivity.handler.post {
                            MainActivity.handler.post {
                                hiddenFilesAdapter.clearCount()
                                cardViewCount.visibility = View.GONE
                            }
                        }
                    }
                } else {
                    Thread(Runnable {
                        val s = hiddenFilesAdapter.getAllSelectedFiles()
                        val fileInfos = ArrayList<FileInfo>()
                        for (i in 0 until hiddenFilesAdapter.selectedItemsCount) {
                            fileInfos.add(FileInfo(s[i], s[i].name.replace(".xpo", ""), true))
                        }
                        TransferWifi.getSender(mainActivity).sendFiles(fileInfos)

                        MainActivity.handler.post {
                            hiddenFilesAdapter.clearCount()
                            cardViewCount.visibility = View.GONE
                        }
                    }).start()
                }
            } else {
                Snackbar.make(
                    mainActivity.window.decorView,
                    "Make connection to send file.",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        fileProperties.setOnClickListener { v ->
            val popupMenu = PopupMenu(context, v)
            popupMenu.menu.add("Properties").setIcon(R.drawable.ic_info_black_24dp)
            popupMenu.menu.add("Unhide")
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Properties" -> hiddenFilesAdapter.showProperties()
                    "Unhide" -> {
                        //                                new Thread(new Runnable() {
                        //                                    @Override
                        //                                    public void run() {
                        //                                        for (File f :
                        //                                                hiddenFilesAdapter.getAllSelectedFiles()) {
                        //                                            System.out.println("Decrypting "+f.getName());
                        //                                            File out = new File(Hider.rootUnhideFiles, f.getName().replace(".xpo", ""));
                        //                                            Hider.decrypt(out, f, getContext(), new Hider.OnProgressUpdateListener() {
                        //                                                @Override
                        //                                                public void onProgressUpdate(int progress) {
                        //
                        //                                                }
                        //                                            });
                        //                                            f.delete();
                        //                                            hiddenFilesAdapter.deleteSelectedFiles();
                        //                                        }
                        //                                    }
                        //                                }).start();
                        val dialogView =
                            View.inflate(context, R.layout.hider_layout_file_encrypt, null)
                        val progressBar =
                            dialogView.findViewById<ProgressBar>(R.id.hide_progress_bar)
                        val progress = dialogView.findViewById<TextView>(R.id.hide_progress)
                        val fileName = dialogView.findViewById<TextView>(R.id.hide_file_name)
                        val status = dialogView.findViewById<TextView>(R.id.hide_status)
                        val alertDialog = AlertDialog.Builder(mainActivity).create()
                            alertDialog.window?.setBackgroundDrawableResource(R.drawable.background_dialogbox)
                            alertDialog.setView(dialogView)
                            alertDialog.setCancelable(false)
                            alertDialog.setTitle("Decrypting.. Please wait")
                            alertDialog.setButton(
                                DialogInterface.BUTTON_POSITIVE,
                                "Do in background"
                            ) { dialog, _ -> dialog.dismiss() }
                            alertDialog.show()

                        FileUnHiderAsyncTask(
                            hiddenFilesAdapter.getAllSelectedFiles(),
                            Hider.rootUnhideFiles,
                            object : FileUnHiderAsyncTask.OnProgressUpdateListener {
                                override fun onProgressUpdate(p: Int) {
                                    progress.text = "$p%"
                                    progressBar.progress = p
                                }

                                override fun setFileName(name: String) {
                                    fileName.text = name.replace(".xpo", "")
                                }

                                override fun setStatus(s: String) {
                                    status.text = s
                                }

                                override fun workDone() {
                                    Toast.makeText(
                                        context,
                                        "File(s) decrypted.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    alertDialog.dismiss()
                                }

                                override fun onSpaceNotAvailable() {
                                    Toast.makeText(context, "No enough space..", Toast.LENGTH_SHORT)
                                        .show()
                                    alertDialog.dismiss()
                                }
                            }).execute()
                        hiddenFilesAdapter.clearCount()
                        cardViewCount.visibility = View.GONE
                    }
                }
                true
            }
            popupMenu.show()
        }
        return view
    }

    fun showCount(count: Int) {
        if (count <= 0) {
            cardViewCount.visibility = View.GONE
            return
        }
        cardViewCount.visibility = View.VISIBLE
        textViewCount.text = count.toString()
    }

    fun filesAvailable(available: Int) {
        noFilesToShow.visibility = if (available > 0) View.GONE else View.VISIBLE
    }

    class FileUnHiderAsyncTask(
        internal var files: List<File>,
        private var filesRoot: File,
        private var l: OnProgressUpdateListener
    ) : AsyncTask<Void, Int, Void>() {

        override fun onPostExecute(aVoid: Void) {
            l.workDone()
            super.onPostExecute(aVoid)
        }

        override fun onProgressUpdate(vararg values: Int?) {
            values[0]?.let {
                l.onProgressUpdate(it)
            }
        }

        override fun doInBackground(vararg voids: Void): Void? {
            var i = 0
            for (f in files) {
                if (!Hider.isSpaceAvailable(f)) {
                    l.onSpaceNotAvailable()
                    return null
                }
                i++
                if (f.exists() && !f.isDirectory && f.canRead()) {

                    val finalI = i
                    MainActivity.handler.post {
                        l.setFileName(f.name)
                        l.setStatus("Decrypting " + finalI + " of " + files.size)
                    }
                    try {
                        val fileInputStream = FileInputStream(f)
                        val out = File(filesRoot, f.name.replace(".xpo", ""))
                        if (out.exists())
                            out.delete()
                        out.createNewFile()

                        val fileOutputStream = FileOutputStream(out)
                        val b = ByteArray(8192)
                        var len: Int
                        var prevPer = 0
                        var written: Long = 0

                        while (true) {
                            len = fileInputStream.read(b)
                            if (len < 0)
                                break

                            for (j in 0 until len) {
                                b[j] = (b[j] - 10).toByte()
                            }
                            written += len.toLong()
                            val per = (written * 100.toLong() / f.length()).toInt()

                            if (prevPer != per) {
                                prevPer = per
                                publishProgress(per)
                            }

                            fileOutputStream.write(b, 0, len)
                        }

                        fileOutputStream.flush()
                        fileOutputStream.close()

                        fileInputStream.close()
                        f.delete()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }
            }
            return null
        }

        interface OnProgressUpdateListener {
            fun onProgressUpdate(progress: Int)

            fun setFileName(name: String)

            fun setStatus(s: String)

            fun workDone()

            fun onSpaceNotAvailable()
        }
    }
}