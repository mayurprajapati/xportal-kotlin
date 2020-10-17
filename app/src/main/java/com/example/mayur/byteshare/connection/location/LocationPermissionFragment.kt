package com.example.mayur.byteshare.connection.location

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

import com.example.mayur.byteshare.R
import com.example.mayur.byteshare.connection.hotspot.HotspotManager

class LocationPermissionFragment : BottomSheetDialogFragment() {

    private var hotspotManager: HotspotManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        this.isCancelable = false
        val view = inflater.inflate(
            R.layout.connection_layout_location_request_bottomsheet,
            container,
            false
        )
        val cancel = view.findViewById<Button>(R.id.btn_cancel_location)
        val grant = view.findViewById<Button>(R.id.btn_grant_location)
        cancel.setOnClickListener { this@LocationPermissionFragment.dismiss() }

        grant.setOnClickListener {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

            startActivityForResult(intent, 8899)

            this@LocationPermissionFragment.dismiss()
        }

        return view
    }


    fun setHotspotManager(hotspotManager: HotspotManager) {
        this.hotspotManager = hotspotManager
    }
}
