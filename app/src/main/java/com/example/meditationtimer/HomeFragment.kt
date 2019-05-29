package com.example.meditationtimer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import org.json.JSONObject
import java.time.Duration
import java.time.Instant

class HomeFragment : Fragment(), TabLayout.OnTabSelectedListener {
    override fun onTabReselected(tab: TabLayout.Tab?) {
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        updateStatsType(tab!!.text.toString())
    }

    private lateinit var rootView : View

    private fun updateStatsType(type : String) {
        rootView.findViewById<TextView>(R.id.entryTypeView).apply {
            text = type
        }

        // to be called after accessing the database
        // entries is a list with all entries of the type passed in
        val processEntries = { entries : List<Entry> ->
            rootView.findViewById<TextView>(R.id.numEntriesView).apply {
                text = entries.size.toString()
            }

            rootView.findViewById<FrameLayout>(R.id.averageValuesHolder).apply {

                // find which values are numeric and can be processed
                val defaultData = EntryTypes.getConfig(type).defaultData
                val averageValues = JSONObject()

                for (key in defaultData.keys()) {
                    val value = defaultData.get(key).toString()

                    if (value.toDoubleOrNull() != null) {
                        val averageForToday = if (entries.isNotEmpty()) {
                            entries.sumByDouble {
                                it.data.getDouble(key)
                            } / entries.size
                        }
                        else {
                            0.0
                        }

                        averageValues.put("average $key", averageForToday)
                    }
                }

                val averageValuesView = RecordDataView(context, averageValues)

                removeAllViews()
                addView(averageValuesView)
            }
        }

        val startEpochSecond = Instant
            .now()
            .minus(Duration.ofDays(1))
            .epochSecond

        val endEpochSecond = Instant.now().epochSecond

        // updating the statistics view
        Thread {
            val entries = LogEntryDatabase.instance.entryDao()
                .getAllWithinDurationAndType(startEpochSecond, endEpochSecond, type)

            activity!!.runOnUiThread {
                processEntries(entries)
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

        updateStatsType(selectedType)

        return rootView
    }
}