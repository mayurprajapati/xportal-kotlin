package com.example.mayur.byteshare.connection.connection


import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.example.mayur.byteshare.MainActivity
import com.example.mayur.byteshare.R

import java.util.Objects

/**
 * A simple [Fragment] subclass.
 */
class RootFragment : Fragment() {
    private var currentFragment: Fragment? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        if (MainActivity.isConnected) {
            val connectionInfoFragment = ConnectionInfoFragment.newInstance("")
            if (currentFragment !is ConnectionInfoFragment) {
                Objects.requireNonNull<FragmentActivity>(activity).supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.connectionFragmentContainer, connectionInfoFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commitAllowingStateLoss()
            }
            currentFragment = connectionInfoFragment
        } else {
            val connectionFragment = ConnectionFragment()
            if (currentFragment !is ConnectionFragment) {
                Objects.requireNonNull<FragmentActivity>(activity).supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.connectionFragmentContainer, connectionFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commitAllowingStateLoss()
            }
            currentFragment = connectionFragment
        }
        return inflater.inflate(R.layout._fragment_root, container, false)
    }

    fun connectionSuccess(deviceName: String) {
        val connectionInfoFragment = ConnectionInfoFragment.newInstance(deviceName)
        if (currentFragment !is ConnectionInfoFragment) {
            Objects.requireNonNull<FragmentActivity>(activity).supportFragmentManager
                .beginTransaction()
                .replace(R.id.connectionFragmentContainer, connectionInfoFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commitAllowingStateLoss()
        }
        currentFragment = connectionInfoFragment
    }

    fun connectionTerminated() {
        val connectionFragment = ConnectionFragment()
        if (currentFragment !is ConnectionFragment) {
            Objects.requireNonNull<FragmentActivity>(activity).supportFragmentManager
                .beginTransaction()
                .replace(R.id.connectionFragmentContainer, connectionFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commitAllowingStateLoss()
        }
        currentFragment = connectionFragment
    }

}