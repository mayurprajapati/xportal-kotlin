package com.example.mayur.byteshare.fragments.music

import android.app.AlertDialog
import android.content.DialogInterface
import android.net.Uri
import android.provider.MediaStore
import com.google.android.material.snackbar.Snackbar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.mayur.byteshare.MainActivity
import com.example.mayur.byteshare.utils.FileUtils
import com.example.mayur.byteshare.R
import java.io.File
import java.text.DateFormat
import java.util.*

class MusicAdapter internal constructor(mainActivity: MainActivity, musicFragment: MusicFragment) :
    RecyclerView.Adapter<MusicAdapter.SongHolder>(), SwipeRefreshLayout.OnRefreshListener {

    var cancelSelectionClickListener: View.OnClickListener
    var selectAllOnClickListener: View.OnClickListener
    var selectAllOnCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null
    private var musicInfoArrayList: ArrayList<MusicInfo>? = null
    private val selectedMusic: MutableList<Int>
    private var mainActivity: MainActivity? = null
    private var musicFragment: MusicFragment? = null

    val selectedItemsCount: Int
        get() = selectedMusic.size

    val selectedFiles: List<File>
        get() {
            val stringBuilder = StringBuilder()
            val temp = ArrayList<File>()
            for (i in selectedMusic.indices) {
                val f = File(musicInfoArrayList!![selectedMusic[i]].songUrl!!.toString())
                if (f.exists())
                    temp.add(f)
                else
                    stringBuilder.append(f.name).append("\n")
            }

            if (stringBuilder.isNotEmpty()) {
                stringBuilder.append("File(s) doesn't exists.")
                MainActivity.handler.post {
                    Toast.makeText(
                        mainActivity,
                        stringBuilder,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            return temp
        }


    init {
        Thread(Runnable {
            musicInfoArrayList = ArrayList()
            loadSongs()
            MainActivity.handler.post { notifyDataSetChanged() }
        }).start()

        selectedMusic = ArrayList()
        this@MusicAdapter.mainActivity = mainActivity

        this@MusicAdapter.musicFragment = musicFragment

        cancelSelectionClickListener = View.OnClickListener {
            selectedMusic.clear()
            notifyDataSetChanged()
            musicFragment.showCount(0)
            musicFragment.checkBoxSelectAll.isChecked = false
        }

        selectAllOnClickListener = View.OnClickListener {
            if (musicFragment.checkBoxSelectAll.isChecked) {
                selectedMusic.clear()
                for (i in musicInfoArrayList!!.indices)
                    selectedMusic.add(i)
                notifyDataSetChanged()
                musicFragment.showCount(selectedMusic.size)
            } else {
                selectedMusic.clear()
                musicFragment.showCount(selectedMusic.size)
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, pos: Int): SongHolder {
        val myView = LayoutInflater.from(mainActivity)
            .inflate(R.layout.music_layout_item_songs, viewGroup, false)
        val songHolder = SongHolder(myView)


        songHolder.cardView.setOnLongClickListener {
            val f = File(musicInfoArrayList!![songHolder.adapterPosition].songUrl!!.toString())
            val alertDialog = AlertDialog.Builder(mainActivity).create()
            if (alertDialog.window != null)
                alertDialog.window!!.setBackgroundDrawableResource(R.drawable.background_dialogbox)
            val view = LayoutInflater.from(mainActivity).inflate(
                R.layout.files_layout_file_properties,
                mainActivity!!.findViewById<View>(R.id.filePropertiesRoot) as ViewGroup
            )
            alertDialog.setTitle("Properties")
            val date = Date(f.lastModified())
            (view.findViewById<View>(R.id.filePropertiesFileName) as TextView).text = f.name
            (view.findViewById<View>(R.id.filePropertiesFilePath) as TextView).text = f.absolutePath
            var dateFormat = DateFormat.getDateInstance().format(date)
            dateFormat += "\n" + DateFormat.getTimeInstance().format(date)
            (view.findViewById<View>(R.id.filePropertiesDateModified) as TextView).text = dateFormat
            val fileSize = view.findViewById<TextView>(R.id.filePropertiesFileSize)
            fileSize.text = FileUtils.formatFileSize(f.length())
            alertDialog.setView(view)
            alertDialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                "OKAY"
            ) { dialog, _ -> dialog.dismiss() }
            alertDialog.show()
            true
        }

        songHolder.cardView.setOnClickListener(View.OnClickListener {
            val pos: Int = songHolder.adapterPosition
            if (selectedMusic.size > 0) {
                if (selectedMusic.contains(pos)) {
                    selectedMusic.remove(Integer.valueOf(pos))
                    songHolder.checkBox.isChecked = false
                    songHolder.selected.visibility = View.GONE
                    return@OnClickListener
                }
                songHolder.checkBox.isChecked = true
                if (!selectedMusic.contains(pos))
                    selectedMusic.add(songHolder.layoutPosition)
                songHolder.selected.visibility = View.VISIBLE
                return@OnClickListener
            }
            val musicInfo = musicInfoArrayList!![pos]
            val file = File(musicInfo.songUrl.toString())
            FileUtils.fireIntent(file, mainActivity!!)
        })

        songHolder.checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                if (!selectedMusic.contains(songHolder.adapterPosition))
                    selectedMusic.add(songHolder.layoutPosition)
                songHolder.selected.visibility = View.VISIBLE
            } else {
                songHolder.selected.visibility = View.GONE
                selectedMusic.remove(Integer.valueOf(songHolder.layoutPosition))
            }

            if (selectedMusic.size == musicInfoArrayList!!.size) {
                musicFragment!!.checkBoxSelectAll.isChecked = true
            }

            if (selectedMusic.size == musicInfoArrayList!!.size - 1)
                musicFragment!!.checkBoxSelectAll.isChecked = false
            musicFragment!!.showCount(selectedMusic.size)
        }
        return songHolder
    }


    fun showProperties() {

        val alertDialog = AlertDialog.Builder(mainActivity).create()
        if (alertDialog.window != null)
            alertDialog.window!!.setBackgroundDrawableResource(R.drawable.background_dialogbox)
        val dialogView = LayoutInflater.from(mainActivity).inflate(
            R.layout.files_layout_file_properties,
            mainActivity!!.findViewById<View>(R.id.filePropertiesRoot) as ViewGroup
        )
        dialogView.findViewById<View>(R.id.textViewPropertiesLastModified).visibility = View.GONE
        dialogView.findViewById<View>(R.id.textViewPropertiesPath).visibility = View.GONE
        dialogView.findViewById<View>(R.id.filePropertiesFilePath).visibility = View.GONE
        dialogView.findViewById<View>(R.id.filePropertiesDateModified).visibility = View.GONE


        Thread(Runnable {
            val fileNames = StringBuilder()
            var len: Long = 0
            val selectedMusic = getSelectedMusic()
            for (file in selectedMusic) {
                fileNames.append(file.name).append("\n")
                len += file.length()
            }
            val finalLen = len
            MainActivity.handler.post {
                val fileSize = dialogView.findViewById<TextView>(R.id.filePropertiesFileSize)

                fileSize.text = FileUtils.formatFileSize(finalLen)
                alertDialog.setTitle("Properties")
                (dialogView.findViewById<View>(R.id.filePropertiesFileName) as TextView).text =
                        fileNames
                alertDialog.setView(dialogView)

                alertDialog.setButton(
                    DialogInterface.BUTTON_POSITIVE,
                    "OKAY"
                ) { dialog, _ -> dialog.dismiss() }

                alertDialog.show()
            }
        }).start()


    }

    override fun onBindViewHolder(songHolder: SongHolder, pos: Int) {
        val s = musicInfoArrayList!![pos]
        songHolder.tvSongName.text = s.songName
        songHolder.tvSongArtist.text = s.artistname
        songHolder.tvSize.text = FileUtils.formatFileSize(s.size)

        songHolder.checkBox.isChecked = selectedMusic.contains(pos)
    }

    override fun getItemCount(): Int {
        return musicInfoArrayList!!.size
    }

    fun clearCount() {
        selectedMusic.clear()
        notifyDataSetChanged()
        musicFragment!!.showCount(0)
    }

    override fun onRefresh() {
        musicFragment!!.swipeRefreshLayout.isRefreshing = true
        Thread(Runnable {
            loadSongs()
            MainActivity.handler.postDelayed({
                notifyDataSetChanged()
                musicFragment!!.refreshCompleted()
            }, 1000)
        }).start()
    }

    private fun loadSongs() {
        musicInfoArrayList = ArrayList()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = MediaStore.Audio.Media.IS_MUSIC + "!=0"
        val cursor = mainActivity!!.contentResolver.query(
            uri, null,
            selection, null,
            MediaStore.Audio.Media.DISPLAY_NAME + " ASC"
        )
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val name =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME))
                    val artist =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                    val fileUrl =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))

                    val f = File(Uri.parse(fileUrl).toString())
                    if (f.exists()) {
                        val s = MusicInfo(name, artist, Uri.parse(fileUrl), f.length())
                        musicInfoArrayList!!.add(s)
                    }

                } while (cursor.moveToNext())
            }

            cursor.close()
            //            sortMusic();
        }
    }

    fun deleteSelectedMusic() {
        val alertDialog = AlertDialog.Builder(mainActivity).create()
        alertDialog.setTitle("Are you sure?\nFollowing songs will be deleted.")
        val stringBuilder = StringBuilder()
        for (file in getSelectedMusic()) {
            stringBuilder.append(file.name).append("\n")
        }
        alertDialog.setMessage(stringBuilder)
        if (alertDialog.window != null) {
            alertDialog.window!!.setBackgroundDrawableResource(R.drawable.background_dialogbox)
        }
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Delete") { dialog, _ ->
            var deleted = true
            dialog.dismiss()
            for (i in 0 until getSelectedMusic().size) {
                val f = getSelectedMusic()[i]
                if (!f.delete()) {
                    deleted = false
                }
                if (deleted) {
                    Snackbar.make(
                        mainActivity!!.mViewPager,
                        "Song(s) deleted successfully.",
                            1
                    ).show()
                } else
                    Snackbar.make(
                        mainActivity!!.mViewPager,
                        "Couldn't delete song(s).",
                        1
                    ).show()
            }

            clearCount()
            onRefresh()
        }

        alertDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE,
            "Cancel"
        ) { dialog, which -> dialog.dismiss() }

        alertDialog.show()
    }

    private fun getSelectedMusic(): List<File> {
        val selectedFiles = ArrayList<File>()
        for (i in selectedMusic.indices) {
            println(musicInfoArrayList!![selectedMusic[i]].songUrl!!.toString())
            selectedFiles.add(File(musicInfoArrayList!![selectedMusic[i]].songUrl.toString()))
        }
        return selectedFiles
    }


    class SongHolder//        Button btnAction;
        (itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvSongName: TextView = itemView.findViewById(R.id.tvSongName)
        var tvSongArtist: TextView = itemView.findViewById(R.id.tvArtistName)
        var tvSize: TextView = itemView.findViewById(R.id.musicSongSize)
        var cardView: CardView = itemView.findViewById(R.id.cardLayout)
        var checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
        var selected: ImageView = itemView.findViewById(R.id.musicItemSelected)

    }
}
