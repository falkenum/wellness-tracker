package com.example.meditationtimer

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter

class MainPagerAdapter(fm : FragmentManager, val timerBinder: TimerService.TimerBinder) :
    FragmentPagerAdapter(fm) {

    // timer and history
    override fun getCount(): Int = 2

    override fun getItem(position: Int): Fragment {
        // deciding which layout to use based on the tab position
        return when (position) {
            0 -> TimerFragment().apply {
                arguments = Bundle().apply { putBinder(MainActivity.ARG_TIMER_SERVICE_BINDER, timerBinder) }
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

