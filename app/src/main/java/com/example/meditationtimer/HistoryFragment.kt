package com.example.meditationtimer

import android.app.Dialog
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.CardView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.lang.IllegalStateException
import java.time.*
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

    class NewRecordDialogFragment() : DialogFragment() {
        lateinit var onConfirm : (LocalTime, Duration) -> Unit

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity!!.let {
                // Use the Builder class for convenient dialog construction
                val builder = AlertDialog.Builder(it)

                val dialogView = LayoutInflater.from(activity!!).
                    inflate(R.layout.view_new_record_dialog, null, false)
                val timePicker = dialogView.findViewById<TimePicker>(R.id.timePicker)
                val durationView = dialogView.findViewById<EditText>(R.id.durationView)

                builder.setView(dialogView)
                    .setPositiveButton("Confirm") { _, _ ->
                            val time = LocalTime.of(timePicker.hour, timePicker.minute)
                            val duration = Duration.ofMinutes(durationView.text.toString().toLong())
                            onConfirm(time, duration)
                        }
                    // default behavior on cancel, do nothing
                    .setNegativeButton("Cancel") { _, _ -> }
                // Create the AlertDialog object and return it
                builder.create()
            }
        }
    }

    private lateinit var monthRecords : Array< ArrayList<Record> >
    private lateinit var tabView: ScrollView
    private lateinit var calendarView : CalendarView
    private lateinit var numRecordsView : TextView
    private lateinit var dayInfoLayout : LinearLayout
    private lateinit var sessionCardsLayout : LinearLayout
    private lateinit var inflater: LayoutInflater
    private lateinit var fm: FragmentManager
    private lateinit var recordDao: RecordDao

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
                refreshTab()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }
    }

    private fun getRecordInfoCard(record : Record) : RecordCardView {

       return RecordCardView(activity!!).apply {
           insertRecordData(record)

           setOnDelete {
               val deleteAction = {
                   Thread {
                       // delete record
                       recordDao.delete(record)
                       refreshTab()
                   }.start()
               }
               // confirm that the record should be deleted
               DeleteRecordDialogFragment().apply {
                   messageStr = "Delete?"
                   onConfirmDelete = deleteAction
               }.show(fm, "DeleteRecordConfirmation")
           }

        }
    }

    private fun getNewRecord() {
        NewRecordDialogFragment().apply {
            onConfirm = { time, duration ->

                // get complete dateTime based on current selected year, month, and day
                val yearMonth = calendarView.yearMonthShown
                val date = LocalDate.of(yearMonth.year, yearMonth.month, selectedDayOFMonth!!)
                val dateTime = OffsetDateTime.of(date, time, OffsetDateTime.now().offset)
                Thread {
                    // add the new record retrieved from the dialog
                    recordDao.insert(Record.newMeditation(dateTime, duration))

                    // refresh the views to reflect new data
                    refreshTab()
                }.start()
            }
            show(fm, "NewRecordDialog")
        }
    }

    private fun showInfoForSelectedDay() {
        if (selectedDayOFMonth == null) {
            dayInfoLayout.visibility = View.GONE
            return
        }

        val recordsForDay = monthRecords[selectedDayOFMonth!! - 1]
        numRecordsView.text = recordsForDay.size.toString()

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
            { ArrayList<Record>(0) }

        for (record in recordDao.getAll()) {
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
        Thread {
            // TODO make sure this is done before using meditationRecordDao
//            meditationRecordDao = RecordDatabase.instance.meditationRecordDao()
            recordDao = RecordDatabase.instance.recordDao()
        }.start()

        inflater = inflaterArg
        tabView = inflater.inflate(R.layout.tab_history, container, false) as ScrollView

        calendarView = tabView.findViewById(R.id.calendarView)
        dayInfoLayout = tabView.findViewById(R.id.dayInfoLayout)
        sessionCardsLayout = tabView.findViewById(R.id.sessionCardsLayout)
        numRecordsView = tabView.findViewById(R.id.numRecords)
        fm = activity!!.supportFragmentManager

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
            selectedDayOFMonth = null
            refreshTab()
        }

        tabView.findViewById<Button>(R.id.addRecordButton).setOnClickListener {
            getNewRecord()
        }

        activity!!.bindService(Intent(activity, TimerService::class.java), timerConnection, 0)

        return tabView
    }

    override fun onStart() {
        super.onStart()
        refreshTab()
    }

    private fun refreshTab() {
        Thread {
            reloadMonthRecords()
            activity?.runOnUiThread {
                fillCalendarDays()
                showInfoForSelectedDay()
            }
        }.start()
    }

    override fun onDestroyView() {
        activity!!.unbindService(timerConnection)
        super.onDestroyView()
    }

}
