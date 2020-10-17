package com.example.mayur.byteshare.bloc

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.delay

class WifiAssistant private constructor(private val context: Context) {
    private val wifiManager: WifiManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    companion object {
        private var wifiAssistant: WifiAssistant? = null

        @Synchronized
        fun getInstance(context: Context): WifiAssistant {
            if (wifiAssistant == null) wifiAssistant = WifiAssistant(context)
            return wifiAssistant!!
        }
    }

    suspend fun enableWifi(): Boolean {
//        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q) {
        wifiManager.isWifiEnabled = true
//        }
        delay(1000)
        return wifiManager.wifiState == WifiManager.WIFI_STATE_ENABLED || wifiManager.wifiState == WifiManager.WIFI_STATE_ENABLING || wifiManager.isWifiEnabled
    }
}