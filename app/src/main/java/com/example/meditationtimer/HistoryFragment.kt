package com.example.meditationtimer

import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.lang.String.format
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import kotlin.math.roundToInt


class HistoryFragment : Fragment() {

    private lateinit var tabView: ConstraintLayout
    private var yearMonthShown = YearMonth.now()

    private fun dpToPx(dp : Int) : Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
            resources.displayMetrics).roundToInt()
    }

    private fun getCell(cellText : String) : TextView {
        return TextView(activity).apply{
            text = cellText
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            layoutParams = TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f)

            background = (activity!!.getDrawable(R.drawable.calendar_day_bg) as LayerDrawable).apply {
                val transparent = 0
                val opaque = 255
                val fillLayer = 0
                val outlineLayer = 1

                getDrawable(fillLayer).alpha = opaque
            }

            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        }
    }

    private fun makeCalendar() {
        // set month/year title
        tabView.findViewById<TextView>(R.id.dateYear).text =
            format(Locale.ENGLISH, "%s %d", yearMonthShown.month.toString(), yearMonthShown.year)

        val calendarTable = tabView.findViewById<TableLayout>(R.id.calendarTable)

        val defaultDaysRow : () -> TableRow = {
            TableRow(activity).apply {
                // padding between rows
                val verticalPadding = dpToPx(10)
                setPaddingRelative(0, verticalPadding, 0, verticalPadding)
            }
        }

        // clear all weeks to start fresh
        calendarTable.removeAllViews()

        // starting with the first day of the month, we will populate the calendar
        var date = LocalDate.of(yearMonthShown.year, yearMonthShown.month, 1)

        // add spaces for the days from the previous month to begin the first row
        var rowToAdd = defaultDaysRow().apply {
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
                calendarTable.addView(rowToAdd)
                rowToAdd = defaultDaysRow()
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

            calendarTable.addView(rowToAdd)
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        tabView = inflater.inflate(R.layout.tab_history, container, false) as ConstraintLayout
        tabView.findViewById<TextView>(R.id.calendarLeft).setOnClickListener {
            yearMonthShown = yearMonthShown.minusMonths(1)
            activity?.runOnUiThread { makeCalendar() }
        }

        tabView.findViewById<TextView>(R.id.calendarRight).setOnClickListener {
            yearMonthShown = yearMonthShown.plusMonths(1)
            activity?.runOnUiThread { makeCalendar() }
        }

        makeCalendar()
        return tabView
    }
}

