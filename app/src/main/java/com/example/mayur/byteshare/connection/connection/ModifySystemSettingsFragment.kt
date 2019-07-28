package com.example.mayur.xportal.connection.connection


import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

import com.example.mayur.xportal.R


/**
 * A simple [Fragment] subclass.
 */
class ModifySystemSettingsFragment : BottomSheetDialogFragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val view = inflater.inflate(R.layout._fragment_modify_system_settings, container, false)
        val cancel = view.findViewById<Button>(R.id.btn_cancel_system_settings)
        val grant = view.findViewById<Button>(R.id.btn_grant_system_settings)

        cancel.setOnClickListener { this@ModifySystemSettingsFragment.dismiss() }

        grant.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                startActivity(intent)
                this@ModifySystemSettingsFragment.dismiss()
            }
        }

        return view
    }

}// Required empty public constructor