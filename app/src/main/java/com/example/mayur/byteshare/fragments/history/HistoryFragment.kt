package com.example.mayur.byteshare.fragments.history

import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.cardview.widget.CardView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mayur.byteshare.R
import java.util.*


/**
 * A simple [Fragment] subclass.
 */
class HistoryFragment : Fragment() {

    private lateinit var historySendFragment: HistorySendFragment
    private lateinit var historyReceiveFragment: HistoryReceiveFragment
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.history_fragment_history, container, false)
        viewPager = view.findViewById(R.id.viewPagerHistory)
        tabLayout = view.findViewById(R.id.tabLayoutHistory)

        historyReceiveFragment = HistoryReceiveFragment()
        historySendFragment = HistorySendFragment()
        viewPager.adapter = object :
            FragmentPagerAdapter(Objects.requireNonNull<FragmentActivity>(activity).supportFragmentManager) {
            override fun getCount(): Int {
                return 2
            }


            override fun getItem(position: Int): Fragment? {
                return when (position) {
                    0 -> historySendFragment
                    else -> historyReceiveFragment
                }
            }

            override fun getPageTitle(position: Int): CharSequence? {
                when (position) {
                    0 -> return "SEND"
                    1 -> return "RECEIVE"
                }
                return super.getPageTitle(position)
            }
        }
        tabLayout.setupWithViewPager(viewPager)
        viewPager.offscreenPageLimit = 2

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(i: Int, v: Float, i1: Int) {

            }

            override fun onPageSelected(i: Int) {
                when (i) {
                    0 -> {
                        currentPage = 0
                        HistorySendFragment.historyAdapterSender.notifyDataSetChanged()
                    }
                    1 -> {
                        currentPage = 1
                        HistoryReceiveFragment.historyAdapterReceiver.notifyDataSetChanged()
                    }
                }
            }

            override fun onPageScrollStateChanged(i: Int) {

            }
        })
        return view
    }

    fun updateCardView(cardViewCount: CardView) {
        val pos = viewPager.currentItem
        when (pos) {
            0 -> {
                HistorySendFragment.historyAdapterSender.notifyDataSetChanged()
                historySendFragment.updateCardView()
            }
            2 -> {
                HistoryReceiveFragment.historyAdapterReceiver.notifyDataSetChanged()
                historyReceiveFragment.updateCardView()
            }
        }
    }

    companion object {
        var currentPage = 0
    }

}// Required empty public constructor
