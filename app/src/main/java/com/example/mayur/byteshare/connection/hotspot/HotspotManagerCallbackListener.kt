package com.example.mayur.xportal.connection.hotspot

import android.graphics.Bitmap

interface HotspotManagerCallbackListener {

    fun hotspotStarted(bitmap: Bitmap, hotspotManager: HotspotManager)

}
