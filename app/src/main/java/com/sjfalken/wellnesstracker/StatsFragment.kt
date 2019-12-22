package com.sjfalken.wellnesstracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
            return when (rootView.spinner.selectedItemPosition) {
                0 -> YEAR
                1 -> MONTH
                2 -> WEEK
                3 -> DAY
                else -> throw Exception("shouldn't get here")
            }
        }

    private lateinit var rootView : View

    private fun updateStats() {

        // to be called after accessing the database
        // entries is a list with all entries of the type passed in
        val processEntries = { entries : List<Entry> ->
            rootView.findViewById<FrameLayout>(R.id.averageValuesHolder).apply {
                val selectedType = (activity!! as MainActivity).selectedType

                // find which values are numeric and can be processed
                val defaultData = EntryTypes.getConfig(selectedType).defaultData
                val averageValues = JSONObject().apply {
                    put("entry count", entries.size)
                }

                for (key in defaultData.keys()) {
                    val defaultValue = defaultData.get(key).toString()

                    if (defaultValue.toDoubleOrNull() != null) {
                        val total = entries.sumByDouble {
                                it.data.getDouble(key)
                        }

                        val average =
                            if (entries.isNotEmpty()) total / entries.size
                            else 0.0

                        averageValues.put("total $key", total)
                        averageValues.put("average $key", average)
                    }
                }

                val averageValuesView = EntryDataLayout(context, averageValues)

                removeAllViews()
                addView(averageValuesView)
            }
        }

        val startEpochSecond = Instant
            .now()
            .minus(Duration.ofDays(periodLengthDays))
            .epochSecond

        val endEpochSecond = Instant.now().epochSecond

        // updating the statistics view
        Thread {
            val selectedType = (activity!! as MainActivity).selectedType
            val entries = LogEntryDatabase.instance.entryDao()
                .getAllWithinDurationAndType(startEpochSecond, endEpochSecond, selectedType)

            activity!!.runOnUiThread {
                processEntries(entries)
            }
        }.start()
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_stats, container, false)

        val mainActivity = (activity!! as MainActivity)

        mainActivity.apply {
            addOnTabSelectedAction {
                if (isVisible)
                    updateStats()
            }
        }

        rootView.spinner.apply {
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