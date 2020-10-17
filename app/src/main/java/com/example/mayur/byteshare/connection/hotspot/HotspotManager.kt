package com.example.mayur.byteshare.connection.hotspot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.mayur.byteshare.Constants
import com.example.mayur.byteshare.MainActivity
import com.example.mayur.byteshare.connection.communication.Communication
import com.example.mayur.byteshare.connection.communication.OnCommunicationInterrupted
import com.example.mayur.byteshare.connection.communication.OnCommunicationSuccessful
import com.example.mayur.byteshare.connection.communication.OnConnectionTerminated
import com.example.mayur.byteshare.connection.location.LocationPermissionFragment
import com.example.mayur.byteshare.connection.logger.Logger
import com.example.mayur.byteshare.connection.wifi.MyWifiManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*

class HotspotManager private constructor(private val mainActivity: MainActivity) {
    private val qrCodeFragment: QRCodeFragment
    private val broadcastReceiver: BroadcastReceiver
    private val intentFilter: IntentFilter
    private var isReceiverRegistered = false
    lateinit var communication: Communication
    private val fragmentManager: FragmentManager
    private var wifiConfiguration: WifiConfiguration? = null
    private var localOnlyHotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null
    private val wifiManager: WifiManager
    private val oldHotspotController: OldHotspotController?
    private var old: WifiConfiguration? = null

    private val inetAddress: InetAddress?
        get() {

            val networkInterfaceEnumeration: Enumeration<NetworkInterface>?
            try {
                networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces()
            } catch (e: SocketException) {
                e.printStackTrace()
                return null
            }

            while (networkInterfaceEnumeration!!.hasMoreElements()) {
                val networkInterface = networkInterfaceEnumeration.nextElement()
                val inetAddressEnumeration = networkInterface.inetAddresses
                while (inetAddressEnumeration.hasMoreElements()) {
                    val inetAddress = inetAddressEnumeration.nextElement()
                    if (inetAddress.isSiteLocalAddress) {
                        Logger.log(
                            "Site Local Address found ${inetAddress.hostAddress}",
                            this@HotspotManager
                        )
                        return inetAddress
                    }
                }
            }
            return null
        }

