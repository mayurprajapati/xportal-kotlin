package com.example.mayur.byteshare.fragments.history


import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.example.mayur.byteshare.MainActivity
import com.example.mayur.byteshare.R

/**
 * A simple [Fragment] subclass.
 */
class HistoryReceiveFragment : Fragment() {


    lateinit var recyclerView: RecyclerView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val view = inflater.inflate(R.layout.history_fragment_history_receive, container, false)
        recyclerView = view.findViewById(R.id.historyReceiveRecyclerView)
        historyAdapterReceiver = HistoryAdapterReceiver.newInstance(activity as MainActivity?, this)!!

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = historyAdapterReceiver
        return view
    }

    fun updateCardView() {
        //Just update CardView
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var historyAdapterReceiver: HistoryAdapterReceiver
    }

}// Required empty public constructor