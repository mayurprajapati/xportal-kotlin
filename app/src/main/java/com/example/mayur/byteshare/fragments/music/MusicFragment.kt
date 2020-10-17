package com.example.mayur.byteshare.fragments.music

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.mayur.byteshare.MainActivity
import com.example.mayur.byteshare.R
import com.example.mayur.byteshare.connection.FileInfo
import com.example.mayur.byteshare.connection.hotspot.TransferHotspot
import com.example.mayur.byteshare.connection.wifimanager.TransferWifi
import java.util.*

//import android.support.v7.widget.DividerItemDecoration;


/**
 * A simple [Fragment] subclass.
 */
class MusicFragment : Fragment() {


    lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var cancelSelection: ImageButton
    private lateinit var musicDelete: ImageButton
    internal lateinit var fileProperties: ImageButton
    internal lateinit var checkBoxSelectAll: CheckBox
    private lateinit var recyclerView: RecyclerView
    private lateinit var musicAdapter: MusicAdapter
    private lateinit var mainActivity: MainActivity
    private lateinit var textViewCount: TextView
    private lateinit var btnSend: Button


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.music_fragment_music, container, false)
        recyclerView = view.findViewById<View>(R.id.recyclerViewApps) as RecyclerView

        recyclerView.layoutManager = LinearLayoutManager(context)
        musicAdapter = MusicAdapter(activity as MainActivity, this@MusicFragment)
        recyclerView.adapter = musicAdapter
        mainActivity = activity as MainActivity

        textViewCount = mainActivity.cardViewCount.findViewById(R.id.txtSelectionCount)
        musicDelete = mainActivity.cardViewCount.findViewById(R.id.btnFileDelete)
        btnSend = mainActivity.cardViewCount.findViewById(R.id.btnSend)
        cancelSelection = mainActivity.findViewById(R.id.cancelSelectionImageButton)
        checkBoxSelectAll = mainActivity.findViewById(R.id.selectAllCheckBox)
        fileProperties = mainActivity.cardViewCount.findViewById(R.id.fileProperties)


        recyclerView.adapter = musicAdapter
        swipeRefreshLayout = view.findViewById(R.id.musicSwipeRefresh)
        swipeRefreshLayout.setOnRefreshListener(musicAdapter)
        return view
    }

    fun showCount(count: Int) {
        if (mainActivity.mViewPager.currentItem == 2) {
            if (count <= 0) {
                mainActivity.cardViewCount.visibility = View.GONE
                return
            }
            mainActivity.cardViewCount.visibility = View.VISIBLE
            textViewCount.text = count.toString()
        }
    }


    fun updateCardView(cardViewCount: CardView) {

        musicDelete.setOnClickListener { musicAdapter.deleteSelectedMusic() }

        fileProperties.setOnClickListener { v ->
            val popupMenu = PopupMenu(context, v)
            popupMenu.menu.add("Properties").setIcon(R.drawable.ic_info_black_24dp)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Properties" -> musicAdapter.showProperties()
                }
                true
            }
            popupMenu.show()
        }

        btnSend.setOnClickListener {
            if (MainActivity.isConnected) {
                if (MainActivity.isSender) {

                    Thread(Runnable {
                        val sender = TransferHotspot.getSender(mainActivity, false)

                        if (sender != null) {
                            val fileInfos = ArrayList<FileInfo>()
                            val s = musicAdapter.selectedFiles
                            for (i in 0 until musicAdapter.selectedItemsCount) {
                                fileInfos.add(FileInfo(s[i], s[i].name, false))
                            }
                            sender.sendFiles(fileInfos)

                            MainActivity.handler.post {
                                musicAdapter.clearCount()
                                cardViewCount.visibility = View.GONE
                            }
                        }
                    }).start()
                } else {
                    Thread(Runnable {
                        val fileInfos = ArrayList<FileInfo>()
                        val s = musicAdapter.selectedFiles
                        for (i in 0 until musicAdapter.selectedItemsCount) {
                            fileInfos.add(FileInfo(s[i], s[i].name, false))
                        }
                        TransferWifi.getSender(mainActivity).sendFiles(fileInfos)

                        MainActivity.handler.post {
                            musicAdapter.clearCount()
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

        checkBoxSelectAll.isChecked = musicAdapter.selectedItemsCount == musicAdapter.itemCount

        cancelSelection.setOnClickListener(musicAdapter.cancelSelectionClickListener)
        checkBoxSelectAll.setOnClickListener(musicAdapter.selectAllOnClickListener)
        //        checkBoxSelectAll.setOnCheckedChangeListener(musicAdapter.selectAllOnCheckedChangeListener);
        if (musicAdapter.selectedItemsCount > 0) {
            cardViewCount.visibility = View.VISIBLE
            textViewCount.text = musicAdapter.selectedItemsCount.toString()
        } else {
            cardViewCount.visibility = View.GONE
        }
    }

    fun refreshCompleted() {
        swipeRefreshLayout.isRefreshing = false
    }
}// Required empty public constructor