package com.example.mayur.byteshare.connection.hotspot

import android.graphics.Bitmap
import com.example.mayur.byteshare.connection.hotspot.HotspotManager

interface HotspotManagerCallbackListener {

    fun hotspotStarted(bitmap: Bitmap, hotspotManager: HotspotManager)

}
