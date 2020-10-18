package com.example.mayur.byteshare.hider


import android.content.Context
import android.os.Bundle
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView

import com.example.mayur.byteshare.R

import java.util.Objects

/**
 * A simple [Fragment] subclass.
 */
class LoginFragment : Fragment() {
    private var PIN: String? = ""
    private var backupQuestion: String? = ""
    private var backupAnswer: String? = ""
    private var hiderActivity: HiderActivity? = null

    interface LoginSuccessful {
        fun loginSuccessful()  // Activity should replace with HiderFragment
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val bundle = arguments
        if (bundle != null) {
            PIN = bundle.getString(HiderActivity.KEY_HIDER_PASSWORD)
            backupQuestion = bundle.getString(HiderActivity.KEY_HIDER_BACKUP_QUESTION)
            backupAnswer = bundle.getString(HiderActivity.KEY_HIDER_BACKUP_QUESTION_ANSWER)
        } else {
            throw NullPointerException("Bundle must not be null, it contains PIN")
        }

        hiderActivity = activity as HiderActivity?
        val view = inflater.inflate(R.layout.hider_fragment_login, container, false)

        val btnForgotPin = view.findViewById<Button>(R.id.btn_forgot_pin)
        val inputMethodManager =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        val editTextPin = view.findViewById<TextInputEditText>(R.id.txt_pin)
        val layoutPin = view.findViewById<TextInputLayout>(R.id.pin_textinput)
        btnForgotPin.setOnClickListener {
            // Start PIN recovery

            val alertDialog = AlertDialog.Builder(requireContext()).create()
            if (alertDialog.window != null) {
                alertDialog.window!!.setBackgroundDrawableResource(R.drawable.background_dialogbox)
                val view1 = inflater.inflate(
                    R.layout.hider_layout_forgot_password,
                    hiderActivity!!.findViewById<View>(R.id.layout_forgot_password) as ViewGroup,
                    false
                )
                val textViewQuestion = view1.findViewById<TextView>(R.id.txtForgotPasswordQuestion)
                textViewQuestion.text = backupQuestion
                val textInputLayout =
                    view1.findViewById<TextInputLayout>(R.id.textInputLayoutForgotPassword)
                val editTextAnswer =
                    view1.findViewById<TextInputEditText>(R.id.editTextForgotPasswordAnswer)

                editTextAnswer.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {

                    }

                    override fun onTextChanged(
                        s: CharSequence,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        textInputLayout.isErrorEnabled = false
                    }

                    override fun afterTextChanged(s: Editable) {

                    }
                })

                val submit = view1.findViewById<Button>(R.id.btn_forgot_password_submit)
                submit.setOnClickListener {
                    val ans = Objects.requireNonNull<Editable>(editTextAnswer.text).toString()
                    if (ans.equals(backupAnswer!!, ignoreCase = true)) {
                        // Give PIN
                        //                                Toast.makeText(getContext(), PIN, Toast.LENGTH_SHORT).show();
                        alertDialog.dismiss()
                        editTextPin.setText(PIN)
                        inputMethodManager.hideSoftInputFromWindow(
                            hiderActivity!!.currentFocus!!.windowToken,
                            0
                        )
                    } else {
                        textInputLayout.error = "Wrong answer"
                        editTextAnswer.requestFocus()
                    }
                }
                alertDialog.setView(view1)
                alertDialog.show()
            }
        }
        editTextPin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                layoutPin.isErrorEnabled = false
            }

            override fun afterTextChanged(s: Editable) {

            }
        })


        val btnLogin = view.findViewById<Button>(R.id.btn_login)
        btnLogin.setOnClickListener {
            val pin = Objects.requireNonNull<Editable>(editTextPin.text).toString()

            if (pin == PIN) {
                // Login successful
                val loginSuccessful = hiderActivity
                loginSuccessful!!.loginSuccessful()
                inputMethodManager.hideSoftInputFromWindow(
                    hiderActivity!!.currentFocus!!.windowToken,
                    0
                )
            } else {
                // Check PIN
                layoutPin.error = "PIN incorrect"
            }
        }



        return view
    }

    companion object {

        fun newInstance(PIN: String, backupQuestion: String, backupAnswer: String): LoginFragment {
            val bundle = Bundle()
            bundle.putString(HiderActivity.KEY_HIDER_PASSWORD, PIN)
            bundle.putString(HiderActivity.KEY_HIDER_BACKUP_QUESTION, backupQuestion)
            bundle.putString(HiderActivity.KEY_HIDER_BACKUP_QUESTION_ANSWER, backupAnswer)
            val loginFragment = LoginFragment()
            loginFragment.arguments = bundle
            return loginFragment
        }
    }

}// Required empty public constructor
