package com.example.meditationtimer

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.support.v7.widget.CardView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.w3c.dom.Text
import java.time.MonthDay
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class HistoryFragment : Fragment() {

    private lateinit var monthRecords : Array< ArrayList<MeditationRecord> >
    private lateinit var tabView: ScrollView
    private lateinit var calendarView : CalendarView
    private lateinit var numRecordsView : TextView
    private lateinit var totalTimeView : TextView
    private lateinit var dayInfoLayout : LinearLayout

    // because the dayOfMonth can't be 0, this indicates that it has not been set
    // or that no day is selected
    private var selectedDayOFMonth : Int = 0

    private val timerConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val timerServiceBinder = (binder as TimerService.TimerBinder)

            timerServiceBinder.getService().onTimerFinishTasks.add {
                val newRecord = RecordDatabase.getLastAdded()

                // add this to the month records if the new record matches the shown month
                addRecordToCalendar(newRecord)

                // if the new record is the same as the currently selected day
                if (newRecord.dateTime.dayOfMonth == selectedDayOFMonth) {
                    // update the shown summary info
                    showInfoForDay(selectedDayOFMonth)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }
    }

    private fun addRecordToCalendar(record: MeditationRecord) {
        val dateTime = record.dateTime

        // if the record is in the shown month
        if (YearMonth.from(dateTime) == calendarView.yearMonthShown) {
            val dayOfMonth = MonthDay.from(dateTime).dayOfMonth

            // add to the appropriate array list, zero indexed for the array
            monthRecords[dayOfMonth - 1].add(record)

            // fill in the day view background to indicate there is at least one record for that day
            calendarView.setDayFilled(dayOfMonth, true)
        }
    }

    private fun showInfoForDay(dayOfMonth : Int) {
        val recordsForDay = monthRecords[dayOfMonth - 1]
        numRecordsView.text = recordsForDay.size.toString()
        totalTimeView.text = recordsForDay.sumBy { it.duration.toMinutes().toInt() }.toString()

        // remove all the cards
        dayInfoLayout.removeViewsInLayout(1, dayInfoLayout.childCount - 1 )

        // generate a cardview for each session of that day
        val inflater = LayoutInflater.from(activity!!)

        for (record in recordsForDay) {
            val timeStamp = record.dateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
            val lengthMinutes = record.duration.toMinutes()

            val card = inflater.inflate(R.layout.session_record_card, dayInfoLayout, false).apply {
                findViewById<TextView>(R.id.timeStamp).text = "at $timeStamp for $lengthMinutes minutes"
            }

            dayInfoLayout.addView(card)
        }

        dayInfoLayout.visibility = View.VISIBLE
    }

    private fun reloadMonthRecords() {
        // Make an array of lists containing the records for each day of the month
        monthRecords = Array(calendarView.yearMonthShown.lengthOfMonth())
        { ArrayList<MeditationRecord>(0) }

        for (record in RecordDatabase.records) addRecordToCalendar(record)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        tabView = inflater.inflate(R.layout.tab_history, container, false) as ScrollView

        calendarView = tabView.findViewById(R.id.calendarView)
        dayInfoLayout = tabView.findViewById(R.id.dayInfoLayout)
        numRecordsView = tabView.findViewById(R.id.numRecords)
        totalTimeView = tabView.findViewById(R.id.totalTime)

        // by default summary and session cards are not visible
        dayInfoLayout.visibility = View.GONE

        reloadMonthRecords()

        // setting up calendar callbacks
        calendarView.onDaySelect = { dayOfMonth ->
            selectedDayOFMonth = dayOfMonth
            showInfoForDay(dayOfMonth)
        }

        calendarView.onDayUnselect = {
            // make summary invisible again when no day is selected
            dayInfoLayout.visibility = View.GONE
            selectedDayOFMonth = 0
        }

        calendarView.onMonthChange = {
            dayInfoLayout.visibility = View.GONE
            reloadMonthRecords()
        }

        activity!!.bindService(Intent(activity, TimerService::class.java), timerConnection, 0)

        // button to add past record
        // deleting records

        return tabView
    }

    override fun onDestroyView() {
        activity!!.unbindService(timerConnection)
        super.onDestroyView()
    }

}
