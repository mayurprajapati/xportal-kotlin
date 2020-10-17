package com.example.mayur.byteshare.bloc

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*
import android.net.wifi.p2p.WifiP2pManager.*
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import com.example.mayur.byteshare.MainActivity
import com.example.mayur.byteshare.ioCoroutine
import com.example.mayur.byteshare.utils.Utils.hasPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.util.*

/**
 * Created by Bruce Too
 * On 27/04/2018.
 * At 10:21
 * Use example:
 * 1. Init assistant by [WifiP2PAssistant.getInstance]
 *
 * 2. Register broadcaster and check if P2P is enable by [WifiP2PAssistant.enable]
 * unregister by [WifiP2PAssistant.disable]
 *
 * 3. Create GO(let the caller device be GO) by [WifiP2PAssistant.createGroup]
 * remove by [WifiP2PAssistant.removeGroup]
 *
 * 4. Discover peer devices by [WifiP2PAssistant.discoverPeers]
 * cancel operation by [WifiP2PAssistant.cancelDiscoverPeers]
 *
 * 5. Connect one single Peer by [WifiP2PAssistant.connect]
 *
 * 6. Register key lifecycle event callback by [WifiP2PAssistant.setCallback]
 *
 * More apis to see the detail below..
 */
class WifiP2PAssistant private constructor(private val activity: MainActivity) {
    /**
     * Key process event callback listener
     */
    interface WifiP2PAssistantCallback {
        fun onWifiP2PEvent(event: Event)
    }

    private val mCurrentPeers: MutableList<WifiP2pDevice> = ArrayList()
    private var mContext: Context? = null
    var isWifiP2pEnabled: Boolean = false
        private set
    private val mIntentFilter: IntentFilter
    private val mWifiP2pChannel: Channel
    private val mWifiP2pManager: WifiP2pManager?
    private var mReceiver: WifiP2pBroadcastReceiver?
    private val mConnectionListener: WifiP2PConnectionInfoListener
    private val mPeerListListener: WifiP2PPeerListListener
    private val mGroupInfoListener: WifiP2PGroupInfoListener
    private var mFailureReason: Int = ERROR
    var connectStatus: ConnectStatus = ConnectStatus.NOT_CONNECTED
        private set
    private var mLastEvent: Event? = null

    /**
     * Get the device mac address
     *
     * @return mac address
     */
    var deviceMacAddress: String = ""
        private set

    /**
     * Get the device name,if want to set device name,you need
     * reflect call [WifiP2pManager.setDeviceName]
     *
     * @return device name
     */
    var deviceName: String = ""
        private set

    /**
     * Get the IP address of the group owner
     *
     * @return ip address
     */
    var groupOwnerAddress: InetAddress? = null
        private set

    /**
     * Get the group owners mac address
     *
     * @return mac address
     */
    var groupOwnerMacAddress: String = ""
        private set

    /**
     * Get the group owners device name
     *
     * @return device name
     */
    var groupOwnerName: String = ""
        private set

    /**
     * Return the passphrase for this network; only valid if this device is the group owner
     *
     * @return the passphrase to this device
     */
    var passphrase: String? = ""
        private set
    private var mGroupFormed: Boolean = false

    // tracks the number of clients, must be thread safe
    private var clients: Int = 0
    var callback: WifiP2PAssistantCallback? = null

    /**
     * Key lifecycle event enum
     */
    enum class Event {
        DISCOVERING_PEERS, PEERS_AVAILABLE, GROUP_CREATED, CONNECTING, CONNECTED_AS_PEER, CONNECTED_AS_GROUP_OWNER, DISCONNECTED, CONNECTION_INFO_AVAILABLE, ERROR
    }

    /**
     * P2P Connect Status
     */
    enum class ConnectStatus {
        NOT_CONNECTED, CONNECTING, CONNECTED, GROUP_OWNER, ERROR
    }

    /*
     * Maintains the list of wifi p2p peers available
     */
    private inner class WifiP2PPeerListListener : PeerListListener {
        /*
         * mEventCallback method, called by Android when the peer list changes
         */
        override fun onPeersAvailable(peerList: WifiP2pDeviceList) {
            mCurrentPeers.clear()
            mCurrentPeers.addAll(peerList.deviceList)
            Log.v(TAG, "Wifi P2P peers found: " + mCurrentPeers.size)
            for (peer: WifiP2pDevice in mCurrentPeers) {
                // deviceAddress is the MAC address, deviceName is the human readable name
                val s: String = "    peer: " + peer.deviceAddress + " " + peer.deviceName
                Log.v(TAG, s)
            }
            fireEvent(Event.PEERS_AVAILABLE)
        }
    }

