package com.example.mayur.byteshare.connection.wifimanager

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.AsyncTask
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.mayur.byteshare.Constants
import com.example.mayur.byteshare.MainActivity
import com.example.mayur.byteshare.connection.intent.Intents
import com.example.mayur.byteshare.connection.wifi.MyWifiManager
import com.example.mayur.byteshare.R
import com.example.mayur.byteshare.connection.FileInfo
import com.example.mayur.byteshare.connection.logger.Logger
import com.example.mayur.byteshare.fragments.history.*
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import java.util.concurrent.ExecutionException

object SocketContainer {
    var socket: Socket? = null
}

/**
 * Class that holds Transmission at Wifi side
 */
class TransferWifi(val mainActivity: MainActivity): Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    private var senderTimer: Timer? = null
    private val intentFilter = IntentFilter()
    private var receiverTimer: Timer? = null
    private val fileInfoQueue = LinkedList<FileInfo>()
    private var xPortalRoot: File? = null

    private val historyAdapterSender: HistoryAdapterSender?
    private val historyAdapterReceiver: HistoryAdapterReceiver?
    private var socket: Socket? = null
    private var connectivityManager: ConnectivityManager? = null
    private val senderHistoryInfoList: MutableList<HistoryInfo>
    private val senderList: List<HistoryInfo>
    private val receiverHistoryInfoList: MutableList<HistoryInfo>

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
            channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("my_service", "My Background Service")
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                ""
            }

        val notificationCompat = NotificationCompat.Builder(this, channelId).apply {
            setContentTitle("ByteShare")
            setContentText("Sharing...")
            setSubText("SubText")
            setSmallIcon(R.drawable.ic_xportal)

            setContentIntent(PendingIntent.getActivity(this@TransferWifi, 1001, Intent(this@TransferWifi, MainActivity::class.java), PendingIntent.FLAG_NO_CREATE))
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }
        startForeground(8154, notificationCompat.build())
        registerReceiver()
        initiate()
        return START_NOT_STICKY
    }

    private fun registerReceiver() {
        if (!isReceiverRegistered)
            registerReceiver(receiver, intentFilter)
        isReceiverRegistered = true
    }

    private fun unregisterReceiver() {
        if (isReceiverRegistered)
            unregisterReceiver(receiver)
        isReceiverRegistered = false
    }

    private fun initiate() {
        Thread(Runnable {
            socket = Socket()
            xPortalRoot = File(Environment.getExternalStorageDirectory().absolutePath + "/XPortal")

            if (!xPortalRoot!!.exists())
                xPortalRoot!!.mkdir()
            else if (xPortalRoot!!.exists() && !xPortalRoot!!.isDirectory) {
                xPortalRoot!!.delete()
                xPortalRoot!!.mkdir()
            }

            connectivityManager =
                    getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            assert(connectivityManager != null)


            val builder = NetworkRequest.Builder()
            builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            connectivityManager!!.requestNetwork(
                builder.build(),
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network?) {
                        super.onAvailable(network)

                        if (network != null) {
                            try {
                                network.bindSocket(socket)
                                socket!!.connect(
                                    InetSocketAddress(
                                            MyWifiManager.ip,
                                        Constants.TransferConstants.PORT
                                    )
                                )
                                SocketContainer.socket = socket as Socket

                                senderTimer = Timer(true)
                                senderTimer!!.scheduleAtFixedRate(object : TimerTask() {
                                    override fun run() {
                                        if (socket != null && socket!!.isConnected) {
                                            val outputStream: OutputStream
                                            val dataOutputStream: DataOutputStream
                                            try {
                                                outputStream = socket!!.getOutputStream()
                                                dataOutputStream =
                                                        DataOutputStream(socket!!.getOutputStream())
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
                                                    Logger.log(info, this@TransferWifi)
                                                    outputStream.write(info.length)                  //Length of JSON
                                                    outputStream.write(info.toByteArray())                //Actual JSON
                                                    //                                        dataOutputStream.writeUTF(info);                   //Send JSON

                                                    val fileInputStream =
                                                        FileInputStream(fileInfo.file)

                                                    try {
                                                        val senderTask = SenderTask(
                                                            fileInfo,
                                                            fileInputStream,
                                                            outputStream
                                                        )

                                                        MainActivity.handler.post { senderTask.execute() }
                                                        senderTask.get()
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
                                receiverTimer!!.scheduleAtFixedRate(object : TimerTask() {
                                    override fun run() {
                                        try {
                                            if (socket != null && socket!!.isConnected && MainActivity.isConnected) {

                                                val inputStream: InputStream
                                                val dataInputStream: DataInputStream

                                                try {
                                                    inputStream = socket!!.getInputStream()
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
                                                    Logger.log(info, this@TransferWifi)
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


                        }
                    }
                })
        }).start()
    }

    init {
        intentFilter.addAction(Intents.ACTION_SEND_FILES_WIFI)
        intentFilter.addAction(Intents.ACTION_STOP_WIFI_SERVICE)
        intentFilter.addAction(Intents.ACTION_TRANSFER_WIFI_TERMINATE_CONNECTION)
        senderList = ArrayList()
        historyAdapterSender = HistorySendFragment.historyAdapterSender
        historyAdapterReceiver = HistoryReceiveFragment.historyAdapterReceiver
        receiverHistoryInfoList = historyAdapterReceiver.historyInfoList
        senderHistoryInfoList = historyAdapterSender.historyInfoList
        Logger.log("Returning Sender", this@TransferWifi)
    }

    private fun mTerminateConnection() {
        Logger.log("mTerminateConnection() ", this@TransferWifi)
        terminateConnection()
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
            .sendBroadcast(Intent(Intents.ACTION_MAINACTIVITY_CONNECTION_TERMINATED))
//        mainActivity.connectionTerminated()
        val myWifiManager = MyWifiManager.getMyWifiManager()
        myWifiManager?.let {
            myWifiManager.terminateConnection()
            myWifiManager.communication.terminateConnection()
        }
    }

    private var isReceiverRegistered = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (intent.action == Intents.ACTION_STOP_WIFI_SERVICE) {
                    mTerminateConnection()
                    stopForeground(true)
                    stopSelf()
                }

                if (intent.action == Intents.ACTION_SEND_FILES_WIFI) {
                    val extras = intent.extras
                    val files = extras?.getStringArrayList(Constants.EXTRA_FILES)
                    files?.let {
                        if (files.isNotEmpty()){
                            val fileInfos = ArrayList<FileInfo>(files.size)
                            for (i in files){
                                val f = File(i)
                                fileInfos.add(FileInfo(f, f.name, false))
                            }
                            sendFiles(fileInfos)
                        }
                    }
                }

                if (it.action == Intents.ACTION_TRANSFER_WIFI_TERMINATE_CONNECTION) {
                    terminateConnection()
                }
            }
        }
    }

    fun terminateConnection() {
        Logger.log("Terminating Connection", this@TransferWifi)
        unregisterReceiver()
        if (receiverTimer != null)
            receiverTimer!!.cancel()
        if (senderTimer != null)
            senderTimer!!.cancel()

        if (socket != null) {
            try {
                socket!!.close()
                Logger.log("Closing socket terminateConnection()", this@TransferWifi)
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }


    fun sendFiles(fileInfo: List<FileInfo>) {

        fileInfoQueue.addAll(fileInfo)

    }

    @SuppressLint("StaticFieldLeak")
    private inner class SenderTask internal constructor(
            internal var fileInfo: FileInfo,
            internal var fileInputStream: FileInputStream,
            internal var outputStream: OutputStream
    ) : AsyncTask<Void?, Void?, HistoryInfo?>() {
        internal lateinit var historyInfo: HistoryInfo

        override fun onPreExecute() {
            historyInfo = HistoryInfo(0, fileInfo.fileName, fileInfo.file.length())
            senderHistoryInfoList.add(0, historyInfo)
            historyAdapterSender!!.recyclerView.recycledViewPool.clear()
            historyAdapterSender.notifyItemInserted(0)
            super.onPreExecute()
        }

        override fun onPostExecute(historyInfo: HistoryInfo?) {
            updateProgress()
            super.onPostExecute(historyInfo)
        }

        override fun onProgressUpdate(vararg values: Void?) {
            updateProgress()
            super.onProgressUpdate(*values)
        }

        private fun updateProgress() {
            historyAdapterSender!!.recyclerView.recycledViewPool.clear()
            historyAdapterSender.notifyItemChanged(0)
        }

        override fun doInBackground(vararg voids: Void?): HistoryInfo? {
            val fileName = fileInfo.fileName
            val total = fileInfo.file.length()
            var len: Int
            Logger.log("Sending file $fileName Size $total", this@TransferWifi)

            var written: Long = 0
            val b = ByteArray(8192)
            var prevTime = System.currentTimeMillis()


            try {
                while (true) {
                    len = fileInputStream.read(b)
                    if (len <= 0) break
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
                        historyAdapterSender!!.updateDatabase(historyInfo)
                    }
                }

                Logger.log("File sent $fileName size $written", this@TransferWifi)
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
    private inner class ReceiverTask(
        internal var fileName: String,
        internal var fileSize: Long,
        internal var inputStream: InputStream,
        private val isHiddenFile: Boolean
    ) : AsyncTask<Void?, Void?, HistoryInfo?>() {
        internal lateinit var historyInfo: HistoryInfo
        internal var total: Long = 0

        init {
            total = fileSize
        }

        override fun onPreExecute() {
            historyInfo = HistoryInfo(0, fileName, total)
            receiverHistoryInfoList.add(0, historyInfo)
            historyAdapterReceiver!!.recyclerView.recycledViewPool.clear()
            historyAdapterReceiver.notifyItemInserted(0)
            super.onPreExecute()
        }

        override fun onPostExecute(historyInfo: HistoryInfo?) {
            updateProgress()
            super.onPostExecute(historyInfo)
        }

        override fun onProgressUpdate(vararg values: Void?) {
            updateProgress()
            super.onProgressUpdate(*values)
        }

        private fun updateProgress() {
            historyAdapterReceiver!!.recyclerView.recycledViewPool.clear()
            historyAdapterReceiver.notifyItemChanged(0)
        }

        @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        override fun doInBackground(vararg voids: Void?): HistoryInfo? {

            try {
                val file = File(xPortalRoot, fileName)
                Logger.log("Receiving file $fileName Size $fileSize", this@TransferWifi)
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
                        if (fileSize > 0 && len != -1) {
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
                                historyAdapterReceiver!!.updateDatabase(historyInfo)
                            }
                        } else break
                    }
                    Logger.log("File Received $fileName Size $written", this@TransferWifi)

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

            return if (ActivityCompat.checkSelfPermission(
                    this@TransferWifi,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                null
            } else null
        }
    }

    companion object {

        private var transferWifi: TransferWifi? = null

        fun getSender(
            mainActivity: MainActivity
        ): TransferWifi {
            if (transferWifi == null) {
                transferWifi = TransferWifi(mainActivity)
            }
            return transferWifi as TransferWifi
        }
    }
}