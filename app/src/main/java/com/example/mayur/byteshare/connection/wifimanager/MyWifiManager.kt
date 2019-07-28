package com.example.mayur.byteshare.connection.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.wifi.SupplicantState
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.widget.Toast
import com.example.mayur.byteshare.Constants
import com.example.mayur.byteshare.MainActivity
import com.example.mayur.byteshare.connection.communication.Communication
import com.example.mayur.byteshare.connection.communication.OnCommunicationInterrupted
import com.example.mayur.byteshare.connection.communication.OnCommunicationSuccessful
import com.example.mayur.byteshare.connection.communication.OnConnectionTerminated
import com.example.mayur.byteshare.connection.hotspot.CouldNotStartHotspotFragment
import com.example.mayur.byteshare.connection.intent.Intents
import com.example.mayur.byteshare.connection.location.LocationPermissionFragment
import com.example.mayur.byteshare.connection.logger.Logger
import com.google.zxing.integration.android.IntentIntegrator
import java.util.*


class MyWifiManager private constructor(private val mainActivity: MainActivity) {
    var communication: Communication
    private val intentFilter: IntentFilter
    private val broadcastReceiver: BroadcastReceiver
    private val wifiScannerBroadcastReceiver: BroadcastReceiver
    private val startingWifiFragment: StartingWifiFragment
    private val scanner: IntentIntegrator
    private val fragmentManager: androidx.fragment.app.FragmentManager
    private val connectingFragment: StartingWifiFragment
    private val wifiManager: WifiManager?
    private var wifiTimer: Timer? = null
    private var connectingFragmentOpened = false
    private var ssid = ""
    private var password = ""
    private var isReceiverRegistered = false
    private var isScanBroadcastReceiverRegistered = false
    private var isWifiEnabled = false
    private var isSsidFound = false

