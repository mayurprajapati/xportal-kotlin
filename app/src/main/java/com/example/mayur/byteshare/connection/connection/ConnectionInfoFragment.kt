package com.example.mayur.byteshare.connection.connection


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.example.mayur.byteshare.R


/**
 * A simple [Fragment] subclass.
 */
class ConnectionInfoFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.connection_info, container, false)
        (view.findViewById<View>(R.id.device_name) as TextView).text =
                arguments!!.getString("devicename")
        return view
    }

    companion object {

        fun newInstance(deviceName: String): ConnectionInfoFragment {
            val connectionInfoFragment = ConnectionInfoFragment()
            val bundle = Bundle()
            bundle.putString("devicename", deviceName)
            connectionInfoFragment.arguments = bundle
            return connectionInfoFragment
        }
    }
}
