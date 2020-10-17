package com.example.mayur.byteshare.hider


import android.content.Context
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.fragment.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import com.example.mayur.byteshare.hider.HiderActivity
import com.example.mayur.byteshare.R
import java.util.*

/**
 * @author mayur
 */
class SetupHiderFragment : Fragment() {
    private var question = ""
    private lateinit var hiderActivity: HiderActivity

    interface OnSetupButtonClicked {
        fun onClick(v: View, pin: String, question: String, answer: String)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        hiderActivity = activity as HiderActivity
        val view = inflater.inflate(R.layout.hider_fragment_setup_hider, container, false)
        val backupQuestion = view.findViewById<Spinner>(R.id.spinner_backup_question)
        val questions = arrayOf(
            "What is name of your best friend?",
            "Name of your favorite team?",
            "What was your childhood nickname?"
        )

        backupQuestion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                question = questions[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val adapter = ArrayAdapter(
            Objects.requireNonNull<Context>(context),
            android.R.layout.simple_spinner_dropdown_item,
            questions
        )
        backupQuestion.adapter = adapter


        val questionAnswer = view.findViewById<TextInputEditText>(R.id.txt_question_answer)
        val pinInputLayout = view.findViewById<TextInputLayout>(R.id.pin_textinput)
        val editTextPIN = view.findViewById<TextInputEditText>(R.id.txt_pin)
        editTextPIN.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if (s.length >= 4) {
                    pinInputLayout.isErrorEnabled = false
                } else {
                    pinInputLayout.error = "PIN must be of at least 4 digits"
                }
            }
        })

        val buttonSetup = view.findViewById<Button>(R.id.btn_setup)
        buttonSetup.setOnClickListener { v ->
            val answer = questionAnswer.text!!.toString()
            if (answer != "" && editTextPIN.text!!.toString().length >= 4) {
                val pin = editTextPIN.text!!.toString()
                val onSetupButtonClicked = hiderActivity
                onSetupButtonClicked.onClick(v, pin, question, answer)
            } else {
                Snackbar.make(v, "Check answer or PIN.", Snackbar.LENGTH_SHORT).show()
            }
        }
        return view
    }
}// Required empty public constructor