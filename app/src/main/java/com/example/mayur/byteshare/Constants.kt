package com.example.mayur.xportal

import android.os.Environment

import java.io.File

object Constants {

    var XPORTAL_ROOT = File(Environment.getExternalStorageDirectory().toString() + "/XPortal")

    init {
        if (!XPORTAL_ROOT.exists())
            XPORTAL_ROOT.mkdir()
        if (XPORTAL_ROOT.exists() && !XPORTAL_ROOT.isDirectory) {
            XPORTAL_ROOT.delete()
            XPORTAL_ROOT.mkdir()
        }
    }

    object InitialCommunication {
        val COMMUNICATION_PORT = 35446
    }

    object HotspotConstants {
        val KEY =
            "6tr4r56ty33rt1h5rth8rtgfh4GF54556asd4f545erg1uyugJBGHJUKNKNK15645151DF15151D55D15151541S515151F51554ASD51DS254541A5SD151Sfg65h4"
        val TYPE_HOTSPOT = 10
    }

    object WifiConstants {
        val KEY =
            "6tr4r56ty33rt1h5rth8rtgfh4GF54556asd4f545erg1KJGYUGASDBJSDF655454F55515F15FEDd12CD5a1sd5215fd15sd4ASD51DS254541A5SD151Sfg65h4"
        val TYPE_WIFI = 11
    }

    object StyleConstants {
        val COLOR_PRIMARY = "color_primary"
        val COLOR_PRIMARY_DARK = "color_primary_dark"
    }

    object TransferConstants {
        val KEY_HIDDEN_FILE = "hidden_file"
        val KEY_FILE_NAME = "file_name"
        val KEY_FILE_TYPE = "file_type"
        val KEY_FILE_SIZE = "file_size"
        val KEY_FILE_LIST = "file_list"
        val PORT = 25642

        val TYPE_FOLDER = "folder"
        val TYPE_FILE = "file"
    }


}
