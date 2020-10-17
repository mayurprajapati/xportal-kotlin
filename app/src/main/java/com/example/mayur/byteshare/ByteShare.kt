package com.example.mayur.byteshare

import android.app.Application
import io.multimoon.colorful.Colorful
import io.multimoon.colorful.initColorful

class ByteShare: Application() {
    override fun onCreate() {
        super.onCreate()
        initColorful(this)
    }
}