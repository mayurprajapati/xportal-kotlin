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
import com.example.mayur.xportal.fragments.history.database.SenderHistory
import com.example.mayur.xportal.util.FileUtils
import java.util.*

class HistoryAdapterSender private constructor(
    private val mainActivity: MainActivity,
    historySendFragment: HistorySendFragment
) : RecyclerView.Adapter<HistoryAdapterSender.SenderHistoryViewHolder>() {
    var recyclerView: RecyclerView
    private val senderHistory: SenderHistory
    val historyInfoList: MutableList<HistoryInfo>
    private val linearLayoutManager: LinearLayoutManager?


    init {
        historyInfoList = ArrayList()
        senderHistory = SenderHistory(mainActivity)
        this.recyclerView = historySendFragment.recyclerView
        linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager?

        Thread(Runnable {
            val cursor = senderHistory.allData
            if (cursor.count <= 0) {
                //No History is there to show
            } else {
                while (cursor.moveToNext()) {
                    val fileName =
                        cursor.getString(cursor.getColumnIndex(SenderHistory.COL_FILE_NAME))
                    val fileSize = java.lang.Long.parseLong(
                        cursor.getString(
                            cursor.getColumnIndex(SenderHistory.COL_FILE_SIZE)
                        )
                    )
                    cursor.getString(cursor.getColumnIndex(SenderHistory.COL_ABS_PATH))
                    val date = java.lang.Long.parseLong(
                        cursor.getString(
                            cursor.getColumnIndex(SenderHistory.COL_DATE)
                        )
                    )
                    historyInfoList.add(HistoryInfo(100, fileName, fileSize).setDate(date))
                }

                historyInfoList.sortWith(Comparator { o1, o2 -> Date(o1.date).compareTo(Date(o2.date)) })

                MainActivity.handler.post { notifyDataSetChanged() }
            }
        }).start()

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SenderHistoryViewHolder {
        return SenderHistoryViewHolder(
            mainActivity.layoutInflater.inflate(
                R.layout.history_layout_item_history,
                parent,
                false
            )
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SenderHistoryViewHolder, position: Int) {
        val fileName = historyInfoList[position].fileName
        holder.fileName.text = fileName
        val historyInfo = historyInfoList[position]
        val progress = historyInfoList[position].progress
        holder.size.text = FileUtils.formatFileSize(historyInfo.totalSize)


        Icons.setIcon(fileName, null, holder.historyIcon, false)
        val totalSize = FileUtils.formatFileSize(historyInfo.totalSize)

        //        if (progress == 0){
        //            holder.waiting.setVisibility(View.VISIBLE);
        //        }
        //        else{
        //            holder.waiting.setVisibility(View.GONE);
        //        }

        if (progress >= 100 || progress <= -1) {
            holder.size.text = totalSize
            holder.progress.visibility = View.GONE
            holder.progressBar.visibility = View.GONE
        } else {
            val written = FileUtils.formatFileSize(historyInfo.sentSize)
            holder.size.text = "$written/$totalSize"
            holder.progressBar.visibility = View.VISIBLE
            holder.progress.visibility = View.VISIBLE
            holder.progressBar.progress = progress
            holder.progress.text = "${historyInfo.progress} %"
        }

    }

    override fun getItemCount(): Int {
        return historyInfoList.size
    }

    fun updateDatabase(info: HistoryInfo) {
        senderHistory.insertData(info.fileName, info.totalSize.toString(), info.date.toString())
    }

    fun clearHistory() {
        historyInfoList.clear()
        notifyDataSetChanged()
        senderHistory.clear()
    }

    inner class SenderHistoryViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        var progressBar: ProgressBar = itemView.findViewById(R.id.progressBarHistory)
        var progress: TextView = itemView.findViewById(R.id.progressBarProgress)
        var fileName: TextView = itemView.findViewById(R.id.historyName)
        var size: TextView = itemView.findViewById(R.id.historySize)
        var waiting: TextView = itemView.findViewById(R.id.waiting)
        var historyIcon: ImageView = itemView.findViewById(R.id.historyIcon)

    }

    companion object {

        @SuppressLint("StaticFieldLeak")
        private var historyAdapterSender: HistoryAdapterSender? = null

        fun newInstance(
            mainActivity: MainActivity,
            historySendFragment: HistorySendFragment?
        ): HistoryAdapterSender {
            if (historyAdapterSender == null) {
                historyAdapterSender = HistoryAdapterSender(mainActivity, historySendFragment!!)
            }
            return historyAdapterSender as HistoryAdapterSender
        }
    }
}