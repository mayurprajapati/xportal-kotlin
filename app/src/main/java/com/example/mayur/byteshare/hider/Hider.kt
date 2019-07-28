package com.example.mayur.xportal.hider

import android.content.Context
import android.content.DialogInterface
import android.os.AsyncTask
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import com.example.mayur.xportal.Constants
import com.example.mayur.xportal.MainActivity
import com.example.mayur.xportal.R

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class Hider
/*
     *
     * JSON{
     * password:""                   //Password to access files
     * file_info:[]                  //Actual file info array that contains abs path for actual file and path to hidden file
     * }
     * */
private constructor(private val mainActivity: MainActivity) {
    init {
        checkFolderExists()
    }

    private fun checkFolderExists() {
        if (pathToHideFiles.exists() && !pathToHideFiles.isDirectory) {
            pathToHideFiles.delete()
            pathToHideFiles.mkdir()
        } else if (!pathToHideFiles.exists())
            pathToHideFiles.mkdir()

        if (filesRoot.exists() && !filesRoot.isDirectory) {
            filesRoot.delete()
            filesRoot.mkdir()
        } else if (!filesRoot.exists())
            filesRoot.mkdir()

        if (rootUnhideFiles.exists() && !rootUnhideFiles.isDirectory) {
            rootUnhideFiles.delete()
            rootUnhideFiles.mkdir()
        } else if (!rootUnhideFiles.exists())
            rootUnhideFiles.mkdir()

        //        if (VIDEO.exists() && !VIDEO.isDirectory()) {
        //            VIDEO.delete();
        //            VIDEO.mkdir();
        //        } else if (!VIDEO.exists())
        //            VIDEO.mkdir();
        //
        //        if (MUSIC.exists() && !MUSIC.isDirectory()) {
        //            MUSIC.delete();
        //            MUSIC.mkdir();
        //        } else if (!MUSIC.exists())
        //            MUSIC.mkdir();
        //
        //        if (IMAGE.exists() && !IMAGE.isDirectory()) {
        //            IMAGE.delete();
        //            IMAGE.mkdir();
        //        } else if (!IMAGE.exists())
        //            IMAGE.mkdir();
        //
        //        if (FILES.exists() && !FILES.isDirectory()) {
        //            FILES.delete();
        //            FILES.mkdir();
        //        } else if (!FILES.exists())
        //            FILES.mkdir();
    }

    fun encrypt(files: List<File>) {
        val dialogView = LayoutInflater.from(mainActivity).inflate(
            R.layout.hider_layout_file_encrypt,
            mainActivity.findViewById<View>(R.id.layout_file_encrypt) as ViewGroup, false
        )
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.hide_progress_bar)
        val progress = dialogView.findViewById<TextView>(R.id.hide_progress)
        val fileName = dialogView.findViewById<TextView>(R.id.hide_file_name)
        val status = dialogView.findViewById<TextView>(R.id.hide_status)
        val alertDialog = AlertDialog.Builder(mainActivity).create()
        if (alertDialog.window != null) {
            alertDialog.window!!.setBackgroundDrawableResource(R.drawable.background_dialogbox)
            alertDialog.setView(dialogView)
            alertDialog.setCancelable(false)
            alertDialog.setTitle("Hiding.. Please wait")
            alertDialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                "Do in background"
            ) { dialog, _ -> dialog.dismiss() }
            alertDialog.show()
        }

        FileHiderAsyncTask(files, filesRoot, object : FileHiderAsyncTask.OnProgressUpdateListener {
            override fun onProgressUpdate(p: Int) {
                //Do update
                progress.text = "$p%"
                progressBar.progress = p
            }

            override fun setFileName(name: String) {
                fileName.text = name
            }

            override fun workDone() {
                Toast.makeText(mainActivity, "File(s) encrypted.", Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()
            }

            override fun onSpaceNotAvailable() {
                Toast.makeText(mainActivity, "No enough space..", Toast.LENGTH_SHORT).show()
            }

            override fun setStatus(s: String) {
                status.text = s
            }
        }).execute()


    }

    class FileHiderAsyncTask(
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
                        l.setStatus("Encrypting " + finalI + " of " + files.size)
                    }
                    try {
                        val fileInputStream = FileInputStream(f)
                        val out = File(filesRoot, f.name + ".xpo")
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
                                b[j] = (b[j] + 10).toByte()
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

    companion object {

        var pathToHideFiles = File(Constants.XPORTAL_ROOT, ".xpo")
        var filesRoot = File(pathToHideFiles, ".files")
        var rootUnhideFiles = File(Constants.XPORTAL_ROOT, "Unhide")
        //    public static final File VIDEO = new File(filesRoot, ".video");
        //    public static final File MUSIC = new File(filesRoot, ".music");
        //    public static final File IMAGE = new File(filesRoot, ".image");
        //    public static final File FILES = new File(filesRoot, ".files");
        private var hider: Hider? = null

        fun getHider(mainActivity: MainActivity): Hider {
            if (hider == null) {
                hider = Hider(mainActivity)
            }
            return hider as Hider
        }

        fun decrypt(outputFile: File, inputFile: File, context: Context) {
            try {
                val fileInputStream = FileInputStream(inputFile)
                val fileOutputStream = FileOutputStream(outputFile)
                var len: Int
                if (inputFile.freeSpace > inputFile.length()) {

                    val b = ByteArray(8192)

                    while (true) {
                        len = fileInputStream.read(b)
                        if (len < 0)
                            break
                        for (i in 0 until len) {
                            b[i] = (b[i] - 10).toByte()
                        }
                        fileOutputStream.write(b, 0, len)
                    }
                    fileOutputStream.flush()
                    fileOutputStream.close()

                    fileInputStream.close()
                } else
                    MainActivity.handler.post {
                        Toast.makeText(
                            context,
                            "No enough space",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        fun isSpaceAvailable(f: File): Boolean {
            return if (!f.exists()) {
                true
            } else f.freeSpace > f.length()
            //        long len = 0;
            //        for (File file : f){
            //            len += file.length();
            //        }
            //        File file = f.get(0);
        }
    }
}