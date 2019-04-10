package com.example.meditationtimer

import android.content.Context
import android.graphics.Typeface
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

class CalendarView(context : Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet) {

    var yearMonthShown : YearMonth = YearMonth.now()
        private set
    val lengthOfMonth : Int get() = yearMonthShown.lengthOfMonth()

    private val displayMetrics = resources.displayMetrics
    private var selectedDayView : DayView? = null
    var onDaySelect : ((Int) -> Unit)? = null
    var onDayUnselect : (() -> Unit)? = null
    var onMonthChange : ((YearMonth) -> Unit)? = null

    companion object {
        const val transparent = 0
        const val opaque = 255
        const val hasEntriesLayer = 0
        const val selectedDayLayer = 1
        const val currentDayLayer = 2
    }

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        orientation = LinearLayout.VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_calendar, this, true)

        findViewById<TextView>(R.id.calendarLeft).setOnClickListener {
            yearMonthShown = yearMonthShown.minusMonths(1)
            makeCalendar()
            onMonthChange?.invoke(yearMonthShown)
        }

        findViewById<TextView>(R.id.calendarRight).setOnClickListener {
            yearMonthShown = yearMonthShown.plusMonths(1)
            makeCalendar()
            onMonthChange?.invoke(yearMonthShown)
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
        private fun setLayer(layer : Int, value : Boolean) {
            (background as LayerDrawable).getDrawable(layer).alpha =
                if (value) opaque else transparent
        }

        fun setHasEntries(value : Boolean) = setLayer(hasEntriesLayer, value)
        fun setSelectedDay(value : Boolean) = setLayer(selectedDayLayer, value)
        fun setCurrentDay(value : Boolean) = setLayer(currentDayLayer, value)

        init {
            id = dayOfMonth
            background = context!!.getDrawable(R.drawable.calendar_day_bg)

            setHasEntries(false)
            setSelectedDay(false)

            // set extra outline for today's date
            val thisDate = LocalDate.of(yearMonthShown.year, yearMonthShown.month, dayOfMonth)
            setCurrentDay(thisDate == LocalDate.now())

            text = dayOfMonth.toString()
            textAlignment = View.TEXT_ALIGNMENT_CENTER

            setOnClickListener {

                if (selectedDayView == this) {
                    selectedDayView = null
                    setSelectedDay(false)
                    onDayUnselect?.invoke()
                }
                else {
                    selectedDayView?.setSelectedDay(false)
                    selectedDayView = this
                    setSelectedDay(true)

                    // callback for when a day is picked
                    onDaySelect?.invoke(dayOfMonth)
                }
            }
            if (dayOfMonth == 1) setTypeface(null, Typeface.BOLD)

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

    // given day of month, set whether the bg of that day should be filled
    fun fillDaysBy(pred : (Int) -> Boolean) {
        for (dayOfMonth in 1..lengthOfMonth) {
            findViewById<DayView>(dayOfMonth).setHasEntries(pred(dayOfMonth))
        }
    }
}
