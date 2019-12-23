package com.sjfalken.wellnesstracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*

class HomeFragment : BaseFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    companion object {
        const val HISTORY_POS = 0
        const val STATS_POS = 1
    }

    var currentPosition = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.homePager.adapter = object : FragmentPagerAdapter(
            childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getItem(position: Int): Fragment {
                return when (position) {
                    HISTORY_POS -> HistoryFragment()
                    STATS_POS -> StatsFragment()
                    else -> throw Exception("shouldn't get here")
                }
            }

            override fun getCount(): Int = 2
        }
    }
}