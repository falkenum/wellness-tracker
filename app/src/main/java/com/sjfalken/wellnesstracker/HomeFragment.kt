package com.sjfalken.wellnesstracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_home.view.*
import org.json.JSONObject
import java.time.Duration
import java.time.Instant

class HomeFragment : BaseFragment() {
    companion object {
        const val YEAR = "year"
        const val MONTH = "month"
        const val WEEK = "week"
        const val DAY = "day"
    }

    private val periodLengthDays : Long
        get() {
            return when (checkedTimePeriod) {
                YEAR -> 365
                MONTH -> 30
                WEEK -> 7
                DAY -> 1
                else -> throw Exception("Invalid period type")
            }
        }

    private val checkedTimePeriod : String
        get() {
            val checkedRadioButtonId = rootView.findViewById<RadioGroup>(R.id.periodLengthRadioGroup)
                .checkedRadioButtonId
            return when (checkedRadioButtonId) {
                R.id.yearButton -> YEAR
                R.id.monthButton -> MONTH
                R.id.weekButton -> WEEK
                R.id.dayButton -> DAY
                else -> throw Exception("shouldn't get here")
            }
        }

    private lateinit var rootView : View

    private fun updateStats() {

        // to be called after accessing the database
        // entries is a list with all entries of the type passed in
        val processEntries = { entries : List<Entry> ->
            rootView.findViewById<TextView>(R.id.timePeriod).apply {
                text = checkedTimePeriod
            }

            rootView.findViewById<TextView>(R.id.numEntriesView).apply {
                text = entries.size.toString()
            }

            rootView.findViewById<FrameLayout>(R.id.averageValuesHolder).apply {
                val selectedType = (activity!! as MainActivity).selectedType

                // find which values are numeric and can be processed
                val defaultData = EntryTypes.getConfig(selectedType).defaultData
                val averageValues = JSONObject()

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
        rootView = inflater.inflate(R.layout.fragment_home, container, false)

        val mainActivity = (activity!! as MainActivity)

        mainActivity.apply {
            addOnTabSelectedAction {
                if (isVisible)
                    updateStats()
            }
        }

        rootView.findViewById<RadioGroup>(R.id.periodLengthRadioGroup).apply {
            setOnCheckedChangeListener { _, _ ->
                updateStats()
            }
            check(R.id.dayButton)
        }

        return rootView
    }

    override fun onStart() {
        super.onStart()

        updateStats()
    }
}