    init {

        wifiManager =
                mainActivity.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        communication = Communication(Constants.WifiConstants.TYPE_WIFI, mainActivity)
        val bundle = Bundle()
        bundle.putBoolean("connecting", false)
        scanner = IntentIntegrator(mainActivity)
        scanner.setBeepEnabled(false)
        startingWifiFragment = StartingWifiFragment()
        startingWifiFragment.setMyWifiManager(this)
        startingWifiFragment.arguments = bundle
        fragmentManager = mainActivity.supportFragmentManager
        connectingFragment = StartingWifiFragment()
        connectingFragment.setMyWifiManager(this)
        val bundle1 = Bundle()
        bundle1.putBoolean("connecting", true)
        connectingFragment.arguments = bundle1
        intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        //        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)


        wifiScannerBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!MainActivity.isConnected) {
                    if (ssid != "") {
                        for (scanResult in wifiManager.scanResults) {
                            if (scanResult.SSID == ssid /*&& !isSsidFound*/) {
                                //                                isSsidFound = true;
                                Logger.log(
                                    "ScanResult available $ssid $password ",
                                    this@MyWifiManager
                                )
                                connectWiFi(ssid, password, scanResult.capabilities)
                                unregisterScannerBroadcastReceiver()
                                Timer(true).scheduleAtFixedRate(object : TimerTask() {
                                    override fun run() {
                                        val wifiInfo = wifiManager.connectionInfo

                                        Logger.log(
                                            "ssid in Timer :- " + wifiInfo!!.ssid,
                                            this@MyWifiManager
                                        )
                                        if (wifiInfo.ssid == "\"" + ssid + "\"") {
                                            communication.setOnCommunicationInterruptedListener(
                                                object : OnCommunicationInterrupted {
                                                    override fun onCommunicationInterrupted() {

                                                    }
                                                })

                                            communication.setOnCommunicationSuccessfulListener(
                                                object : OnCommunicationSuccessful {
                                                    override fun onCommunicationSuccessful(
                                                        deviceName: String
                                                    ) {
                                                        Logger.log(
                                                            "Communication successful",
                                                            this@MyWifiManager
                                                        )
                                                        MainActivity.handler.post {
                                                            connectingFragmentOpened = false
                                                            connectingFragment.dismissAllowingStateLoss()
                                                            mainActivity.connectionSuccess(
                                                                deviceName
                                                            )
//                                                            Toast.makeText(
//                                                                mainActivity,
//                                                                "Communication successful...",
//                                                                Toast.LENGTH_SHORT
//                                                            ).show()
                                                            //                                                            mainActivity.startActivity(new Intent(mainActivity, MainActivity.class));
                                                        }

//                                                        Thread(Runnable {
////                                                            TransferWifi.getSender(mainActivity)
//
//                                                        }).start()
                                                        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(mainActivity).sendBroadcast(
                                                            Intent(mainActivity, TransferWifi::class.java)
                                                        )
                                                    }
                                                })
                                            communication.setOnConnectionTerminatedListener(object: OnConnectionTerminated{
                                                override fun onCommunicationTerminated() {
                                                    isSsidFound = false
                                                    terminateConnection()
                                                    androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(mainActivity).sendBroadcast(
                                                        Intent(Intents.ACTION_STOP_WIFI_SERVICE)
                                                    )
//                                                    TransferWifi.getSender(mainActivity)
//                                                        .terminateConnection()
                                                    communication.terminateConnection()
                                                    mainActivity.connectionTerminated()
                                                }
                                            })

                                            Logger.log("Making Communication", this@MyWifiManager)
                                            communication.makeCommunication()
                                            cancel()
                                        }
                                    }
                                }, 0, 1000)

                                //                                        new Thread(new Runnable() {
                                //                                            @Override
                                //                                            public void run() {
                                //
                                //                                                try {
                                //                                                    Thread.sleep(2500);
                                //                                                } catch (InterruptedException e) {
                                //                                                    e.printStackTrace();
                                //                                                }
                                //
                                //                                            }
                                //                                        }).start();
                                //                                        break;
                            }
                        }
                    }
                }
            }
        }
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action!!
                when (action) {
                    WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                        Logger.log("WIFI_STATE_CHANGED_ACTION", this@MyWifiManager)
                        stateChanged(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1))
                    }
                    WifiManager.SUPPLICANT_STATE_CHANGED_ACTION -> {
                        Logger.log("SUPPLICANT_STATE_CHANGED_ACTION", this@MyWifiManager)
                        if (MainActivity.isConnected) {
                            val supplicantState =
                                intent.getParcelableExtra<SupplicantState>(WifiManager.EXTRA_NEW_STATE)
                            if (supplicantState != null && supplicantState == SupplicantState.DISCONNECTED) {
                                terminateConnection()
                                isSsidFound = false
                                mainActivity.connectionTerminated()
                                communication.terminateConnection()
//                                TransferWifi.getSender(mainActivity).terminateConnection()
                                androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(mainActivity).sendBroadcast(
                                    Intent(Intents.ACTION_STOP_WIFI_SERVICE)
                                )
                            }
                            MainActivity.isConnected = false
                        }
                    }
                }
            }
        }
    }

    private fun stateChanged(state: Int) {
        when (state) {
            WifiManager.WIFI_STATE_DISABLED -> {
                isSsidFound = false
                isWifiEnabled = false
                println("WIFI_STATE_DISABLING")
            }
            WifiManager.WIFI_STATE_DISABLING -> println("WIFI_STATE_DISABLING")
            WifiManager.WIFI_STATE_ENABLING -> {
                println("WIFI_STATE_ENABLING")
                println("WIFI_STATE_ENABLED...")
                isWifiEnabled = true
            }
            WifiManager.WIFI_STATE_ENABLED -> {
                println("WIFI_STATE_ENABLED...")
                isWifiEnabled = true
            }
            WifiManager.WIFI_STATE_UNKNOWN -> {
                println("WIFI_STATE_UNKNOWN")
                Toast.makeText(
                    mainActivity,
                    "Couldn't start WI-FI unknown error",
                    Toast.LENGTH_LONG
                ).show()
            }
        }//If stopped in between of sharing then do something...
    }

    private fun registerScannerBroadcastReceiver() {
        if (!isScanBroadcastReceiverRegistered)
            mainActivity.registerReceiver(
                wifiScannerBroadcastReceiver,
                IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            )
        isScanBroadcastReceiverRegistered = true
    }

    private fun unregisterScannerBroadcastReceiver() {
        if (isScanBroadcastReceiverRegistered)
            mainActivity.unregisterReceiver(wifiScannerBroadcastReceiver)
        isScanBroadcastReceiverRegistered = false
    }

    fun startWifi() {
        isSsidFound = false
        registerScannerBroadcastReceiver()
        registerReceiver()
        object : Thread() {
            override fun run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val androidLocationManager =
                        mainActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val gpsEnabled =
                        androidLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                    //                    boolean networkEnabled = androidLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                    if (!gpsEnabled) {
                        MainActivity.handler.post {
                            val locationPermissionFragment = LocationPermissionFragment()
                            fragmentManager.beginTransaction().add(locationPermissionFragment, null)
                                .commitAllowingStateLoss()
                        }
                        return
                    }
                }

                mainActivity.runOnUiThread {
                    fragmentManager
                        .beginTransaction()
                        .add(startingWifiFragment, null)
                        .setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commitAllowingStateLoss()
                }
                wifiTimer = Timer(true)
                wifiTimer!!.scheduleAtFixedRate(object : TimerTask() {
                    var time = 0

                    override fun run() {
                        wifiManager!!.isWifiEnabled = true
                        if (isWifiEnabled) {
                            onWifiEnabled(true)
                            cancel()
                        } else {
                            if (time >= 12000) {
                                onWifiEnabled(false)
                                cancel()
                            }
                        }
                        time += 1000
                    }
                }, 0, 1000)
            }
        }.start()
    }


    fun resultGenerated(result: String?) {
        object : Thread() {
            override fun run() {
                if (result != null && result.contains("&&")) {

                    ssid =
                            result.split("&&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                    password =
                            result.split("&&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                    ip = result.split("&&")[2]

                    if (password == "NoPasswordMayurPrajapati") {
                        password = ""
                    }
                    wifiManager!!.startScan()

                    mainActivity.runOnUiThread {
                        fragmentManager.beginTransaction()
                            .add(connectingFragment, null)
                            .setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .commitAllowingStateLoss()
                        connectingFragmentOpened = true
                    }
                } else {
                    closeWifi()
                }
            }
        }.start()

    }

    internal fun stopWifi() {
        isSsidFound = false
        object : Thread() {
            override fun run() {
                wifiTimer!!.cancel()
                if (connectingFragmentOpened) {
                    connectingFragmentOpened = false
                }
                wifiManager!!.isWifiEnabled = false
            }
        }.start()
    }

    private fun onWifiEnabled(enabled: Boolean) {
        //If wifi enabled then stop loading fragment
        Logger.log("Wifi enabled $enabled", this)
        if (enabled) {
            wifiManager!!.startScan()
            Logger.log("Starting scan", this)
            startingWifiFragment.dismissAllowingStateLoss()
            scanner.setPrompt("Scan QR Code to start sharing")
            scanner.initiateScan()
        } else {
            if (wifiManager!!.isWifiEnabled)
                onWifiEnabled(true)
            isSsidFound = false
            mainActivity.runOnUiThread {
                val couldNotStartHotspotFragment = CouldNotStartHotspotFragment()
                val bundle = Bundle()
                bundle.putBoolean("wifi", true)
                startingWifiFragment.dismissAllowingStateLoss()
                couldNotStartHotspotFragment.arguments = bundle
                fragmentManager
                    .beginTransaction()
                    .add(couldNotStartHotspotFragment, null)
                    .setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commitAllowingStateLoss()
            }
        }
    }


    private fun connectWiFi(SSID: String, password: String, Security: String) {
        Logger.log("ConnectWifi $SSID $password $Security", this@MyWifiManager)
        try {
            val conf = WifiConfiguration()
            conf.SSID = "\"" + SSID +
                    "\""   // Please note the quotes. String should contain ssid in quotes
            conf.status = WifiConfiguration.Status.ENABLED
            conf.priority = 2000

            when {
                Security.toUpperCase().contains("WEP") -> {
                    Logger.log("Configuring WEP ", this@MyWifiManager)
                    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                    conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                    conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                    conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
                    conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED)
                    conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                    conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)

                    if (password.matches("^[0-9a-fA-F]+$".toRegex())) {
                        conf.wepKeys[0] = password
                    } else {
                        conf.wepKeys[0] = "\"" + password + "\""
                    }
                    conf.wepTxKeyIndex = 0

                }
                Security.toUpperCase().contains("WPA") -> {
                    Logger.log("Configuring WPA ", this@MyWifiManager)

                    conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                    conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                    conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                    conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
                    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
                    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)

                    conf.preSharedKey = "\"" + password + "\""
                }
                else -> {
                    Logger.log("Configuring OPEN network ", this@MyWifiManager)
                    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                    conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                    conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                    conf.allowedAuthAlgorithms.clear()
                    conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                    conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
                    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
                    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
                }
            }

            val networkId = wifiManager!!.addNetwork(conf)

            wifiManager.disconnect()
            wifiManager.enableNetwork(networkId, true)
            wifiManager.reconnect()
            Logger.log("Adding result $networkId ", this@MyWifiManager)

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun terminateConnection() {
        isSsidFound = false
        Thread(object : Runnable {
            override fun run() {
                Logger.log("Terminating connection ", this)
                //                try {
                //                    Thread.sleep(2000);
                //                } catch (InterruptedException e) {
                //                    e.printStackTrace();
                //                }
                closeWifi()
            }
        }).start()
    }

    private fun closeWifi() {
        if (wifiManager != null)
            wifiManager.isWifiEnabled = false
        isSsidFound = false
        ssid = ""
        Logger.log("Wifi closed ", this)
        unregisterReceiver()
        unregisterScannerBroadcastReceiver()
    }

    private fun registerReceiver() {
        Logger.log("Registering receiver ", this)
        if (!isReceiverRegistered) {
            mainActivity.registerReceiver(broadcastReceiver, intentFilter)
            isReceiverRegistered = true
        }
    }

    private fun unregisterReceiver() {
        Logger.log("Unregistering receiver ", this)
        if (isReceiverRegistered) {
            isReceiverRegistered = false
            mainActivity.unregisterReceiver(broadcastReceiver)
        }
    }

    companion object {
        var ip = ""
        private var myWifiManager: MyWifiManager? = null

        fun getMyWifiManager(): MyWifiManager?{
            return myWifiManager
        }

        fun getMyWifiManager(mainActivity: MainActivity): MyWifiManager {
            if (myWifiManager == null)
                myWifiManager = MyWifiManager(mainActivity)
            return myWifiManager as MyWifiManager
        }
    }
}