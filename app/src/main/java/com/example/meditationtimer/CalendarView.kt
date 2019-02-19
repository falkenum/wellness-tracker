package com.example.meditationtimer

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.text.BoringLayout
import android.text.Layout
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
import java.time.Year
import java.time.YearMonth
import java.util.*
import kotlin.math.roundToInt

class CalendarView(context : Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet) {

    var yearMonthShown : YearMonth = YearMonth.now()
        private set

    private val displayMetrics = resources.displayMetrics
    private var selectedDayView : DayView? = null
    var onDaySelect : ((Int) -> Unit)? = null
    var onDayUnselect : (() -> Unit)? = null
    var onMonthChange : ((YearMonth) -> Unit)? = null

    companion object {
        const val transparent = 0
        const val opaque = 255
        const val fillLayer = 0
        const val outlineLayer = 1
    }

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        orientation = LinearLayout.VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_calendar, this, true)

        findViewById<TextView>(R.id.calendarLeft).setOnClickListener {
            yearMonthShown = yearMonthShown.minusMonths(1)
            onMonthChange?.invoke(yearMonthShown)
            makeCalendar()
        }

        findViewById<TextView>(R.id.calendarRight).setOnClickListener {
            yearMonthShown = yearMonthShown.plusMonths(1)
            onMonthChange?.invoke(yearMonthShown)
            makeCalendar()
        }

        makeCalendar()
    }

    private open inner class EmptyDayView : TextView(context)  {
        init {
            layoutParams = TableRow.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    private inner class DayView(val dayOfMonth : Int) : EmptyDayView()  {

        fun setFilled(value : Boolean) {
            (background as LayerDrawable).getDrawable(fillLayer).alpha =
                if (value) opaque else transparent
        }

        fun setOutlined(value : Boolean) {
            (background as LayerDrawable).getDrawable(outlineLayer).alpha =
                if (value) opaque else transparent
        }

        init {
            id = dayOfMonth
            background = context!!.getDrawable(R.drawable.calendar_day_bg)

            setFilled(false)
            setOutlined(false)

            text = dayOfMonth.toString()
            textAlignment = View.TEXT_ALIGNMENT_CENTER

            setOnClickListener {

                if (selectedDayView == this) {
                    selectedDayView = null
                    setOutlined(false)
                    onDayUnselect?.invoke()
                }
                else {
                    selectedDayView?.setOutlined(false)
                    selectedDayView = this
                    setOutlined(true)

                    // callback for when a day is picked
                    onDaySelect?.invoke(dayOfMonth)
                }
            }

            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        }
    }

    private fun dpToPx(dp : Int) : Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
            displayMetrics).roundToInt()
    }

    private fun makeCalendar() {
        // reset selected day
        selectedDayView = null

        // set month/year title
        val dateYearStr = "${yearMonthShown.month} ${yearMonthShown.year}"
        findViewById<TextView>(R.id.dateYear).text = dateYearStr

        val calendarTable = findViewById<TableLayout>(R.id.calendarTable)

        val defaultDaysRow: () -> TableRow = {
            TableRow(context).apply {
                // padding between rows
                val verticalPadding = dpToPx(5)
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

    fun setDayFilled(dayOfMonth : Int, filled : Boolean) {
        findViewById<DayView>(dayOfMonth).setFilled(filled)
    }

}