    private val p2pPermissions = arrayOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
    )

    private suspend fun hasP2PPermissions(): Boolean = withContext(Dispatchers.Main) {
        return@withContext activity.hasPermissions(*p2pPermissions)
    }

    /*
     * Updates when this device connects
     */
    private inner class WifiP2PConnectionInfoListener :
            ConnectionInfoListener {
        @SuppressLint("MissingPermission")
        override fun onConnectionInfoAvailable(info: WifiP2pInfo) {
            ioCoroutine.launch {
                if (mWifiP2pManager == null) return@launch
                //when the connection state changes, request group info to find GO
                if (!hasP2PPermissions()) {
                    Log.v(TAG, "Permission not granted", Exception())
                    return@launch
                }

                mWifiP2pManager.requestGroupInfo(mWifiP2pChannel, mGroupInfoListener)
                groupOwnerAddress = info.groupOwnerAddress
                Log.v(TAG, "Group owners address: " + groupOwnerAddress.toString())
                if (info.groupFormed && info.isGroupOwner) {
                    Log.v(TAG, "Wifi P2P group formed, this device is the group owner (GO)")
                    connectStatus = ConnectStatus.GROUP_OWNER
                    fireEvent(Event.CONNECTED_AS_GROUP_OWNER)
                } else if (info.groupFormed) {
                    Log.v(TAG, "Wifi P2P group formed, this device is a client")
                    connectStatus = ConnectStatus.CONNECTED
                    fireEvent(Event.CONNECTED_AS_PEER)
                } else {
                    Log.v(
                            TAG,
                            "Wifi P2P group NOT formed, ERROR: $info"
                    )
                    mFailureReason = ERROR // there is no error code for this
                    connectStatus = ConnectStatus.ERROR
                    fireEvent(Event.ERROR)
                }
            }
        }
    }

    private inner class WifiP2PGroupInfoListener : GroupInfoListener {
        override fun onGroupInfoAvailable(group: WifiP2pGroup) {
            if (group.isGroupOwner) {
                groupOwnerMacAddress = deviceMacAddress
                groupOwnerName = deviceName
            } else {
                val go: WifiP2pDevice = group.owner
                groupOwnerMacAddress = go.deviceAddress
                groupOwnerName = go.deviceName
            }
            passphrase = group.passphrase

            // make sure passphrase isn't null
            passphrase = if ((passphrase != null)) passphrase else ""
            Log.v(TAG, "Wifi P2P connection information available")
            fireEvent(Event.CONNECTION_INFO_AVAILABLE)
        }
    }

    private inner class WifiP2pBroadcastReceiver : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            ioCoroutine.launch {
                val action: String? = intent.action
                if ((WIFI_P2P_STATE_CHANGED_ACTION == action)) {
                    val state: Int = intent.getIntExtra(EXTRA_WIFI_STATE, -1)
                    isWifiP2pEnabled = (state == WIFI_P2P_STATE_ENABLED)
                    Log.v(TAG, "Wifi P2P state - enabled: $isWifiP2pEnabled")
                } else if ((WIFI_P2P_PEERS_CHANGED_ACTION == action)) {
                    Log.v(TAG, "Wifi P2P peers changed")
                    if (mWifiP2pManager == null) return@launch
                    if (!hasP2PPermissions()) {
                        Log.v(TAG, "Permission not granted", Exception())
                        return@launch
                    }
                    mWifiP2pManager.requestPeers(mWifiP2pChannel, mPeerListListener)
                } else if ((WIFI_P2P_CONNECTION_CHANGED_ACTION == action)) {
                    val networkInfo: NetworkInfo = intent.getParcelableExtra(EXTRA_NETWORK_INFO)
                    val wifip2pinfo: WifiP2pInfo = intent.getParcelableExtra(EXTRA_WIFI_P2P_INFO)
                    Log.v(
                            TAG,
                            "Wifi P2P connection changed - connected: " + networkInfo.isConnected
                    )
                    if (mWifiP2pManager == null) return@launch
                    if (networkInfo.isConnected) {
                        mWifiP2pManager.requestConnectionInfo(mWifiP2pChannel, mConnectionListener)
                        mWifiP2pManager.stopPeerDiscovery(mWifiP2pChannel, null)
                    } else {
                        connectStatus = ConnectStatus.NOT_CONNECTED
                        if (!mGroupFormed) {
                            discoverPeers()
                        }
                        // if we were previously connected, notify that we are now disconnected
                        if (isConnected) {
                            fireEvent(Event.DISCONNECTED)
                        }
                        mGroupFormed = wifip2pinfo.groupFormed
                    }
                } else if ((WIFI_P2P_THIS_DEVICE_CHANGED_ACTION == action)) {
                    Log.v(TAG, "Wifi P2P this device changed")
                    val wifiP2pDevice: WifiP2pDevice = intent.getParcelableExtra<Parcelable>(
                            EXTRA_WIFI_P2P_DEVICE
                    ) as WifiP2pDevice
                    deviceName = wifiP2pDevice.deviceName
                    deviceMacAddress = wifiP2pDevice.deviceAddress
                    Log.v(TAG, "Wifi P2P device information: $deviceName $deviceMacAddress")
                }
            }
        }
    }

    @Synchronized
    fun enable() {
        clients += 1
        Log.v(
                TAG,
                "There are $clients Wifi P2P Assistant Clients (+)"
        )
        if (clients == 1) {
            Log.v(TAG, "Enabling Wifi P2P Assistant")
            if (mReceiver == null) mReceiver = WifiP2pBroadcastReceiver()
            mContext!!.registerReceiver(mReceiver, mIntentFilter)
        }
    }

    @Synchronized
    fun disable() {
        clients -= 1
        Log.v(
                TAG,
                "There are $clients Wifi P2P Assistant Clients (-)"
        )
        if (clients == 0) {
            Log.v(TAG, "Disabling Wifi P2P Assistant")
            mWifiP2pManager!!.stopPeerDiscovery(mWifiP2pChannel, null)
            mWifiP2pManager.cancelConnect(mWifiP2pChannel, null)
            try {
                mContext!!.unregisterReceiver(mReceiver)
            } catch (e: IllegalArgumentException) {
                // disable() was called, but enable() was never called; ignore
            }
            mLastEvent = null
        }
    }

    @get:Synchronized
    val isEnabled: Boolean
        get() = (clients > 0)
    val peersList: List<WifiP2pDevice>
        get() = ArrayList(mCurrentPeers)

    /**
     * Returns true if connected, or group owner
     *
     * @return true if connected, otherwise false
     */
    val isConnected: Boolean
        get() = ((connectStatus == ConnectStatus.CONNECTED
                || connectStatus == ConnectStatus.GROUP_OWNER))

    /**
     * Returns true if this device is the group owner
     *
     * @return true if group owner, otherwise false
     */
    val isGroupOwner: Boolean
        get() {
            return (connectStatus == ConnectStatus.GROUP_OWNER)
        }

    /**
     * Discover Wifi P2P peers
     */
    @SuppressLint("MissingPermission")
    fun discoverPeers() {
        ioCoroutine.launch {
            if (!hasP2PPermissions()) {
                Log.v(TAG, "Permission not granted", Exception())
                return@launch
            }
            mWifiP2pManager!!.discoverPeers(mWifiP2pChannel, object : ActionListener {
                override fun onSuccess() {
                    fireEvent(Event.DISCOVERING_PEERS)
                    Log.v(TAG, "Wifi P2P discovering peers")
                }

                override fun onFailure(reason: Int) {
                    val reasonStr: String = failureReasonToString(reason)
                    mFailureReason = reason
                    Log.v(
                            TAG,
                            "Wifi P2P failure while trying to discover peers - reason: $reasonStr"
                    )
                    fireEvent(Event.ERROR)
                }
            })
        }
    }

    /**
     * Cancel discover Wifi P2P peers request
     */
    fun cancelDiscoverPeers() {
        Log.v(TAG, "Wifi P2P stop discovering peers")
        mWifiP2pManager!!.stopPeerDiscovery(mWifiP2pChannel, null)
    }

    /**
     * Create a Wifi P2P group
     *
     *
     * Will receive a Event.GROUP_CREATED if the group is created. If there is an
     * error creating group Event.ERROR will be sent. If group already exists, no
     * event will be sent. However, a Event.CONNECTED_AS_GROUP_OWNER should be
     * received.
     */
    @SuppressLint("MissingPermission")
    fun createGroup() {
        ioCoroutine.launch {
            if (!hasP2PPermissions()) {
                Log.v(TAG, "Permission not granted", Exception())
                return@launch
            }
            mWifiP2pManager!!.createGroup(mWifiP2pChannel, object : ActionListener {
                override fun onSuccess() {
                    connectStatus = ConnectStatus.GROUP_OWNER
                    fireEvent(Event.GROUP_CREATED)
                    Log.v(TAG, "Wifi P2P created group")
                }

                override fun onFailure(reason: Int) {
                    if (reason == BUSY) {
                        // most likely group is already created
                        Log.v(TAG, "Wifi P2P cannot create group, does group already exist?")
                    } else {
                        val reasonStr: String = failureReasonToString(reason)
                        mFailureReason = reason
                        Log.v(
                                TAG,
                                "Wifi P2P failure while trying to create group - reason: $reasonStr"
                        )
                        connectStatus = ConnectStatus.ERROR
                        fireEvent(Event.ERROR)
                    }
                }
            })
        }
    }

    /**
     * Remove a Wifi P2P group
     */
    fun removeGroup() {
        mWifiP2pManager!!.removeGroup(mWifiP2pChannel, null)
    }

    @SuppressLint("MissingPermission")
    fun connect(peer: WifiP2pDevice) {
        ioCoroutine.launch {
            if (connectStatus == ConnectStatus.CONNECTING || connectStatus == ConnectStatus.CONNECTED) {
                Log.v(
                        TAG,
                        "WifiP2P connection request to " + peer.deviceAddress + " ignored, already connected"
                )
                return@launch
            }
            Log.v(TAG, "WifiP2P connecting to " + peer.deviceAddress)
            connectStatus = ConnectStatus.CONNECTING
            val config = WifiP2pConfig()
            config.deviceAddress = peer.deviceAddress
            config.wps.setup = WpsInfo.PBC
            config.groupOwnerIntent = 1
            if (!hasP2PPermissions()) {
                Log.v(TAG, "Permission not granted", Exception())
                return@launch
            }
            mWifiP2pManager!!.connect(mWifiP2pChannel, config, object : ActionListener {
                override fun onSuccess() {
                    Log.v(TAG, "WifiP2P connect started")
                    fireEvent(Event.CONNECTING)
                }

                override fun onFailure(reason: Int) {
                    val reasonStr: String = failureReasonToString(reason)
                    mFailureReason = reason
                    Log.v(
                            TAG,
                            "WifiP2P connect cannot start - reason: $reasonStr"
                    )
                    fireEvent(Event.ERROR)
                }
            })
        }
    }

    val failureReason: String
        get() {
            return failureReasonToString(mFailureReason)
        }

    private fun fireEvent(event: Event) {
        // don't send duplicate events
        if (mLastEvent == event && mLastEvent != Event.PEERS_AVAILABLE) return
        mLastEvent = event
        if (callback != null) callback!!.onWifiP2PEvent(event)
    }

    companion object {
        private val TAG: String = WifiP2PAssistant::class.java.simpleName
        private var sWifiP2PAssistant: WifiP2PAssistant? = null

        @Synchronized
        fun getInstance(activity: MainActivity): WifiP2PAssistant {
            if (sWifiP2PAssistant == null) sWifiP2PAssistant = WifiP2PAssistant(activity)
            return sWifiP2PAssistant!!
        }

        fun failureReasonToString(reason: Int): String {
            return when (reason) {
                P2P_UNSUPPORTED -> "P2P_UNSUPPORTED"
                ERROR -> "ERROR"
                BUSY -> "BUSY"
                else -> "UNKNOWN (reason $reason)"
            }
        }
    }

    init {
        mContext = activity

        // Set up the intent filter for wifi P2P
        mIntentFilter = IntentFilter()
        mIntentFilter.addAction(WIFI_P2P_STATE_CHANGED_ACTION)
        mIntentFilter.addAction(WIFI_P2P_PEERS_CHANGED_ACTION)
        mIntentFilter.addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION)
        mIntentFilter.addAction(WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        mWifiP2pManager = activity.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        mWifiP2pChannel = mWifiP2pManager.initialize(activity, Looper.getMainLooper(), null)
        mReceiver = WifiP2pBroadcastReceiver()
        mConnectionListener = WifiP2PConnectionInfoListener()
        mPeerListListener = WifiP2PPeerListListener()
        mGroupInfoListener = WifiP2PGroupInfoListener()
    }
}