    init {
        intentFilter = IntentFilter(WIFI_AP_STATE_CHANGED_ACTION)
        qrCodeFragment = QRCodeFragment()
        wifiManager =
                mainActivity.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        oldHotspotController = OldHotspotController.getApControl(wifiManager)

        fragmentManager = mainActivity.supportFragmentManager
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action!!

                when (action) {
                    WIFI_AP_STATE_CHANGED_ACTION -> {
                        Logger.log("WIFI_AP_STATE_CHANGED_ACTION", this@HotspotManager)
                        val state = intent.getIntExtra(EXTRA_WIFI_AP_STATE, -1)
                        when (state) {
                            WIFI_AP_STATE_DISABLED -> {
                                Logger.log("WIFI_AP_STATE_DISABLED", this@HotspotManager)
                                if (localOnlyHotspotReservation != null)
                                    localOnlyHotspotReservation!!.close()
                                localOnlyHotspotReservation = null
                                if (MainActivity.isConnected) {
                                    terminateConnection()
                                }
                            }
                            WIFI_AP_STATE_DISABLING -> Logger.log(
                                "WIFI_AP_STATE_DISABLING",
                                this@HotspotManager
                            )
                            WIFI_AP_STATE_ENABLED -> {
                                Logger.log("WIFI_AP_STATE_ENABLED", this@HotspotManager)
                                startServer()
                            }
                            WIFI_AP_STATE_ENABLING -> Logger.log(
                                "WIFI_AP_STATE_ENABLING",
                                this@HotspotManager
                            )
                            WIFI_AP_STATE_FAILED -> Logger.log(
                                "WIFI_AP_STATE_FAILED",
                                this@HotspotManager
                            )
                        }//                                mainActivity.sendBroadcast(new Intent(Intents.HOTSPOT_STOPPED));
                    }
                }
            }
        }
    }

    fun startHotspot() {

        //Request for location
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val androidLocationManager =
                mainActivity.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            val gpsEnabled =
                androidLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if (!gpsEnabled) {
                val locationPermissionFragment = LocationPermissionFragment()
                fragmentManager.beginTransaction()
                    .add(locationPermissionFragment, "locationpermission").commitAllowingStateLoss()
                return
            }
        }



        registerReceiver()
        //start hotspot
        fragmentManager.beginTransaction()
            .add(qrCodeFragment, "qrcodefragment")
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commitAllowingStateLoss()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            for (temp in wifiManager.configuredNetworks) {
                wifiManager.removeNetwork(temp.networkId)
            }
            if (localOnlyHotspotReservation != null)
                localOnlyHotspotReservation!!.close()
            localOnlyHotspotReservation = null
            wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                    Logger.log("onStarted() " + this.javaClass.simpleName, this@HotspotManager)
                    wifiConfiguration = reservation.wifiConfiguration
                    this@HotspotManager.localOnlyHotspotReservation = reservation
                    wifiConfigurationChanged()
                }

                override fun onFailed(reason: Int) {
                    Logger.log("onFailed() " + this.javaClass.simpleName, this@HotspotManager)
                    qrCodeFragment.dismissAllowingStateLoss()
                    val couldNotStartHotspotFragment = CouldNotStartHotspotFragment()
                    val bundle = Bundle()
                    bundle.putBoolean("wifi", false)
                    couldNotStartHotspotFragment.arguments = bundle
                    fragmentManager.beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .add(couldNotStartHotspotFragment, null)
                        .commitAllowingStateLoss()
                }

                override fun onStopped() {
                    Logger.log("onStopped() " + this.javaClass.simpleName, this@HotspotManager)
                    if (localOnlyHotspotReservation != null)
                        localOnlyHotspotReservation!!.close()
                    localOnlyHotspotReservation = null
                    if (qrCodeFragment.isVisible)
                        qrCodeFragment.dismissAllowingStateLoss()
                }
            }, null)
        } else {
            if (old == null)
                old = oldHotspotController!!.wifiApConfiguration
            val name = StringBuilder()
            val min = 65
            wifiConfiguration = WifiConfiguration()
            val random = Random()
            for (i in 0..11) {
                var r = random.nextInt(24)
                r += min
                val ch = r.toChar()
                name.append(ch)
            }

            wifiConfiguration!!.SSID = name.toString()

            oldHotspotController?.let {
                if (oldHotspotController.isWifiApEnabled())
                    oldHotspotController.setWifiApEnabled(null, false)
                oldHotspotController.setWifiApEnabled(wifiConfiguration, true)
            }
            wifiConfigurationChanged()
        }
    }

    private fun unregisterReceiver() {
        Logger.log("Unregistering Receiver", this@HotspotManager)
        if (isReceiverRegistered) {
            object : Thread() {
                override fun run() {
                    super.run()
                    mainActivity.unregisterReceiver(broadcastReceiver)
                }
            }.start()
        }
        isReceiverRegistered = false
    }

    private fun registerReceiver() {
        Logger.log("Registering Receiver", this@HotspotManager)
        if (!isReceiverRegistered)
            mainActivity.registerReceiver(broadcastReceiver, intentFilter)
        isReceiverRegistered = true
    }

    private fun startServer() {
        val slept = intArrayOf(0)
        object : Thread() {
            override fun run() {

                while (true) {
                    try {
                        Thread.sleep(1000)
                        slept[0] = slept[0] + 1000
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                    val inetAddress = inetAddress
                    if (inetAddress != null) {
                        Logger.log(
                            String.format(
                                "InetAddress %s HostName %s HostAddress %s",
                                inetAddress.toString(),
                                inetAddress.hostName,
                                inetAddress.hostAddress
                            ), this@HotspotManager
                        )
                        communication =
                                Communication(Constants.HotspotConstants.TYPE_HOTSPOT, mainActivity)
                        communication.setOnCommunicationInterruptedListener(object :
                                OnCommunicationInterrupted {
                            override fun onCommunicationInterrupted() {
                            }
                        })

                        communication.setOnCommunicationSuccessfulListener(object :
                                OnCommunicationSuccessful {
                            override fun onCommunicationSuccessful(deviceName: String) {
                                MainActivity.handler.post {
                                    Logger.log("Communication Successful", this@HotspotManager)
                                    mainActivity.connectionSuccess(deviceName)
                                    qrCodeFragment.dismissAllowingStateLoss()
                                }
                                TransferHotspot.getSender(mainActivity, true)
                            }
                        })

                        communication.setOnConnectionTerminatedListener(object :
                                OnConnectionTerminated {
                            override fun onCommunicationTerminated() {
                                terminateConnection()
                                val sender = TransferHotspot.getSender(mainActivity, false)
                                sender?.terminateConnection()
                                MainActivity.handler.post {
                                    mainActivity.connectionTerminated()
                                    Logger.log("Connection Terminated by User", this@HotspotManager)
                                }
                            }
                        })

                        Logger.log("Making Communication", this@HotspotManager)
                        communication.makeCommunication()
                        slept[0] = 0
                        break
                    } else {
                        if (slept[0] >= 10000) {
                            break
                        }
                    }
                }
            }

        }.start()
    }

    fun stopHotspot() {
        qrCodeFragment.dismissAllowingStateLoss()
        wifiConfiguration = null
        terminateConnection()
    }

    private fun wifiConfigurationChanged() {
        val name = wifiConfiguration!!.SSID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val key = wifiConfiguration!!.preSharedKey

            val barcodeEncoder = BarcodeEncoder()
            try {
                qrCodeFragment.hotspotStarted(
                    barcodeEncoder.encodeBitmap(
                        "$name&&$key",
                        BarcodeFormat.QR_CODE,
                        800,
                        750
                    ), this
                )
            } catch (e: WriterException) {
                e.printStackTrace()
            }

        } else {
            val password = "NoPasswordMayurPrajapati"
            val timer = Timer(true)

            timer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    oldHotspotController?.let {
                        if (oldHotspotController.isWifiApEnabled()) {
                            val barcodeEncoder = BarcodeEncoder()
                            MainActivity.handler.post {
                                try {
                                    qrCodeFragment.hotspotStarted(
                                        barcodeEncoder.encodeBitmap(
                                            "$name&&$password",
                                            BarcodeFormat.QR_CODE,
                                            800,
                                            750
                                        ), this@HotspotManager
                                    )
                                } catch (e: WriterException) {
                                    e.printStackTrace()
                                }
                            }
                            timer.cancel()
                        }
                    }
                }
            }, 3000, 1000)
        }
    }


    fun terminateConnection() {
        unregisterReceiver()
        Logger.log("Terminating Connection", this@HotspotManager)
        Thread(Runnable {
            //                try {
            //                    Thread.sleep(2000);
            //                } catch (InterruptedException e) {
            //                    e.printStackTrace();
            //                }
            closeHotspot()
        }).start()
    }

    private fun closeHotspot() {
        Logger.log("Closing Hotspot", this@HotspotManager)
        communication.terminateConnection()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (localOnlyHotspotReservation != null)
                localOnlyHotspotReservation!!.close()
            localOnlyHotspotReservation = null
            Logger.log("Closing LocalOnlyHotspotReservation", this@HotspotManager)
        } else {
            old?.let {
                oldHotspotController?.let {
                    if (oldHotspotController.isWifiApEnabled()) {
                        oldHotspotController.setWifiApEnabled(null, false)
                        oldHotspotController.setWifiApEnabled(old, true)
                        oldHotspotController.setWifiApEnabled(null, false)
                    }
                }
            }
        }
        unregisterReceiver()
    }

    companion object {

        private const val EXTRA_WIFI_AP_STATE = "wifi_state"
        private const val WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED"
        private const val WIFI_AP_STATE_DISABLING = 10
        private const val WIFI_AP_STATE_DISABLED = 11
        private const val WIFI_AP_STATE_ENABLING = 12
        private const val WIFI_AP_STATE_ENABLED = 13
        private const val WIFI_AP_STATE_FAILED = 14
        private var hotspotManager: HotspotManager? = null

        fun getHotspotManager(mainActivity: MainActivity): HotspotManager {
            if (hotspotManager == null)
                hotspotManager = HotspotManager(mainActivity)
            return hotspotManager as HotspotManager
        }
    }
}