package com.example.mayur.byteshare.connection

import com.example.mayur.byteshare.Constants

import org.json.JSONException
import org.json.JSONObject

import java.io.File

class FileInfo
    (var file: File, val fileName: String, isHiddenFile: Boolean) {
    var jsonObject: JSONObject = JSONObject()

    init {
        if (file.exists()) {
            if (!file.isDirectory) {
                try {
                    jsonObject.put(
                        Constants.TransferConstants.KEY_FILE_TYPE,
                        Constants.TransferConstants.TYPE_FILE
                    )
                    jsonObject.put(Constants.TransferConstants.KEY_FILE_NAME, fileName)
                    jsonObject.put(Constants.TransferConstants.KEY_FILE_LIST, "")
                    jsonObject.put(Constants.TransferConstants.KEY_FILE_SIZE, file!!.length())
                    jsonObject.put(Constants.TransferConstants.KEY_HIDDEN_FILE, isHiddenFile)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            }
            //                else {
            //                    try {
            //                        jsonObject.put(Constants.TransferConstants.KEY_FILE_TYPE, Constants.TransferConstants.TYPE_FOLDER);
            //                        jsonObject.put(Constants.TransferConstants.KEY_FILE_NAME, file.getName());
            //                        if (file.list().length <= 0){
            //                            jsonObject.put(Constants.TransferConstants.KEY_FILE_LIST, "");
            //                            jsonObject.put(Constants.TransferConstants.KEY_FILE_SIZE, com.example.mayur.xportal.util.FileUtils.getFolderSize(file));
            //                            return;
            //                        }
            //                        JSONArray jsonArray = new JSONArray(file.list());
            //                        jsonObject.put(Constants.TransferConstants.KEY_FILE_LIST, jsonArray);
            //                        jsonObject.put(Constants.TransferConstants.KEY_FILE_SIZE, file.length());
            //                    } catch (JSONException e) {
            //                        e.printStackTrace();
            //                    }
            //
            //                }
        }
    }
}
