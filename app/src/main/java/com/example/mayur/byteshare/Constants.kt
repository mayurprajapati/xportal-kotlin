package com.example.mayur.byteshare

import android.os.Environment

import java.io.File

object Constants {
var EXTRA_FILES = "extra_files"
    var BYTESHARE_ROOT = File(Environment.getExternalStorageDirectory().toString() + "/ByteShare")

    init {
        if (!BYTESHARE_ROOT.exists())
            BYTESHARE_ROOT.mkdir()
        if (BYTESHARE_ROOT.exists() && !BYTESHARE_ROOT.isDirectory) {
            BYTESHARE_ROOT.delete()
            BYTESHARE_ROOT.mkdir()
        }
    }

    object InitialCommunication {
        const val COMMUNICATION_PORT = 35446
    }

    object HotspotConstants {
        const val KEY =
            "6tr4r56ty33rt1h5rth8rtgfh4GF54556asd4f545erg1uyugJBGHJUKNKNK15645151DF15151D55D15151541S515151F51554ASD51DS254541A5SD151Sfg65h4"
        const val TYPE_HOTSPOT = 10
    }

    object WifiConstants {
        const val KEY =
            "6tr4r56ty33rt1h5rth8rtgfh4GF54556asd4f545erg1KJGYUGASDBJSDF655454F55515F15FEDd12CD5a1sd5215fd15sd4ASD51DS254541A5SD151Sfg65h4"
        const val TYPE_WIFI = 11
    }

    object StyleConstants {
        const val COLOR_PRIMARY = "color_primary"
        const val COLOR_PRIMARY_DARK = "color_primary_dark"
    }

    object TransferConstants {
        const val KEY_HIDDEN_FILE = "hidden_file"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_FILE_TYPE = "file_type"
        const val KEY_FILE_SIZE = "file_size"
        const val KEY_FILE_LIST = "file_list"
        const val PORT = 25642
        const val TYPE_FOLDER = "folder"
        const val TYPE_FILE = "file"
    }
}
