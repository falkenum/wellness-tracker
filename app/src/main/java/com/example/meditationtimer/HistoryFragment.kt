package com.example.meditationtimer

import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.w3c.dom.Text
import java.time.MonthDay
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId

class HistoryFragment : Fragment() {

    private lateinit var tabView: ConstraintLayout

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        tabView = inflater.inflate(R.layout.tab_history, container, false) as ConstraintLayout
        val calendarView = tabView.findViewById<CalendarView>(R.id.calendarView)
        val yearMonth = calendarView.yearMonthShown

        // Make an array of lists containing the records for each day of the month
        val monthRecords = Array(yearMonth.lengthOfMonth()) { ArrayList<MeditationRecord>(0) }

        for (record in RecordDatabase.records) {
            val dateTime = record.dateTime

            // if the record is in the shown month
            if (YearMonth.from(dateTime) == yearMonth) {
                val dayOfMonth = MonthDay.from(dateTime).dayOfMonth

                // add to the appropriate array list, zero indexed for the array
                monthRecords[dayOfMonth - 1].add(record)

                // fill in the day view background to indicate there is at least one record for that day
                calendarView.setDayFilled(dayOfMonth, true)
            }
        }

        val summaryLayout = tabView.findViewById<ConstraintLayout>(R.id.summaryLayout)
        val numRecordsView = summaryLayout.findViewById<TextView>(R.id.numRecords)
        val totalTimeView = summaryLayout.findViewById<TextView>(R.id.totalTime)

        // by default summary is not visible
        summaryLayout.visibility = View.GONE

        calendarView.onDaySelect = { dayOfMonth ->
            val recordsForDay = monthRecords[dayOfMonth - 1]
            numRecordsView.text = recordsForDay.size.toString()
            totalTimeView.text = recordsForDay.sumBy { it.duration.toMinutes().toInt() }.toString()
            summaryLayout.visibility = View.VISIBLE
        }

        // make summary invisible again when no day is selected
        calendarView.onDayUnselect = {
            summaryLayout.visibility = View.GONE
        }

        return tabView
    }
}

