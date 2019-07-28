package com.example.mayur.xportal.connection.connection


import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.CardView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.example.mayur.xportal.AppStyle
import com.example.mayur.xportal.R


/**
 * A simple [Fragment] subclass.
 */
class ConnectionFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        //        CardView send = view.findViewById(R.id.cardViewButtonSend);
        //        send.setCardBackgroundColor(Color.parseColor(AppStyle.currentThemeColor));
        //        CardView receive = view.findViewById(R.id.cardViewButtonReceive);
        //        receive.setCardBackgroundColor(Color.parseColor(AppStyle.currentThemeColor));
        return inflater.inflate(R.layout.connection_fragment_send_receive_buttons, container, false)
    }

}// Required empty public constructor
