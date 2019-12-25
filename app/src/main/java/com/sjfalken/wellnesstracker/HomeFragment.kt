package com.sjfalken.wellnesstracker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*

class HomeFragment : BaseFragment(), ViewPager.OnPageChangeListener {
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
        view.homePager.apply {
            adapter = object : FragmentPagerAdapter(
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

            addOnPageChangeListener(this@HomeFragment)
        }

        view.fab.setOnClickListener {
            (activity!! as MainActivity).navController.navigate(R.id.newEntryFragment)
        }

        view.bottom_navigation.setOnNavigationItemSelectedListener {
            val pos = when (it.itemId) {
                R.id.historyMenuItem -> HISTORY_POS
                R.id.statsMenuItem -> STATS_POS
                else -> throw Exception("shouldn't get here")
            }

            homePager.setCurrentItem(pos, true)

            true
        }
//        view.checkBox.apply {
//            setOnClickListener {
//                Log.d("tag", "test")
//            }
//        }

//        view.checkedTextView.apply{
//            setOnClickListener {
//                toggle()
//            }
//        }

    }

    override fun onPageScrollStateChanged(state: Int) = Unit
    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) = Unit
    override fun onPageSelected(position: Int) {
        bottom_navigation.selectedItemId = when (position) {
            HISTORY_POS -> R.id.historyMenuItem
            STATS_POS -> R.id.statsMenuItem
            else -> throw Exception("shouldn't get here")
        }
    }
}