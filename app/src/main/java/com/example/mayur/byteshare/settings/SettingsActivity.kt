package com.example.mayur.xportal.settings

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.CardView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import com.example.mayur.xportal.AppStyle
import com.example.mayur.xportal.MainActivity
import com.example.mayur.xportal.R
import com.example.mayur.xportal.connection.logger.Logger
import com.example.mayur.xportal.fragments.history.HistoryAdapterReceiver
import com.example.mayur.xportal.fragments.history.HistoryAdapterSender
import io.multimoon.colorful.CAppCompatActivity

class SettingsActivity : CAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity_settings)

        findViewById<View>(R.id.cardViewThemeChange).setOnClickListener {
            onClickThemeChangeButton()
        }

        findViewById<View>(R.id.cardViewClearHistory).setOnClickListener { v ->
            onClickOfClearHistory()
        }

        val cardView = findViewById<CardView>(R.id.btn_color_change)
        cardView.setCardBackgroundColor(Color.parseColor(AppStyle.currentThemeColor))
        cardView.setOnClickListener { onClickThemeChangeButton() }
    }


    private fun onClickThemeChangeButton() {
        val alertDialog = AlertDialog.Builder(this@SettingsActivity).create()
        val dialogView = LayoutInflater.from(this).inflate(
            R.layout.settings_theme_colors,
            findViewById<View>(R.id.colorsLayoutRoot) as ViewGroup
        )
        val onClickListener = View.OnClickListener { v -> changeColor(v) }
        alertDialog.setView(dialogView)
        if (alertDialog.window != null)
            alertDialog.window!!.setBackgroundDrawableResource(R.drawable.background_dialogbox)
        dialogView.findViewById<View>(R.id.theme_color_deep_orange)
            .setOnClickListener(onClickListener)
        dialogView.findViewById<View>(R.id.theme_color_green).setOnClickListener(onClickListener)
        dialogView.findViewById<View>(R.id.theme_color_grey).setOnClickListener(onClickListener)
        dialogView.findViewById<View>(R.id.theme_color_light_blue)
            .setOnClickListener(onClickListener)
        dialogView.findViewById<View>(R.id.theme_color_pink).setOnClickListener(onClickListener)
        dialogView.findViewById<View>(R.id.theme_color_yellow).setOnClickListener(onClickListener)
        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE,
            "OKAY"
        ) { dialog, _ -> dialog.dismiss() }
        alertDialog.show()
    }


    private fun changeColor(view: View) {
        val sharedPreferences = getSharedPreferences(AppStyle.PREFERENCE_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        when (view.id) {
            R.id.theme_color_deep_orange -> {
                Logger.log(AppStyle.ColorValues.DEEP_ORANGE, this)
                editor.putString(AppStyle.THEME_COLOR_KEY, AppStyle.ColorValues.DEEP_ORANGE)
            }
            R.id.theme_color_green -> {
                Logger.log(AppStyle.ColorValues.DEEP_ORANGE, this)
                editor.putString(AppStyle.THEME_COLOR_KEY, AppStyle.ColorValues.GREEN)
            }
            R.id.theme_color_light_blue -> {
                Logger.log(AppStyle.ColorValues.DEEP_ORANGE, this)
                editor.putString(AppStyle.THEME_COLOR_KEY, AppStyle.ColorValues.LIGHT_BLUE)
            }
            R.id.theme_color_pink -> {
                Logger.log(AppStyle.ColorValues.DEEP_ORANGE, this)
                editor.putString(AppStyle.THEME_COLOR_KEY, AppStyle.ColorValues.PINK)
            }
            R.id.theme_color_grey -> {
                Logger.log(AppStyle.ColorValues.DEEP_ORANGE, this)
                editor.putString(AppStyle.THEME_COLOR_KEY, AppStyle.ColorValues.GREY)
            }
            R.id.theme_color_yellow -> {
                Logger.log(AppStyle.ColorValues.DEEP_ORANGE, this)
                editor.putString(AppStyle.THEME_COLOR_KEY, AppStyle.ColorValues.BLACK)
            }
        }
        editor.apply()

        Toast.makeText(this, "Changes will take effect on next restart", Toast.LENGTH_LONG).show()
    }

    private fun onClickOfClearHistory() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Are you sure?, You want to clear history?")
        builder.setPositiveButton("CLEAR") { dialog, _ ->
            val historyAdapterSender = HistoryAdapterSender.newInstance(parent as MainActivity, null)
            val historyAdapterReceiver = HistoryAdapterReceiver.newInstance(null, null)

            historyAdapterReceiver?.clearHistory()
            historyAdapterSender.clearHistory()
            dialog.dismiss()
        }

        builder.setNegativeButton("CANCEL") { dialog, which -> dialog.dismiss() }.show()
    }
}
