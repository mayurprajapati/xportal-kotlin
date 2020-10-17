package com.example.mayur.byteshare.connection.wifimanager


import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.mayur.byteshare.connection.wifi.MyWifiManager
import com.example.mayur.byteshare.R

/**
 * A simple [Fragment] subclass.
 */
class StartingWifiFragment : BottomSheetDialogFragment() {

    private var myWifiManager: MyWifiManager? = null

    private var bundle: Bundle? = null

    init {
        myWifiManager = null
        // Required empty public constructor
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bundle = arguments
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        isCancelable = false
        assert(arguments != null)

        val view = inflater.inflate(R.layout.connection_fragment_wifi, container, false)
        val cancel = view.findViewById<Button>(R.id.btn_cancel_wifi)
        cancel.setOnClickListener {
            if (myWifiManager != null) {
                this@StartingWifiFragment.dismiss()
                myWifiManager!!.stopWifi()
            }
        }
        if (bundle!!.getBoolean("connecting")) {

            (view.findViewById<View>(R.id.txt_starting_wifi) as TextView).text = "Connecting..."
        } else {
            (view.findViewById<View>(R.id.txt_starting_wifi) as TextView).text =
                    "Starting Wifi for sharing"
        }
        return view
    }

    fun setMyWifiManager(wifiManager: MyWifiManager) {
        this.myWifiManager = wifiManager
    }

}
