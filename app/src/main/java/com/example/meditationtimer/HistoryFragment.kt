package com.example.meditationtimer

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class HistoryFragment : Fragment() {


    fun getCell(cellText : String) : TextView {
        return TextView(activity).apply{
            text = cellText
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            layoutParams = TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f)

        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val tabView = inflater.inflate(R.layout.tab_history, container, false)
        val calendar = tabView.findViewById<TableLayout>(R.id.calendarTable)

        val yearMonthShown = YearMonth.now()

        // starting with the first day of the month, we will populate the calendar
        var date = LocalDate.of(yearMonthShown.year, yearMonthShown.month, 1)

        // add spaces for the days from the previous month to begin the first row
        var rowToAdd = TableRow(activity).apply {
            var dayOfWeek = DayOfWeek.SUNDAY
            while (dayOfWeek != DayOfWeek.from(date)) {
                addView(getCell(""))
                dayOfWeek = dayOfWeek.plus(1)
            }
        }

        for (i in 1..date.lengthOfMonth()) {
            rowToAdd.addView(getCell(date.dayOfMonth.toString()))
            // if saturday, then add to calendar and go to next row
            if (date.dayOfWeek == DayOfWeek.SATURDAY) {
                calendar.addView(rowToAdd)
                rowToAdd = TableRow(activity)
            }
            date = date.plusDays(1)
        }

        // add any days from the last row
        val daysInLastWeek = rowToAdd.childCount
        if (daysInLastWeek > 0) {
            // add spaces to fill in the gap
            for (i in 1..(7 - daysInLastWeek)) {
                rowToAdd.addView(getCell(""))
            }

            calendar.addView(rowToAdd)
        }

        return tabView
    }
}

