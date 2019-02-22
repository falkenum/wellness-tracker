package com.example.meditationtimer

import android.app.Dialog
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.icu.text.AlphabeticIndex
import android.os.Bundle
import android.os.IBinder
import android.support.constraint.ConstraintLayout
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.CardView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlinx.android.synthetic.main.tab_history.*
import org.w3c.dom.Text
import java.lang.IllegalStateException
import java.time.MonthDay
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class HistoryFragment : Fragment() {

    class DeleteRecordDialogFragment() : DialogFragment() {
        lateinit var messageStr : String
        lateinit var onConfirmDelete : () -> Unit

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity!!.let {
                // Use the Builder class for convenient dialog construction
                val builder = AlertDialog.Builder(it)
                builder.setMessage(messageStr)
                    .setPositiveButton("Yes",
                        DialogInterface.OnClickListener { _, _ ->
                            onConfirmDelete()
                        })
                    .setNegativeButton("No", DialogInterface.OnClickListener { _, _ -> })
                // Create the AlertDialog object and return it
                builder.create()
            }
        }
    }

    private lateinit var monthRecords : Array< ArrayList<MeditationRecord> >
    private lateinit var tabView: ScrollView
    private lateinit var calendarView : CalendarView
    private lateinit var numRecordsView : TextView
    private lateinit var totalTimeView : TextView
    private lateinit var dayInfoLayout : LinearLayout
    private lateinit var sessionCardsLayout : LinearLayout
    private lateinit var inflater: LayoutInflater

    // null means no day is selected
    private var selectedDayOFMonth : Int? = null
        set(value) {
            if (value != null && (value < 1 || value > calendarView.lengthOfMonth))
                throw IllegalStateException("Invalid day $value")

            field = value
        }

    private val timerConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val timerServiceBinder = (binder as TimerService.TimerBinder)

            timerServiceBinder.getService().onTimerFinishTasks.add {
                reloadMonthRecords()
                activity!!.runOnUiThread {
                    fillCalendarDays()
                    showInfoForSelectedDay()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }
    }

    private fun getRecordInfoCard(record : MeditationRecord) : CardView {
        val timeStamp = record.dateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
        val lengthMinutes = record.duration.toMinutes()
        val recordStr = "at $timeStamp for $lengthMinutes minutes"

       return inflater.inflate(R.layout.session_record_card, dayInfoLayout, false).apply {
            val deleteAction = {
                Thread {
                    // delete record
                    RecordDatabase.remove(record)
                    reloadMonthRecords()
                    activity!!.runOnUiThread {
                        fillCalendarDays()
                        showInfoForSelectedDay()
                    }
                }.start()
            }

            findViewById<TextView>(R.id.timeStamp).text = recordStr
            findViewById<Button>(R.id.deleteButton).setOnClickListener {
                // confirm that the record should be deleted
                DeleteRecordDialogFragment().apply {
                    messageStr = "Delete \"$recordStr\"?"
                    onConfirmDelete = deleteAction
                }.show(activity!!.supportFragmentManager, "DeleteRecordConfirmation")
            }

        } as CardView
    }

    private fun showInfoForSelectedDay() {
        if (selectedDayOFMonth == null) {
            dayInfoLayout.visibility = View.GONE
            return
        }

        val recordsForDay = monthRecords[selectedDayOFMonth!! - 1]
        numRecordsView.text = recordsForDay.size.toString()
        totalTimeView.text = recordsForDay.sumBy { it.duration.toMinutes().toInt() }.toString()

        // remove all the cards, leave the summaryLayout at the beginning
        sessionCardsLayout.removeAllViews()

        // generate a cardview for each session of that day
        for (record in recordsForDay) {
            sessionCardsLayout.addView(getRecordInfoCard(record))
        }

        dayInfoLayout.visibility = View.VISIBLE
    }

    private fun reloadMonthRecords() {
        // Make an array of lists containing the records for each day of the month
        monthRecords = Array(calendarView.yearMonthShown.lengthOfMonth())
            { ArrayList<MeditationRecord>(0) }

        for (record in RecordDatabase.records) {
            val dateTime = record.dateTime

            // if the record is in the shown month
            if (YearMonth.from(dateTime) == calendarView.yearMonthShown) {
                val dayOfMonth = MonthDay.from(dateTime).dayOfMonth

                // add to the appropriate array list, zero indexed for the array
                monthRecords[dayOfMonth - 1].add(record)
            }
        }
    }

    private fun fillCalendarDays() {
        calendarView.fillDaysBy { dayOfMonth -> monthRecords[dayOfMonth - 1].size > 0 }
    }

    override fun onCreateView(inflaterArg: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        inflater = inflaterArg
        tabView = inflater.inflate(R.layout.tab_history, container, false) as ScrollView

        calendarView = tabView.findViewById(R.id.calendarView)
        dayInfoLayout = tabView.findViewById(R.id.dayInfoLayout)
        sessionCardsLayout = tabView.findViewById(R.id.sessionCardsLayout)
        numRecordsView = tabView.findViewById(R.id.numRecords)
        totalTimeView = tabView.findViewById(R.id.totalTime)

        reloadMonthRecords()
        fillCalendarDays()
        showInfoForSelectedDay()

        // setting up calendar callbacks
        calendarView.onDaySelect = { dayOfMonth ->
            selectedDayOFMonth = dayOfMonth
            showInfoForSelectedDay()
        }

        calendarView.onDayUnselect = {
            // make summary invisible again when no day is selected
            selectedDayOFMonth = null
            showInfoForSelectedDay()
        }

        calendarView.onMonthChange = {
            reloadMonthRecords()
            fillCalendarDays()
            showInfoForSelectedDay()
        }

        activity!!.bindService(Intent(activity, TimerService::class.java), timerConnection, 0)

        return tabView
    }

    override fun onDestroyView() {
        activity!!.unbindService(timerConnection)
        super.onDestroyView()
    }

}
