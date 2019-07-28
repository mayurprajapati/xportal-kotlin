package com.example.mayur.xportal.connection.logger

import android.util.Log

import com.example.mayur.xportal.MainActivity

object Logger {
    fun log(data: String, o: Any) {
        Log.v(MainActivity.TAG, data + " " + o.javaClass.simpleName)
        //        new Thread() {
        //            @Override
        //            public void run() {
        //                try {
        //                    File dir = new File(Environment.getExternalStorageDirectory().getPath() + "/XPortal/log");
        //                    if (!dir.exists()) {
        //                        dir.mkdirs();
        //                    }
        //                    File file = new File(dir.getAbsolutePath() + "/log.txt");
        //                    if (!file.exists())
        //                        file.createNewFile();
        //
        //                    FileWriter fileWriter = new FileWriter(file, true);
        //                    PrintWriter printWriter = new PrintWriter(new BufferedWriter(fileWriter));
        //                    printWriter.println(data);
        //
        //                    printWriter.flush();
        //                    printWriter.close();
        //                } catch (Exception e) {
        //                    e.printStackTrace();
        //                }
        //            }
        //        }.start();
    }
}