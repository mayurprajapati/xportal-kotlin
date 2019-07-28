package com.example.mayur.xportal.connection.hotspot


import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

import com.example.mayur.xportal.R


/**
 * A simple [Fragment] subclass.
 */
class CouldNotStartHotspotFragment : BottomSheetDialogFragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =
            inflater.inflate(R.layout.connection_fragment_could_not_start_hotspot, container, false)
        assert(arguments != null)
        if (arguments!!.getBoolean("wifi")) {
            val textView = view.findViewById<TextView>(R.id.txt_could_not)
            textView.text = "Could not start Wifi for sharing"
        }
        val cancel = view.findViewById<Button>(R.id.btn_cancel_settings)
        val openSettings = view.findViewById<Button>(R.id.btn_open_settings)

        cancel.setOnClickListener { this@CouldNotStartHotspotFragment.dismiss() }

        openSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_SETTINGS))
            this@CouldNotStartHotspotFragment.dismiss()
        }
        return view
    }

}// Required empty public constructor
