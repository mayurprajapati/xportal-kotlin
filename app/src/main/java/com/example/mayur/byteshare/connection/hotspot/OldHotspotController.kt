package com.example.mayur.byteshare.connection.hotspot

import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.util.Log

import java.lang.reflect.Method

class OldHotspotController private constructor(private val mgr: WifiManager) {

    // shouldn't happen
    val wifiApState: Int
        get() {
            try {
                return getWifiApState!!.invoke(mgr) as Int
            } catch (e: Exception) {
                Log.v("BatPhone", e.toString(), e)
                return -1
            }

        }

    // shouldn't happen
    val wifiApConfiguration: WifiConfiguration?
        get() {
            try {
                return getWifiApConfiguration!!.invoke(mgr) as WifiConfiguration
            } catch (e: Exception) {
                Log.v("BatPhone", e.toString(), e)
                return null
            }

        }

    fun isWifiApEnabled(): Boolean {
        try {
            return isWifiApEnabled!!.invoke(mgr) as Boolean
        } catch (e: Exception) {
            Log.v("BatPhone", e.toString(), e) // shouldn't happen
            return false
        }

    }

    fun setWifiApEnabled(config: WifiConfiguration?, enabled: Boolean): Boolean {
        return try {
            setWifiApEnabled?.invoke(mgr, config, enabled) as Boolean
        } catch (e: Exception) {
            Log.v("BatPhone", e.toString(), e) // shouldn't happen
            false
        }
    }

    companion object {
        private var getWifiApState: Method? = null
        private var isWifiApEnabled: Method? = null
        private var setWifiApEnabled: Method? = null
        private var getWifiApConfiguration: Method? = null

        val WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED"

        val WIFI_AP_STATE_DISABLED = WifiManager.WIFI_STATE_DISABLED
        val WIFI_AP_STATE_DISABLING = WifiManager.WIFI_STATE_DISABLING
        val WIFI_AP_STATE_ENABLED = WifiManager.WIFI_STATE_ENABLED
        val WIFI_AP_STATE_ENABLING = WifiManager.WIFI_STATE_ENABLING
        val WIFI_AP_STATE_FAILED = WifiManager.WIFI_STATE_UNKNOWN

        val EXTRA_PREVIOUS_WIFI_AP_STATE = WifiManager.EXTRA_PREVIOUS_WIFI_STATE
        val EXTRA_WIFI_AP_STATE = WifiManager.EXTRA_WIFI_STATE

        init {
            // lookup methods and fields not defined publicly in the SDK.
            val cls = WifiManager::class.java
            for (method in cls.declaredMethods) {
                val methodName = method.name
                if (methodName == "getWifiApState") {
                    getWifiApState = method
                } else if (methodName == "isWifiApEnabled") {
                    isWifiApEnabled = method
                } else if (methodName == "setWifiApEnabled") {
                    setWifiApEnabled = method
                } else if (methodName == "getWifiApConfiguration") {
                    getWifiApConfiguration = method
                }
            }
        }

        val isApSupported: Boolean
            get() = (getWifiApState != null && isWifiApEnabled != null
                    && setWifiApEnabled != null && getWifiApConfiguration != null)

        fun getApControl(mgr: WifiManager): OldHotspotController? {
            return if (!isApSupported) null else OldHotspotController(mgr)
        }
    }
}
