package com.example.mayur.byteshare.connection.hotspot


import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.mayur.byteshare.connection.hotspot.HotspotManager
import com.example.mayur.byteshare.connection.hotspot.HotspotManagerCallbackListener

import com.example.mayur.byteshare.R

/**
 * A simple [Fragment] subclass.
 */
class QRCodeFragment : BottomSheetDialogFragment(), HotspotManagerCallbackListener {

    internal lateinit var view: View


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isCancelable = false
        view = inflater.inflate(R.layout.connection_layout_start_hotspot, container, false)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        progressBar.animate()
        return view
    }

    override fun hotspotStarted(bitmap: Bitmap, hotspotManager: HotspotManager) {
        val linearLayout = view as LinearLayout
        val textView = view.findViewById<TextView>(R.id.txt_status)
        textView.text = "Scan QR Code to start sharing"
        val cancel = view.findViewById<Button>(R.id.btn_cancel_hotspot)
        cancel.isEnabled = true
        cancel.setTextColor(Color.BLACK)
        cancel.setOnClickListener { hotspotManager.stopHotspot() }
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        linearLayout.removeView(progressBar)
        val imageView = view.findViewById<ImageView>(R.id.qrcode_imageview)
        imageView.setImageBitmap(bitmap)
    }

}// Required empty public constructor
