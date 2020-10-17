package com.example.mayur.byteshare.fragments.photos


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
import com.example.mayur.byteshare.connection.FileInfo
import com.example.mayur.byteshare.connection.hotspot.TransferHotspot
import com.example.mayur.byteshare.connection.wifimanager.TransferWifi
import java.util.*


/**
 * A simple [Fragment] subclass.
 */
class PhotosFragment : Fragment() {
    internal lateinit var view: View
    internal lateinit var photosRecyclerView: RecyclerView
    private lateinit var cancelSelection: ImageButton
    private lateinit var musicDelete: ImageButton
    internal lateinit var fileProperties: ImageButton
    internal lateinit var checkBoxSelectAll: CheckBox
    private lateinit var photosAdapter: PhotosAdapter
    private lateinit var textViewCount: TextView
    private lateinit var btnSend: Button
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
        photosAdapter = PhotosAdapter(this, mainActivity)
        photosRecyclerView.adapter = photosAdapter

        textViewCount = mainActivity.cardViewCount.findViewById(R.id.txtSelectionCount)
        musicDelete = mainActivity.cardViewCount.findViewById(R.id.btnFileDelete)
        btnSend = mainActivity.cardViewCount.findViewById(R.id.btnSend)
        cancelSelection = mainActivity.findViewById(R.id.cancelSelectionImageButton)
        checkBoxSelectAll = mainActivity.findViewById(R.id.selectAllCheckBox)

        fileProperties = mainActivity.cardViewCount.findViewById(R.id.fileProperties)

        swipeRefreshLayout = view.findViewById(R.id.imageSwipeRefresh)
        swipeRefreshLayout.setOnRefreshListener(photosAdapter)
        return view
    }

    fun updateCardView(cardViewCount: CardView) {
        val mainActivity = activity as MainActivity

        musicDelete.setOnClickListener { photosAdapter.deleteSelectedImages() }

        fileProperties.setOnClickListener { v ->
            val popupMenu = PopupMenu(context, v)
            popupMenu.menu.add("Properties").setIcon(R.drawable.ic_info_black_24dp)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Properties" -> photosAdapter.showProperties()
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
                            val s = photosAdapter.selectedFiles
                            for (i in s.indices) {
                                fileInfos.add(FileInfo(s[i], s[i].name, false))
                            }
                            sender.sendFiles(fileInfos)

                            MainActivity.handler.post {
                                photosAdapter.clearCount()
                                cardViewCount.visibility = View.GONE
                            }
                        }
                    }).start()
                } else {
                    Thread(Runnable {
                        val fileInfos = ArrayList<FileInfo>()
                        val s = photosAdapter.selectedFiles
                        for (i in 0 until photosAdapter.selectedItemsCount) {
                            fileInfos.add(FileInfo(s[i], s[i].name, false))
                        }
                        TransferWifi.getSender(mainActivity).sendFiles(fileInfos)

                        MainActivity.handler.post {
                            photosAdapter.clearCount()
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

        checkBoxSelectAll.isChecked = photosAdapter.selectedItemsCount == photosAdapter.itemCount

        cancelSelection.setOnClickListener(photosAdapter.cancelSelectionClickListener)
        checkBoxSelectAll.setOnClickListener(photosAdapter.selectAllOnClickListener)
        //        checkBoxSelectAll.setOnCheckedChangeListener(photosAdapter.selectAllOnCheckedChangeListener);
        if (photosAdapter.selectedItemsCount > 0) {

            cardViewCount.visibility = View.VISIBLE
            textViewCount.text = photosAdapter.selectedItemsCount.toString()
        } else {
            cardViewCount.visibility = View.GONE
        }
    }

    fun showCount(count: Int) {
        val mainActivity = activity as MainActivity?
        if (mainActivity != null && mainActivity.mViewPager.currentItem == 4) {
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