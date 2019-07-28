package com.example.mayur.xportal.fragments.apps


import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.CardView
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.mayur.xportal.MainActivity
import com.example.mayur.xportal.R
import com.example.mayur.xportal.connection.FileInfo
import com.example.mayur.xportal.connection.hotspot.TransferHotspot
import com.example.mayur.xportal.connection.wifi.TransferWifi
import java.util.*


/**
 * A simple [Fragment] subclass.
 */
class AppsFragment : Fragment() {

    private lateinit var cancelSelection: ImageButton
    private lateinit var uninstallPackage: ImageButton
    internal lateinit var checkBoxSelectAll: CheckBox
    private lateinit var btnSend: Button
    private lateinit var textViewCount: TextView
    private lateinit var mainActivity: MainActivity
    private lateinit var appsAdapter: AppsAdapter
    private lateinit var fileProperties: ImageButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.apps_fragment_apps, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewApps)
        recyclerView.layoutManager = GridLayoutManager(recyclerView.context, 4)

        mainActivity = activity as MainActivity

        appsAdapter = AppsAdapter(
            activity as MainActivity,
            recyclerView,
            this
        )

        swipeRefreshLayout = view.findViewById(R.id.appsSwipeRefresh)
        swipeRefreshLayout.setOnRefreshListener(appsAdapter)
        recyclerView.adapter = appsAdapter

        fileProperties = mainActivity.cardViewCount.findViewById(R.id.fileProperties)
        cancelSelection = mainActivity.findViewById(R.id.cancelSelectionImageButton)
        checkBoxSelectAll = mainActivity.findViewById(R.id.selectAllCheckBox)
        textViewCount = mainActivity.findViewById(R.id.txtSelectionCount)
        btnSend = mainActivity.cardViewCount.findViewById(R.id.btnSend)
        uninstallPackage = mainActivity.cardViewCount.findViewById(R.id.btnFileDelete)
        return view
    }
    
    fun showCount(count: Int) {
        mainActivity.let {
            if (it.mViewPager.currentItem == 1) {
                if (count <= 0) {
                    it.cardViewCount.visibility = View.GONE
                    return
                }
                it.cardViewCount.visibility = View.VISIBLE
                textViewCount.text = count.toString()
            }
        }
    }

    fun updateCardView(cardViewCount: CardView) {
        btnSend.setOnClickListener {
            if (MainActivity.isConnected) {
                if (MainActivity.isSender) {
                    Thread(Runnable {
                        val sender = TransferHotspot.getSender(mainActivity, false)
                        val s = appsAdapter.getAllSelectedApps()
                        if (sender != null) {
                            val fileInfos = ArrayList<FileInfo>()
                            for (i in 0 until appsAdapter.selectedItemsCount) {
                                fileInfos.add(FileInfo(s[i].f, s[i].appName, false))
                            }
                            sender.sendFiles(fileInfos)
                            MainActivity.handler.post {
                                appsAdapter.clearCount()
                                cardViewCount.visibility = View.GONE
                            }
                        }
                    }).start()
                } else {
                    Thread(Runnable {
                        val fileInfos = ArrayList<FileInfo>()
                        val s = appsAdapter.getAllSelectedApps()
                        for (i in 0 until appsAdapter.selectedItemsCount) {
                            fileInfos.add(FileInfo(s[i].f, s[i].appName, false))
                        }
                        TransferWifi.getSender(mainActivity).sendFiles(fileInfos)

                        MainActivity.handler.post {
                            appsAdapter.clearCount()
                            cardViewCount.visibility = View.GONE
                        }
                    }).start()
                }

            } else {
                mainActivity.mViewPager.currentItem = 0
                Snackbar.make(
                    mainActivity.mViewPager,
                    "Make connection to send file.",
                    Snackbar.LENGTH_LONG
                ).show()
                cardViewCount.visibility = View.GONE
            }
        }

        uninstallPackage.setOnClickListener { appsAdapter.removeSelectedPackages() }
        cancelSelection.setOnClickListener(appsAdapter.cancelSelectionOnClickListener)
        //        checkBoxSelectAll.setOnCheckedChangeListener(appsAdapter.selectAllOnCheckedChangeListener);
        checkBoxSelectAll.setOnClickListener(appsAdapter.selectAllOnClickListener)
        checkBoxSelectAll.isChecked = appsAdapter.selectedItemsCount == appsAdapter.itemCount

        if (appsAdapter.selectedItemsCount > 0) {
            cardViewCount.visibility = View.VISIBLE
            textViewCount.text = appsAdapter.selectedItemsCount.toString()
        } else {
            cardViewCount.visibility = View.GONE
        }

        fileProperties.setOnClickListener { v ->
            val popupMenu = PopupMenu(context, v)
            popupMenu.menu.add("Properties").setIcon(R.drawable.ic_info_black_24dp)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Properties" -> appsAdapter.showProperties()
                }
                true
            }
            popupMenu.show()
        }
    }

    fun refreshCompleted() {
        swipeRefreshLayout.isRefreshing = false
    }
}