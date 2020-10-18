package com.example.mayur.byteshare.fragments.apps


import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.mayur.byteshare.MainActivity
import com.example.mayur.byteshare.R
import com.example.mayur.byteshare.bloc.interfaces.selectedCount
import com.example.mayur.byteshare.connection.FileInfo
import com.example.mayur.byteshare.connection.hotspot.TransferHotspot
import com.example.mayur.byteshare.connection.wifimanager.TransferWifi
import com.example.mayur.byteshare.ioCoroutine
import kotlinx.coroutines.launch
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
                    Thread {
                        val sender = TransferHotspot.getSender(mainActivity, false)
                        val s = appsAdapter.getAllSelectedApps()
                        if (sender != null) {
                            val fileInfos = ArrayList<FileInfo>()
                            for (i in 0 until appsAdapter.appsBloc.apps.selectedCount()) {
                                fileInfos.add(FileInfo(s[i].f, s[i].appName, false))
                            }
                            sender.sendFiles(fileInfos)
                            MainActivity.handler.post {
                                appsAdapter.clearCount()
                                cardViewCount.visibility = View.GONE
                            }
                        }
                    }.start()
                } else {
                    Thread {
                        val fileInfos = ArrayList<FileInfo>()
                        val s = appsAdapter.getAllSelectedApps()
                        for (i in 0 until appsAdapter.appsBloc.apps.selectedCount()) {
                            fileInfos.add(FileInfo(s[i].f, s[i].appName, false))
                        }
                        TransferWifi.getSender(mainActivity).sendFiles(fileInfos)

                        MainActivity.handler.post {
                            appsAdapter.clearCount()
                            cardViewCount.visibility = View.GONE
                        }
                    }.start()
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

        uninstallPackage.setOnClickListener { ioCoroutine.launch { appsAdapter.uninstallSelectedPackages() } }
        cancelSelection.setOnClickListener(appsAdapter.cancelSelectionOnClickListener)
        //        checkBoxSelectAll.setOnCheckedChangeListener(appsAdapter.selectAllOnCheckedChangeListener);
        checkBoxSelectAll.setOnClickListener(appsAdapter.selectAllOnClickListener)

        val selectedCount = appsAdapter.appsBloc.apps.selectedCount()
        checkBoxSelectAll.isChecked = selectedCount == appsAdapter.itemCount
        if (selectedCount > 0) {
            cardViewCount.visibility = View.VISIBLE
            textViewCount.text = selectedCount.toString()
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