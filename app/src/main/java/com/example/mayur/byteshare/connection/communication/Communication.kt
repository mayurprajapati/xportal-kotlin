package com.example.mayur.xportal.connection.communication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.widget.Toast

import com.example.mayur.xportal.Constants
import com.example.mayur.xportal.MainActivity
import com.example.mayur.xportal.connection.intent.Intents
import com.example.mayur.xportal.connection.logger.Logger

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class Communication(type: Int, private val mainActivity: MainActivity) : Thread() {

    private var mOnCommunicationSuccessful: OnCommunicationSuccessful? = null
    private var mOnCommunicationInterrupted: OnCommunicationInterrupted? = null
    private var deviceName = ""
    private var type = -1
    private var isReceiverRegistered = false
    private var serverSocket: ServerSocket? = null
    private var socket: Socket? = null
    private var mOnConnectionTerminated: OnConnectionTerminated? = null
    private var dataOutputStream: DataOutputStream? = null
    private var dataInputStream: DataInputStream? = null

    init {
        this.type = type
    }

    fun makeCommunication() {

        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                //                try {
                //                    serverSocket.close();
                //                } catch (IOException e) {
                //                    e.printStackTrace();
                //                }
            }
        }
        when (type) {
            Constants.HotspotConstants.TYPE_HOTSPOT -> {
                val serverThread = object : Thread() {
                    override fun run() {
                        try {
                            serverSocket =
                                    ServerSocket(Constants.InitialCommunication.COMMUNICATION_PORT)
                            serverSocket!!.reuseAddress = true
                            serverSocket!!.soTimeout = 0
                            Logger.log("Server started", this@Communication)
                            mainActivity.registerReceiver(
                                broadcastReceiver,
                                IntentFilter(Intents.HOTSPOT_STOPPED)
                            )
                            isReceiverRegistered = true
                            socket = serverSocket!!.accept()
                            Logger.log("Client found", this@Communication)
                            if (socket != null && socket!!.isConnected) {
                                val sent =
                                    generateComObject(Constants.HotspotConstants.TYPE_HOTSPOT)
                                        ?: throw NullPointerException()
                                dataOutputStream = DataOutputStream(socket!!.getOutputStream())
                                dataInputStream = DataInputStream(socket!!.getInputStream())
                                dataOutputStream!!.writeUTF(sent)
                                Logger.log("KEY Written $sent", this@Communication)
                                val received = dataInputStream!!.readUTF()
                                Logger.log("KEY Received $received", this@Communication)
                                if (decodeString(
                                        Constants.HotspotConstants.TYPE_HOTSPOT,
                                        received
                                    )
                                ) {
                                    //send true
                                    dataOutputStream!!.writeBoolean(true)
                                    val result = dataInputStream!!.readBoolean()

                                    if (result) {
                                        //communication successful
                                        if (mOnCommunicationSuccessful != null)
                                            mOnCommunicationSuccessful!!.onCommunicationSuccessful(
                                                deviceName
                                            )


                                        Logger.log("Communication Successful", this@Communication)

                                        connectionTerminationMessageReader()

                                        if (isReceiverRegistered) {
                                            mainActivity.unregisterReceiver(broadcastReceiver)
                                            isReceiverRegistered = false
                                        }
                                    } else {
                                        Logger.log("Communication Unsuccessful", this@Communication)
                                        if (mOnCommunicationInterrupted != null)
                                            mOnCommunicationInterrupted!!.onCommunicationInterrupted()
                                        dataInputStream!!.close()
                                        dataOutputStream!!.flush()
                                        dataOutputStream!!.close()
                                        socket!!.close()
                                        serverSocket!!.close()
                                        if (isReceiverRegistered) {
                                            mainActivity.unregisterReceiver(broadcastReceiver)
                                            isReceiverRegistered = false
                                        }

                                    }
                                } else {
                                    //send false
                                    dataOutputStream!!.writeBoolean(false)
                                    dataInputStream!!.close()
                                    dataOutputStream!!.flush()
                                    dataOutputStream!!.close()
                                    socket!!.close()
                                    serverSocket!!.close()
                                    if (isReceiverRegistered) {
                                        mainActivity.unregisterReceiver(broadcastReceiver)
                                        isReceiverRegistered = false
                                    }
                                }
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                            if (isReceiverRegistered) {
                                mainActivity.unregisterReceiver(broadcastReceiver)
                                isReceiverRegistered = false
                            }
                        } catch (e: NullPointerException) {
                            e.printStackTrace()
                            if (isReceiverRegistered) {
                                mainActivity.unregisterReceiver(broadcastReceiver)
                                isReceiverRegistered = false
                            }
                        }

                    }
                }
                serverThread.start()
            }


            Constants.WifiConstants.TYPE_WIFI -> object : Thread() {
                override fun run() {
                    socket = Socket()
                    val connectivityManager =
                        mainActivity.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    //                        Network myNetwork = null;

                    val builder = NetworkRequest.Builder()
                    builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    connectivityManager.requestNetwork(
                        builder.build(),
                        object : ConnectivityManager.NetworkCallback() {
                            override fun onAvailable(network: Network) {
                                super.onAvailable(network)
                                try {
                                    network.bindSocket(socket)
                                    Logger.log("Binding socket", this@Communication)
                                    val inetSocketAddress = InetSocketAddress(
                                        InetAddress.getByName("192.168.43.1"),
                                        Constants.InitialCommunication.COMMUNICATION_PORT
                                    )
                                    //                                socket = myNetwork.getSocketFactory().createSocket(InetAddress.getByName("192.168.43.1"), Constants.InitialCommunication.COMMUNICATION_PORT);
                                    MainActivity.handler.post {
                                        Toast.makeText(
                                            mainActivity,
                                            "Socket bounded",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    socket!!.connect(inetSocketAddress)
                                    dataInputStream = DataInputStream(socket!!.getInputStream())
                                    dataOutputStream = DataOutputStream(socket!!.getOutputStream())
                                    val received = dataInputStream!!.readUTF()
                                    val sent = generateComObject(Constants.WifiConstants.TYPE_WIFI)
                                        ?: throw NullPointerException()
                                    dataOutputStream!!.writeUTF(sent)
                                    //decode received
                                    val result = dataInputStream!!.readBoolean()
                                    if (result) {
                                        if (decodeString(
                                                Constants.WifiConstants.TYPE_WIFI,
                                                received
                                            )
                                        ) {
                                            //communication successful
                                            dataOutputStream!!.writeBoolean(true)
                                            mOnCommunicationSuccessful!!.onCommunicationSuccessful(
                                                deviceName
                                            )
                                            connectionTerminationMessageReader()
                                        } else {
                                            dataOutputStream!!.writeBoolean(false)
                                            if (mOnCommunicationInterrupted != null)
                                                mOnCommunicationInterrupted!!.onCommunicationInterrupted()
                                            dataOutputStream!!.flush()
                                            dataOutputStream!!.close()
                                            dataInputStream!!.close()
                                            socket!!.close()
                                        }
                                    } else {
                                        dataInputStream!!.close()
                                        dataOutputStream!!.flush()
                                        dataOutputStream!!.close()
                                        socket!!.close()
                                    }

                                    //get result and send result
                                } catch (e: IOException) {
                                    if (mOnCommunicationInterrupted != null)
                                        mOnCommunicationInterrupted!!.onCommunicationInterrupted()
                                    e.printStackTrace()
                                }

                            }

                        })


                    //                        for (Network network : connectivityManager.getAllNetworks()) {
                    //                            if (connectivityManager.getNetworkInfo(network).getType() == ConnectivityManager.TYPE_WIFI) {
                    //                                System.out.println("Network found...");
                    //
                    //                                myNetwork = network;
                    //                            }
                    //                        }
                    //
                    //                        if (myNetwork != null) {
                    //
                    //
                    //                        }
                }
            }.start()
        }
    }

    private fun generateComObject(type: Int): String? {
        when (type) {
            Constants.HotspotConstants.TYPE_HOTSPOT -> return Build.MODEL + "&&" +
                    Constants.HotspotConstants.KEY
            Constants.WifiConstants.TYPE_WIFI -> return Build.MODEL + "&&" +
                    Constants.WifiConstants.KEY
        }
        return null
    }

    private fun decodeString(type: Int, string: String): Boolean {
        when (type) {
            Constants.HotspotConstants.TYPE_HOTSPOT -> {

                if (string != "" && string.contains("&&")) {
                    val key =
                        string.split("&&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                    if (key == Constants.WifiConstants.KEY) {
                        deviceName =
                                string.split("&&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                        println("DECODE :- TRUE")
                        return true
                    }
                } else {
                    println("DECODE :- FALSE")
                    return false
                }
                if (string != "" && string.contains("&&")) {
                    val key =
                        string.split("&&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                    if (key == Constants.HotspotConstants.KEY) {
                        deviceName =
                                string.split("&&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                        return true
                    }
                } else {
                    return false
                }
            }
            Constants.WifiConstants.TYPE_WIFI -> if (string != "" && string.contains("&&")) {
                val key =
                    string.split("&&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                if (key == Constants.HotspotConstants.KEY) {
                    deviceName =
                            string.split("&&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                    return true
                }
            } else {
                return false
            }
        }
        return false
    }

    fun setOnCommunicationSuccessfulListener(mOnCommunicationSuccessful: OnCommunicationSuccessful) {
        this.mOnCommunicationSuccessful = mOnCommunicationSuccessful
    }

    fun setOnCommunicationInterruptedListener(mOnCommunicationInterrupted: OnCommunicationInterrupted) {
        this.mOnCommunicationInterrupted = mOnCommunicationInterrupted
    }

    fun setOnConnectionTerminatedListener(mOnConnectionTerminated: OnConnectionTerminated) {
        this.mOnConnectionTerminated = mOnConnectionTerminated
    }

    private fun connectionTerminationMessageReader() {
        Thread(Runnable {
            try {
                //true if connection terminated...
                if (dataInputStream!!.readBoolean()) {
                    //connection terminated...
                    socket!!.close()
                    if (serverSocket != null)
                        serverSocket!!.close()
                    if (mOnConnectionTerminated != null) {
                        mOnConnectionTerminated!!.onCommunicationTerminated()
                    }
                }
            } catch (ignored: IOException) {
            }
        }).start()
    }

    fun terminateConnection() {
        Thread(Runnable {
            try {
                if (socket != null)
                    dataOutputStream!!.writeBoolean(true)
            } catch (e: IOException) {

                e.printStackTrace()
            }

            if (socket != null) {
                try {
                    socket!!.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }

            if (serverSocket != null) {
                try {
                    serverSocket!!.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }).start()
    }
}