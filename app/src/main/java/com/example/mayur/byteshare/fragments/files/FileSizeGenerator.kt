package com.example.mayur.byteshare.fragments.files

import android.os.AsyncTask

import java.io.File

class FileSizeGenerator(
    private val folderList: List<File>,
    private val onProgressUpdateListener: OnProgressUpdateListener
) : AsyncTask<Void, Long, Long>() {
    private var total: Long = 0

    override fun onPreExecute() {
        super.onPreExecute()
        total = 0
    }

    override fun onPostExecute(s: Long?) {
        super.onPostExecute(s)
        if (!isCancelled)
            onProgressUpdateListener.onProgressUpdate(s!!)
    }

    override fun doInBackground(vararg voids: Void): Long? {
        var len: Long = 0

        for (f in folderList) {
            len += getFolderSize(f)
        }
        return len
    }

    private fun getFolderSize(dir: File): Long {
        var len: Long = 0
        if (!isCancelled) {
            for (f in dir.listFiles()) {
                if (!isCancelled) {
                    len += if (!f.isDirectory) {
                        f.length()
                    } else
                        getFolderSize(f)
                }
            }
        }
        return len
    }

    interface OnProgressUpdateListener {
        fun onProgressUpdate(size: Long)
    }
}
