package com.sjfalken.wellnesstracker

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.android.synthetic.main.fragment_stats.view.*
import org.json.JSONObject
import java.time.Duration
import java.time.Instant

class StatsFragment : BaseFragment() {
    companion object {
        const val YEAR = "year"
        const val MONTH = "month"
        const val WEEK = "week"
        const val DAY = "day"
    }

    private val periodLengthDays : Long
        get() {
            return when (selectedTimePeriod) {
                YEAR -> 365
                MONTH -> 30
                WEEK -> 7
                DAY -> 1
                else -> throw Exception("Invalid period type")
            }
        }

    private val selectedTimePeriod : String
        get() {
            return when (view!!.periodLengthSpinner.selectedItemPosition) {
                0 -> YEAR
                1 -> MONTH
                2 -> WEEK
                3 -> DAY
                else -> throw Exception("shouldn't get here")
            }
        }


    //TODO
    private val selectedType = EntryTypes.MEDITATION

    private fun getStatsViewForType(entryType : String, entries : List<Entry>): View {

        val entriesForType = entries.filter { entry -> entry.type == entryType }

        // find which values are numeric and can be processed
        val defaultData = EntryTypes.getConfig(entryType).defaultData
        val averageValues = JSONObject().apply {
            put("entry count", entriesForType.size)
        }

        for (key in defaultData.keys()) {
            val defaultValue = defaultData.get(key).toString()

            if (defaultValue.toDoubleOrNull() != null) {
                val total = entriesForType.sumByDouble {
                    it.data.getDouble(key)
                }

                val average =
                    if (entriesForType.isNotEmpty()) total / entriesForType.size
                    else 0.0

                averageValues.put("total $key", total)
                averageValues.put("average $key", average)
            }
        }


        return LinearLayout(context!!).apply {
            orientation = LinearLayout.VERTICAL

            val statsView = EntryDataLayout(context!!, averageValues)
            val titleView = TextView(context).apply {
                text = entryType
                setTypeface(null, Typeface.BOLD)
            }

            addView(titleView)
            addView(statsView)
        }
    }

    private fun updateStats() {

        val startEpochSecond = Instant
            .now()
            .minus(Duration.ofDays(periodLengthDays))
            .epochSecond

        val endEpochSecond = Instant.now().epochSecond

        // updating the statistics view
        Thread {
            val entries = LogEntryDatabase.instance.entryDao()
                .getAllWithinDuration(startEpochSecond, endEpochSecond)

            activity!!.runOnUiThread {
                view!!.statsHolder.run {
                    removeAllViews()

                    for (type in (parentFragment as HomeFragment).selectedTypes) {
                        addView(getStatsViewForType(type, entries))
                    }

                    // leave room for fab
                    addView(Space(context).apply {
                        minimumHeight = parentFragment!!.view!!.fab.run {
                            measuredHeight * 2
                        }
                    })
                }

            }
        }.start()
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_stats, container, false)

        val mainActivity = (activity!! as MainActivity)

        rootView.periodLengthSpinner.apply {
            adapter = ArrayAdapter.createFromResource(
                mainActivity,
                R.array.period_lengths_array,
                android.R.layout.simple_spinner_item).apply {

                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    updateStats()
                }
            }

            setSelection(0)
        }

        return rootView
    }

    override fun onStart() {
        super.onStart()

        updateStats()
    }
}