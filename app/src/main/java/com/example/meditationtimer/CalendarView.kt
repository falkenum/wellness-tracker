package com.example.meditationtimer

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import java.lang.String.format
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import kotlin.math.roundToInt

class CalendarView(context : Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet) {

    private var yearMonthShown = YearMonth.now()
    private var selectedDay = LocalDate.now()

    val transparent = 0
    val opaque = 255
    val fillLayer = 0
    val outlineLayer = 1

    init {
        orientation = LinearLayout.VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_calendar, this, true)

        findViewById<TextView>(R.id.calendarLeft).setOnClickListener {
            yearMonthShown = yearMonthShown.minusMonths(1)
            makeCalendar()
        }

        findViewById<TextView>(R.id.calendarRight).setOnClickListener {
            yearMonthShown = yearMonthShown.plusMonths(1)
            makeCalendar()
        }

        makeCalendar()
    }

    private fun dpToPx(dp : Int) : Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
            resources.displayMetrics).roundToInt()
    }

    open inner class EmptyDayView : TextView(context)  {
        init {
            layoutParams = TableRow.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        }
    }

    inner class DayView(val dayOfMonth : Int) : EmptyDayView()  {

        private fun setFilled(value : Boolean) {
            (background as LayerDrawable).getDrawable(fillLayer).alpha =
                if (value) opaque else transparent
        }

        private fun setOutlined(value : Boolean) {
            (background as LayerDrawable).getDrawable(outlineLayer).alpha =
                if (value) opaque else transparent
        }

        init {
            background = context!!.getDrawable(R.drawable.calendar_day_bg)

            setFilled(false)
            setOutlined(false)

            text = dayOfMonth.toString()
            textAlignment = View.TEXT_ALIGNMENT_CENTER

            setOnClickListener {
                setOutlined(true)
            }

            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        }
    }

    private fun makeCalendar() {
        // set month/year title

        val monthStr = yearMonthShown.month.toString()
        val yearStr = yearMonthShown.year
        val dateYearStr = "$monthStr $yearStr"
        findViewById<TextView>(R.id.dateYear).text = dateYearStr

        val calendarTable = findViewById<TableLayout>(R.id.calendarTable)

        val defaultDaysRow: () -> TableRow = {
            TableRow(context).apply {
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
                addView(EmptyDayView())
                dayOfWeek = dayOfWeek.plus(1)
            }
        }

        for (i in 1..date.lengthOfMonth()) {
            rowToAdd.addView(DayView(date.dayOfMonth))
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
                rowToAdd.addView(EmptyDayView())
            }

            calendarTable.addView(rowToAdd)
        }
    }
}
