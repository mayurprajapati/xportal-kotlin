package com.example.mayur.xportal.fragments.history

import android.annotation.SuppressLint
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.example.mayur.xportal.MainActivity
import com.example.mayur.xportal.R
import com.example.mayur.xportal.fragments.files.icon.Icons
import com.example.mayur.xportal.fragments.history.database.ReceiverHistory
import com.example.mayur.xportal.util.FileUtils
import java.util.*

class HistoryAdapterReceiver(
    private val mainActivity: MainActivity,
    private val historyReceiveFragment: HistoryReceiveFragment
) : RecyclerView.Adapter<HistoryAdapterReceiver.ReceiverHistoryViewHolder>() {
    var recyclerView: RecyclerView
    val historyInfoList: MutableList<HistoryInfo>
    private val linearLayoutManager: LinearLayoutManager?
    private val receiverHistory: ReceiverHistory


    init {
        historyInfoList = ArrayList()
        this.recyclerView = historyReceiveFragment.recyclerView
        linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager?
        receiverHistory = ReceiverHistory(mainActivity)

        Thread(Runnable {
            val cursor = receiverHistory.allData
            if (cursor.count <= 0) {
                //No History is there to show
            } else
                while (cursor.moveToNext()) {
                    val fileName =
                        cursor.getString(cursor.getColumnIndex(ReceiverHistory.COL_FILE_NAME))
                    val fileSize = java.lang.Long.parseLong(
                        cursor.getString(
                            cursor.getColumnIndex(ReceiverHistory.COL_FILE_SIZE)
                        )
                    )
                    cursor.getString(cursor.getColumnIndex(ReceiverHistory.COL_ABS_PATH))
                    val date = java.lang.Long.parseLong(
                        cursor.getString(
                            cursor.getColumnIndex(ReceiverHistory.COL_DATE)
                        )
                    )
                    historyInfoList.add(HistoryInfo(100, fileName, fileSize).setDate(date))
                }

            historyInfoList.sortWith(Comparator { o1, o2 -> Date(o1.date).compareTo(Date(o2.date)) })
        }).start()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiverHistoryViewHolder {
        return ReceiverHistoryViewHolder(
            mainActivity.layoutInflater.inflate(
                R.layout.history_layout_item_history,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ReceiverHistoryViewHolder, position: Int) {
        val fileName = historyInfoList[position].fileName
        holder.fileName.text = fileName
        val historyInfo = historyInfoList[position]
        val progress = historyInfoList[position].progress

        Icons.setIcon(fileName, null, holder.historyIcon, false)
        val totalSize = FileUtils.formatFileSize(historyInfo.totalSize)
        if (progress >= 100) {
            holder.size.text = totalSize
            holder.progressBar.visibility = View.GONE
            holder.progress.visibility = View.GONE
        } else {
            val written = FileUtils.formatFileSize(historyInfo.sentSize)
            holder.size.text = "$written/$totalSize"
            holder.progress.visibility = View.VISIBLE
            holder.progressBar.visibility = View.VISIBLE
            holder.progressBar.progress = progress
            holder.progress.text = "${historyInfo.progress}%"
        }
    }

    override fun getItemCount(): Int {
        return historyInfoList.size
    }

    fun scrollTo(pos: Int) {

        MainActivity.handler.postDelayed({
            if (linearLayoutManager != null && linearLayoutManager.findLastVisibleItemPosition() <= 10) {
                recyclerView.smoothScrollToPosition(pos)
            }
        }, 100)
    }

    fun itemInserted(pos: Int) {
        MainActivity.handler.post {
            try {
                notifyItemInserted(pos)
                scrollTo(pos)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateDatabase(info: HistoryInfo) {
        receiverHistory.insertData(
            info.fileName,
            info.totalSize.toString(),
            info.date.toString()
        )
    }

    fun clearHistory() {
        historyInfoList.clear()
        notifyDataSetChanged()
        receiverHistory.clear()
    }


    class ReceiverHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var progressBar: ProgressBar = itemView.findViewById(R.id.progressBarHistory)
        var progress: TextView = itemView.findViewById(R.id.progressBarProgress)
        var fileName: TextView = itemView.findViewById(R.id.historyName)
        var size: TextView = itemView.findViewById(R.id.historySize)
        var historyIcon: ImageView = itemView.findViewById(R.id.historyIcon)

    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var historyAdapterReceiver: HistoryAdapterReceiver? = null

        fun newInstance(
            mainActivity: MainActivity?,
            historyReceiveFragment: HistoryReceiveFragment?
        ): HistoryAdapterReceiver? {

            if ((mainActivity == null || historyReceiveFragment == null) && historyAdapterReceiver == null)
                return null

            if (historyAdapterReceiver == null) {
                historyAdapterReceiver =
                        HistoryAdapterReceiver(mainActivity!!, historyReceiveFragment!!)
            }
            return historyAdapterReceiver
        }
    }
}