package com.example.mayur.xportal.fragments.video


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
class VideosFragment : Fragment() {
    internal lateinit var view: View
    internal lateinit var photosRecyclerView: RecyclerView
    private lateinit var cancelSelection: ImageButton
    private lateinit var musicDelete: ImageButton
    internal lateinit var fileProperties: ImageButton
    internal lateinit var checkBoxSelectAll: CheckBox
    private lateinit var videosAdapter: VideosAdapter
    private lateinit var btnSend: Button
    private lateinit var textViewCount: TextView
    lateinit var swipeRefreshLayout: SwipeRefreshLayout


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val mainActivity = activity as MainActivity
        view = inflater.inflate(R.layout.photos_fragment_photos, container, false)
        photosRecyclerView = view.findViewById(R.id.photosRecyclerView)
        val gridLayoutManager = GridLayoutManager(view.context, 3)
        photosRecyclerView.layoutManager = gridLayoutManager
        videosAdapter = VideosAdapter(this, activity as MainActivity)
        photosRecyclerView.adapter = videosAdapter

        textViewCount = mainActivity.cardViewCount.findViewById(R.id.txtSelectionCount)
        musicDelete = mainActivity.cardViewCount.findViewById(R.id.btnFileDelete)
        btnSend = mainActivity.cardViewCount.findViewById(R.id.btnSend)
        cancelSelection = mainActivity.findViewById(R.id.cancelSelectionImageButton)
        checkBoxSelectAll = mainActivity.findViewById(R.id.selectAllCheckBox)

        fileProperties = mainActivity.cardViewCount.findViewById(R.id.fileProperties)

        swipeRefreshLayout = view.findViewById(R.id.imageSwipeRefresh)
        swipeRefreshLayout.setOnRefreshListener(videosAdapter)
        return view
    }

    fun updateCardView(cardViewCount: CardView) {
        val mainActivity = activity as MainActivity

        musicDelete.setOnClickListener { videosAdapter.deleteSelectedImages() }

        fileProperties.setOnClickListener { v ->
            val popupMenu = PopupMenu(context, v)
            popupMenu.menu.add("Properties").setIcon(R.drawable.ic_info_black_24dp)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Properties" -> videosAdapter.showProperties()
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
                            val s = videosAdapter.selectedFiles
                            for (i in s.indices) {
                                fileInfos.add(FileInfo(s[i], s[i].name, false))
                            }
                            sender.sendFiles(fileInfos)

                            MainActivity.handler.post {
                                videosAdapter.clearCount()
                                cardViewCount.visibility = View.GONE
                            }
                        }
                    }).start()
                } else {
                    Thread(Runnable {
                        val fileInfos = ArrayList<FileInfo>()
                        val s = videosAdapter.selectedFiles
                        for (i in 0 until videosAdapter.selectedItemsCount) {
                            fileInfos.add(FileInfo(s[i], s[i].name, false))
                        }
                        TransferWifi.getSender(mainActivity).sendFiles(fileInfos)

                        MainActivity.handler.post {
                            videosAdapter.clearCount()
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

        checkBoxSelectAll.isChecked = videosAdapter.selectedItemsCount == videosAdapter.itemCount

        cancelSelection.setOnClickListener(videosAdapter.cancelSelectionClickListener)
        checkBoxSelectAll.setOnClickListener(videosAdapter.selectAllOnClickListener)
        //        checkBoxSelectAll.setOnCheckedChangeListener(videosAdapter.selectAllOnCheckedChangeListener);
        if (videosAdapter.selectedItemsCount > 0) {

            cardViewCount.visibility = View.VISIBLE
            textViewCount.text = videosAdapter.selectedItemsCount.toString()
        } else {
            cardViewCount.visibility = View.GONE
        }
    }

    fun showCount(count: Int) {
        val mainActivity = activity as MainActivity?
        if (mainActivity != null && mainActivity.mViewPager.currentItem == 3) {
            if (count <= 0) {
                mainActivity.cardViewCount.visibility = View.GONE
                return
            }
            mainActivity.cardViewCount.visibility = View.VISIBLE
            textViewCount.text = count.toString()
        }
    }

    fun refreshCompleted() {
        swipeRefreshLayout.isRefreshing = false
    }
}// Required empty public constructor