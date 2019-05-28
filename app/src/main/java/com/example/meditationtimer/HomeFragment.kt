package com.example.meditationtimer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import java.time.LocalDate

class HomeFragment : Fragment(), TabLayout.OnTabSelectedListener {
    override fun onTabReselected(tab: TabLayout.Tab?) {
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        setStatsType(tab!!.text.toString())
    }

    private lateinit var rootView : View

    private fun setStatsType(type : String) {
        rootView.findViewById<TextView>(R.id.entryTypeView).apply {
            text = type
        }

        // updating the statistics view
        Thread {
            val numEntries = RecordDatabase.instance
                .recordDao()
                .getAll()
                .filter { (it.type == type) &&
                        (it.dateTime.toLocalDate() == LocalDate.now()) }
                .size

            activity!!.runOnUiThread {
                rootView.findViewById<TextView>(R.id.numEntriesView).apply {
                    text = numEntries.toString()
                }
            }
        }.start()

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_home, container, false)

        rootView.findViewById<Button>(R.id.newLogEntryButton).setOnClickListener {
            findNavController().navigate(R.id.newEntryFragment)
        }

        val selectedType = activity!!.findViewById<TabLayout>(R.id.tabLayout).run{
            addOnTabSelectedListener(this@HomeFragment)
            getTabAt(selectedTabPosition)!!.text.toString()
        }

        setStatsType(selectedType)

        return rootView
    }

    companion object SavedInstanceItems {
        const val SELECTED_ITEM_POS = "com.example.meditationtimer.SELECTED_ITEM_POS"
    }
}