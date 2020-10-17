package com.example.mayur.byteshare.hider

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.FragmentTransaction
import androidx.appcompat.widget.Toolbar
import android.view.View
import android.view.WindowManager
import com.example.mayur.byteshare.R
import io.multimoon.colorful.CAppCompatActivity

class HiderActivity : CAppCompatActivity(), SetupHiderFragment.OnSetupButtonClicked,
        LoginFragment.LoginSuccessful {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hider_activity_hider)
        val toolbar = findViewById<Toolbar>(R.id.toolbar_hider)
        toolbar.title = "Hider"

        sharedPreferences = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val password = sharedPreferences.getString(KEY_HIDER_PASSWORD, "")

        if (password != null && password == "") {
            //App started first time
            supportFragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.fragments_container_hider, SetupHiderFragment(), "")
                .commitAllowingStateLoss()
        } else {
            val question = sharedPreferences.getString(KEY_HIDER_BACKUP_QUESTION, "")
            val answer = sharedPreferences.getString(KEY_HIDER_BACKUP_QUESTION_ANSWER, "")
            supportFragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(
                    R.id.fragments_container_hider,
                        LoginFragment.newInstance(password, question, answer),
                    ""
                )
                .commitAllowingStateLoss()
        }
    }

    override fun onClick(v: View, pin: String, question: String, answer: String) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY_HIDER_PASSWORD, pin)
        editor.putString(KEY_HIDER_BACKUP_QUESTION_ANSWER, answer.toLowerCase())
        editor.putString(KEY_HIDER_BACKUP_QUESTION, question)
        editor.apply()

        loginSuccessful()
    }

    override fun loginSuccessful() {
        supportFragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.fragments_container_hider, HiderFragment())
            .commitAllowingStateLoss()
    }

    companion object {
        val PREFERENCE_NAME = "hider"
        val KEY_HIDER_PASSWORD = "key_hider_password"
        val KEY_HIDER_BACKUP_QUESTION_ANSWER = "key_hider_backup_question_answer"
        val KEY_HIDER_BACKUP_QUESTION = "key_hider_backup_question"
    }
}
