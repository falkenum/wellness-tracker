package com.example.meditationtimer

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class MainPagerAdapter(fm : androidx.fragment.app.FragmentManager, val timerBinder: TimerService.TimerBinder) :
    androidx.fragment.app.FragmentPagerAdapter(fm) {

    // timer and history
    override fun getCount(): Int = 2

    override fun getItem(position: Int): androidx.fragment.app.Fragment {
        // deciding which layout to use based on the tab position
        return when (position) {
            0 -> TimerFragment().apply {
                arguments = Bundle().apply { putBinder(BundleKeys.TIMER_SERVICE_BINDER, timerBinder) }
            }
            1 -> HistoryFragment()
            else -> throw Exception("getItem fragment error")
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        // deciding the tab title based on position
        return when (position) {
            0 -> "Timer"
            1 -> "History"
            else -> throw Exception("getPageTitle fragment error")
        }
    }
}

