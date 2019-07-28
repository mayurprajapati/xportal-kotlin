package com.example.mayur.xportal.fragments.files


import android.os.Bundle
import android.os.Environment
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
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
import com.example.mayur.xportal.hider.Hider
import java.io.File
import java.util.*


/**
 * A simple [Fragment] subclass.
 */

class FilesFragment : Fragment() {

    lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var cancelSelection: ImageButton
    internal lateinit var checkBoxSelectAll: CheckBox
    private val currentPathStack: Stack<View> = Stack()
    private lateinit var recyclerView: RecyclerView
    private lateinit var horizontalScrollView: HorizontalScrollView
    private lateinit var mainActivity: MainActivity
    private lateinit var filesAdapter: FilesAdapter
    private lateinit var fileProperties: ImageButton
    private lateinit var fileDelete: ImageButton
    private lateinit var textViewCount: TextView
    private lateinit var btnSend: Button
    private lateinit var currentPath: LinearLayout
    private lateinit var noFilesToShow: View

    init {
        // Required empty public constructor
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mainActivity = activity as MainActivity
        val view = inflater.inflate(R.layout.files_fragment_files, container, false)
        noFilesToShow = view.findViewById(R.id.noFilesFound)
        recyclerView = view.findViewById(R.id.recyclerViewApps)
        filesAdapter = FilesAdapter(mainActivity, this)

        textViewCount = mainActivity.cardViewCount.findViewById(R.id.txtSelectionCount)

        fileProperties = mainActivity.cardViewCount.findViewById(R.id.fileProperties)
        btnSend = mainActivity.cardViewCount.findViewById(R.id.btnSend)
        cancelSelection = mainActivity.findViewById(R.id.cancelSelectionImageButton)
        checkBoxSelectAll = mainActivity.findViewById(R.id.selectAllCheckBox)
        filesAdapter.setRecyclerView(recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = filesAdapter
        recyclerView.setHasFixedSize(true)
        recyclerView.setRecycledViewPool(RecyclerView.RecycledViewPool())

        fileDelete = mainActivity.cardViewCount.findViewById(R.id.btnFileDelete)
        horizontalScrollView = view.findViewById(R.id.filesHorizontalScrollView)
        currentPath = view.findViewById(R.id.filesCurrentPath)
        val currentPathView =
            inflater.inflate(R.layout.files_current_path_button, currentPath, false)
        val button = currentPathView.findViewById<Button>(R.id.btnCurrentPath)
        button.tag = Environment.getExternalStorageDirectory()
        button.text = "sdcard"
        button.setOnClickListener { filesAdapter.onCurrentPathButtonClicked(Environment.getExternalStorageDirectory()) }
        currentPath.addView(currentPathView)

        swipeRefreshLayout = view.findViewById(R.id.filesSwipeRefresh)
        swipeRefreshLayout.setOnRefreshListener(filesAdapter)

        return view
    }


    fun refreshCompleted() {
        swipeRefreshLayout.isRefreshing = false
    }

    fun showCount(count: Int) {
        if (mainActivity.mViewPager.currentItem == 5) {
            if (count <= 0) {
                mainActivity.cardViewCount.visibility = View.GONE
                return
            }
            mainActivity.cardViewCount.visibility = View.VISIBLE
            textViewCount.text = count.toString()
        }
    }


    fun onBackPressed(): Boolean {
        if (currentPathStack.size != 0) {
            currentPath.removeView(currentPathStack.pop())
        }
        return filesAdapter.onBackPressed()
    }

    fun updateCardView(cardViewCount: CardView) {
        btnSend.setOnClickListener {
            if (MainActivity.isConnected) {
                if (MainActivity.isSender) {
                    val sender = TransferHotspot.getSender(mainActivity, false)
                    val s = filesAdapter.getAllSelectedFiles()
                    if (sender != null) {
                        val fileInfos = ArrayList<FileInfo>()
                        for (i in 0 until filesAdapter.selectedItemsCount) {
                            fileInfos.add(FileInfo(s[i], s[i].name, false))
                        }
                        sender.sendFiles(fileInfos)

                        MainActivity.handler.post {
                            MainActivity.handler.post {
                                filesAdapter.clearCount()
                                cardViewCount.visibility = View.GONE
                            }
                        }
                    }
                } else {

                    Thread(Runnable {
                        val s = filesAdapter.getAllSelectedFiles()
                        val fileInfos = ArrayList<FileInfo>()
                        for (i in 0 until filesAdapter.selectedItemsCount) {
                            fileInfos.add(FileInfo(s[i], s[i].name, false))
                        }
                        TransferWifi.getSender(mainActivity).sendFiles(fileInfos)


                        MainActivity.handler.post {
                            filesAdapter.clearCount()
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
        cancelSelection.setOnClickListener(filesAdapter.cancelSelectionClickListener)
        checkBoxSelectAll.setOnClickListener(filesAdapter.selectAllOnClickListener)
        checkBoxSelectAll.isChecked = filesAdapter.selectedItemsCount == filesAdapter.itemCount
        if (filesAdapter.selectedItemsCount > 0) {
            cardViewCount.visibility = View.VISIBLE
            textViewCount.text = filesAdapter.selectedItemsCount.toString()
        } else {
            cardViewCount.visibility = View.GONE
        }

        fileDelete.setOnClickListener {
            println("Listener set...")
            filesAdapter.deleteSelectedFiles()
        }

        fileProperties.setOnClickListener { v ->
            val popupMenu = PopupMenu(context, v)
            popupMenu.menu.add("Properties").setIcon(R.drawable.ic_info_black_24dp)
            popupMenu.menu.add("Hide")
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Properties" -> filesAdapter.showProperties()
                    "Hide" -> {
                        Hider.getHider(mainActivity).encrypt(filesAdapter.getAllSelectedFiles())
                        filesAdapter.clearCount()
                        cardViewCount.visibility = View.GONE
                    }
                }
                true
            }
            popupMenu.show()
        }
    }

    fun updateCurrentPath(file: File) {
        //Add buttons in stack...
        val view = LayoutInflater.from(mainActivity)
            .inflate(R.layout.files_current_path_button, currentPath, false)
        val button = view.findViewById<Button>(R.id.btnCurrentPath)
        button.text = file.name
        button.setOnClickListener { v ->
            val current = v.tag as File
            filesAdapter.onCurrentPathButtonClicked(current)
        }

        button.tag = file
        currentPathStack.push(view)
        currentPath.addView(view)
        MainActivity.handler.postDelayed(
            { horizontalScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT) },
            100
        )
    }

    fun filesAvailable(available: Int) {
        MainActivity.handler.post {
            noFilesToShow.visibility = if (available > 0) View.GONE else View.VISIBLE
        }
    }
}