package com.example.mayur.byteshare.connection.hotspot

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Environment
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import com.example.mayur.byteshare.Constants
import com.example.mayur.byteshare.MainActivity
import com.example.mayur.byteshare.connection.FileInfo
import com.example.mayur.byteshare.connection.logger.Logger
import com.example.mayur.byteshare.fragments.history.*
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import java.util.concurrent.ExecutionException


/**
 * Class that holds Transmission at Hotspot side
 */
class TransferHotspot private constructor(private val mainActivity: MainActivity) {
    private val senderList: List<HistoryInfo>
    private var senderTimer: Timer? = null
    private var receiverTimer: Timer? = null
    private val fileInfoQueue = LinkedList<FileInfo>()
    private val xPortalRoot: File
    private val senderHistoryInfoList: MutableList<HistoryInfo>
    private val historyAdapterSender: HistoryAdapterSender
    private var serverSocket: ServerSocket? = null
    private var socket: Socket? = null
    private var receiverHistoryInfoList: MutableList<HistoryInfo>
    private var historyAdapterReceiver: HistoryAdapterReceiver

    init {
        Logger.log("Returning Sender Object", this@TransferHotspot)
        senderList = ArrayList()
        historyAdapterSender = HistorySendFragment.historyAdapterSender
        senderHistoryInfoList = historyAdapterSender.historyInfoList

        historyAdapterReceiver = HistoryReceiveFragment.historyAdapterReceiver
        receiverHistoryInfoList = historyAdapterReceiver.historyInfoList
        historyAdapterReceiver = HistoryReceiveFragment.historyAdapterReceiver
        receiverHistoryInfoList = historyAdapterReceiver.historyInfoList
        xPortalRoot = File(Environment.getExternalStorageDirectory().absolutePath + "/XPortal")
        if (xPortalRoot.exists() && !xPortalRoot.isDirectory)
            xPortalRoot.delete()
        if (!xPortalRoot.exists())
            xPortalRoot.mkdir()


        Thread(Runnable {
            try {
                serverSocket = ServerSocket(
                    Constants.TransferConstants.PORT,
                    200,
                    InetAddress.getByName("192.168.43.1")
                )
                Logger.log("Server started ", this@TransferHotspot)
                val socket = serverSocket!!.accept()
                Logger.log("Client accepted ", this@TransferHotspot)
                isStarted = true

                senderTimer = Timer(true)
                senderTimer?.scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        if (socket != null && socket.isConnected) {
                            val outputStream: OutputStream
                            val dataOutputStream: DataOutputStream
                            try {
                                outputStream = socket.getOutputStream()
                                dataOutputStream = DataOutputStream(socket.getOutputStream())
                            } catch (e: IOException) {
                                mTerminateConnection()
                                e.printStackTrace()
                                return
                            }

                            if (fileInfoQueue.size > 0) {
                                val fileInfo = fileInfoQueue.remove()
                                try {
                                    dataOutputStream.writeBoolean(true)            //Some files are ready to send
                                    val info = fileInfo.jsonObject.toString()
                                    Logger.log(info, this@TransferHotspot)
                                    outputStream.write(info.length)                  //Length of JSON
                                    outputStream.write(info.toByteArray())                //Actual JSON

                                    val fileInputStream = FileInputStream(fileInfo.file)


                                    try {
                                        val senderTask =
                                            SenderTask(fileInfo, fileInputStream, outputStream)

                                        MainActivity.handler.post {
                                            senderTask.execute()
                                            Logger.log(
                                                "Executing sender task...",
                                                this@TransferHotspot
                                            )
                                        }
                                        senderTask.get()
                                        Logger.log("SenderTask completed...", this@TransferHotspot)
                                    } catch (e: ExecutionException) {
                                        e.printStackTrace()
                                    } catch (e: InterruptedException) {
                                        e.printStackTrace()
                                    }

                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }

                            } else {
                                try {
                                    dataOutputStream.writeBoolean(false)
                                } catch (e: IOException) {
                                    mTerminateConnection()
                                    e.printStackTrace()
                                }

                            }
                        }
                    }
                }, 0, 1000)


                receiverTimer = Timer(true)
                receiverTimer?.scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        try {
                            if (socket != null && socket.isConnected && MainActivity.isConnected) {

                                val inputStream: InputStream
                                val dataInputStream: DataInputStream

                                try {
                                    inputStream = socket.getInputStream()
                                    dataInputStream = DataInputStream(inputStream)
                                } catch (e: IOException) {
                                    mTerminateConnection()
                                    e.printStackTrace()
                                    return
                                }

                                val isFilesAvailable: Boolean
                                try {
                                    isFilesAvailable = dataInputStream.readBoolean()
                                } catch (e: IOException) {
                                    mTerminateConnection()
                                    e.printStackTrace()
                                    return
                                }

                                if (isFilesAvailable) {
                                    //                                        String info = dataInputStream.readUTF();        //Receiving JSON
                                    val lenOfJson = inputStream.read()
                                    val json = ByteArray(lenOfJson)
                                    inputStream.read(json, 0, lenOfJson)
                                    val info = String(json)
                                    Logger.log(info, this@TransferHotspot)
                                    val jsonObject = JSONObject(info)

                                    val fileName =
                                        jsonObject.getString(Constants.TransferConstants.KEY_FILE_NAME)
                                    val fileSize =
                                        jsonObject.getLong(Constants.TransferConstants.KEY_FILE_SIZE)
                                    val isHiddenFile =
                                        jsonObject.getBoolean(Constants.TransferConstants.KEY_HIDDEN_FILE)


                                    try {
                                        val receiverTask = ReceiverTask(
                                            fileName,
                                            fileSize,
                                            inputStream,
                                            isHiddenFile
                                        )

                                        MainActivity.handler.post { receiverTask.execute() }

                                        receiverTask.get()
                                    } catch (e: ExecutionException) {
                                        e.printStackTrace()
                                    } catch (e: InterruptedException) {
                                        e.printStackTrace()
                                    }

                                }

                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }

                    }
                }, 0, 1000)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }).start()
    }

    private fun mTerminateConnection() {
        terminateConnection()
        mainActivity.connectionTerminated()
        val hotspotManager = HotspotManager.getHotspotManager(mainActivity)
        hotspotManager.terminateConnection()
        hotspotManager.communication.terminateConnection()
    }

    fun sendFiles(fileInfo: List<FileInfo>) {
        fileInfoQueue.addAll(fileInfo)
    }

    fun terminateConnection() {
        Thread(Runnable {
            Logger.log("Terminating Connection", this@TransferHotspot)
            receiverTimer?.cancel()
            receiverTimer = null
            if (senderTimer != null) {
                senderTimer?.cancel()
                senderTimer = null
            }


            if (socket != null) {
                try {
                    socket!!.close()
                    Logger.log("Closing Socket in terminateConnection()", this@TransferHotspot)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }

            if (serverSocket != null) {
                try {
                    serverSocket!!.close()
                    Logger.log(
                        "Closing ServerSocket in terminateConnection()",
                        this@TransferHotspot
                    )
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }).start()

    }

    @SuppressLint("StaticFieldLeak")
    private inner class SenderTask internal constructor(
            internal var fileInfo: FileInfo,
            internal var fileInputStream: FileInputStream,
            internal var outputStream: OutputStream
    ) : AsyncTask<Void, Void, HistoryInfo>() {
        internal lateinit var historyInfo: HistoryInfo

        override fun onPreExecute() {
            historyInfo = HistoryInfo(
                0,
                fileInfo.fileName,
                fileInfo.file.length()
            ).setDate(System.currentTimeMillis())
            senderHistoryInfoList.add(0, historyInfo)
            historyAdapterSender.recyclerView.recycledViewPool.clear()
            historyAdapterSender.notifyItemInserted(0)
            super.onPreExecute()
        }

        override fun onPostExecute(historyInfo: HistoryInfo) {
            updateProgress()
            super.onPostExecute(historyInfo)
        }

        override fun onProgressUpdate(vararg values: Void) {
            updateProgress()
            super.onProgressUpdate(*values)
        }

        private fun updateProgress() {
            historyAdapterSender.recyclerView.recycledViewPool.clear()
            historyAdapterSender.notifyItemChanged(0)
        }

        override fun doInBackground(vararg voids: Void): HistoryInfo? {

            val fileName = fileInfo.fileName
            val total = fileInfo.file.length()
            var len: Int
            Logger.log("Sending file $fileName Size $total", this@TransferHotspot)

            var written: Long = 0
            val b = ByteArray(8192)
            var prevTime = System.currentTimeMillis()

            try {
                while (true) {
                    len = fileInputStream.read(b)
                    if (len <= 0)
                        break
                    outputStream.write(b, 0, len)
                    written += len.toLong()
                    val per = (written * 100.toLong() / total).toInt()

                    val now = System.currentTimeMillis()

                    if (now - prevTime >= 750) {
                        prevTime = now
                        historyInfo.progress = per
                        historyInfo.sentSize = written
                        publishProgress()
                    }

                    if (per >= 100) {
                        historyInfo.progress = per
                        historyInfo.sentSize = written
                        publishProgress()
                        historyAdapterSender.updateDatabase(historyInfo)
                    }

                }

                Logger.log("File sent $fileName size $written", this@TransferHotspot)
                fileInputStream.close()

                try {
                    Thread.sleep(600)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            } catch (e: IOException) {
                e.printStackTrace()
            }


            return null
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class ReceiverTask internal constructor(
        internal var fileName: String,
        internal var fileSize: Long,
        internal var inputStream: InputStream,
        isHiddenFile: Boolean
    ) : AsyncTask<Void, Void, HistoryInfo>() {
        internal lateinit var historyInfo: HistoryInfo
        internal var total: Long = 0
        private var isHiddenFile = false

        init {
            total = fileSize
            this.isHiddenFile = isHiddenFile
        }

        override fun onPreExecute() {

            historyInfo = HistoryInfo(0, fileName, total).setDate(System.currentTimeMillis())
            receiverHistoryInfoList.add(0, historyInfo)
            historyAdapterReceiver.recyclerView.recycledViewPool.clear()
            historyAdapterReceiver.itemInserted(0)

            super.onPreExecute()
        }

        override fun onPostExecute(historyInfo: HistoryInfo) {
            updateProgress()
            super.onPostExecute(historyInfo)
        }

        override fun onProgressUpdate(vararg values: Void) {
            updateProgress()
            super.onProgressUpdate(*values)
        }

        private fun updateProgress() {
            historyAdapterReceiver.recyclerView.recycledViewPool.clear()
            historyAdapterReceiver.notifyItemChanged(0)
        }


        @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        override fun doInBackground(vararg voids: Void): HistoryInfo? {

            if (ActivityCompat.checkSelfPermission(
                    mainActivity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    mainActivity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    8989
                )
                return null
            }
            try {
                val file = File(xPortalRoot, fileName)
                Logger.log("Receiving file $fileName Size $fileSize", this@TransferHotspot)
                if (file.exists()) {
                    file.renameTo(File(fileName + System.currentTimeMillis()))
                    file.delete()
                }
                file.createNewFile()

                val b = ByteArray(8192)
                var written: Long = 0
                var len: Int

                val fileOutputStream = FileOutputStream(file)
                try {
                    var prevTime = System.currentTimeMillis()

                    while (true) {
                        len = inputStream.read(b, 0, Math.min(b.size.toLong(), fileSize).toInt())
                        if (fileSize > 0 && len != -1){
                            if (isHiddenFile) {
                                for (i in 0 until len) {
                                    b[i] = (b[i] - 10).toByte()
                                }
                            }
                            fileOutputStream.write(b, 0, len)
                            fileSize -= len.toLong()
                            written += len.toLong()
                            val per = (written * 100.toLong() / total).toInt()

                            val now = System.currentTimeMillis()
                            if (now - prevTime >= 750) {
                                prevTime = now
                                historyInfo.progress = per
                                historyInfo.sentSize = written
                                publishProgress()
                            }

                            if (per == 100) {
                                historyInfo.progress = per
                                historyInfo.sentSize = written
                                publishProgress()
                                historyAdapterReceiver.updateDatabase(historyInfo)
                            }
                        }
                        else break
                    }
                    Logger.log("File Received $fileName Size $written", this@TransferHotspot)
                    try {
                        Thread.sleep(600)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                }

            } catch (e: IOException) {
                e.printStackTrace()
            }

            return null
        }
    }

    companion object {

        private var isStarted = false
        private var transferHotspot: TransferHotspot? = null

        fun getSender(mainActivity: MainActivity, isNew: Boolean): TransferHotspot? {
            if (isNew) {
                if (transferHotspot != null) {
                    transferHotspot!!.terminateConnection()
                    transferHotspot = null
                    transferHotspot = TransferHotspot(mainActivity)
                    return transferHotspot
                }

                transferHotspot = TransferHotspot(mainActivity)
                return transferHotspot
            }
            return transferHotspot
        }
    }
}