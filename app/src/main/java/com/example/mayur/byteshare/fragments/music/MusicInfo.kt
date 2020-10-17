package com.example.mayur.byteshare.fragments.music

import android.net.Uri

class MusicInfo(songname: String, var artistname: String?, var songUrl: Uri?, var size: Long) {
    var songName: String? = null
        private set

    init {
        songName = songname
    }

    fun setSongname(songname: String) {
        songName = songname
    }
}
