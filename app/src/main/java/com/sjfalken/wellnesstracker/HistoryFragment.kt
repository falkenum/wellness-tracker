package com.sjfalken.wellnesstracker

import android.app.Dialog
import android.content.*
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.lang.IllegalStateException
import java.time.*
import androidx.navigation.fragment.findNavController

class HistoryFragment : BaseFragment() {

    class DeleteEntryDialogFragment() : androidx.fragment.app.DialogFragment() {
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

    private lateinit var monthEntries : Array< ArrayList<Entry> >
    private lateinit var tabView: ScrollView
    private lateinit var calendarView : CalendarView
    private lateinit var numEntriesView : TextView
    private lateinit var dayInfoLayout : LinearLayout
    private lateinit var sessionCardsLayout : LinearLayout
    private lateinit var inflater: LayoutInflater
    private lateinit var fm: androidx.fragment.app.FragmentManager
    private lateinit var entryDao: EntryDao

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

    private fun getEntryInfoCard(entry : Entry) : EntryCardView {

       return EntryCardView(activity!!).apply {
           insertEntryData(entry)

           setOnDelete {
               val deleteAction = {
                   Thread {
                       // delete entry
                       entryDao.delete(entry)
                       refreshTab()
                   }.start()
               }
               // confirm that the entry should be deleted
               DeleteEntryDialogFragment().apply {
                   messageStr = "Delete?"
                   onConfirmDelete = deleteAction
               }.show(fm, "DeleteEntryConfirmation")
           }

        }
    }

    private fun showInfoForSelectedDay() {
        if (selectedDayOFMonth == null) {
            dayInfoLayout.visibility = View.GONE
            return
        }

        val recordsForDay = monthEntries[selectedDayOFMonth!! - 1]
        numEntriesView.text = recordsForDay.size.toString()

        // remove all the cards, leave the summaryLayout at the beginning
        sessionCardsLayout.removeAllViews()

        // generate a cardview for each session of that day
        for (record in recordsForDay) {
            sessionCardsLayout.addView(getEntryInfoCard(record))
        }

        dayInfoLayout.visibility = View.VISIBLE
    }

    private fun reloadMonthEntries() {
        // Make an array of lists containing the records for each day of the month
        monthEntries = Array(calendarView.yearMonthShown.lengthOfMonth())
            { ArrayList<Entry>(0) }

        for (record in entryDao.getAll()) {
            val dateTime = record.dateTime

            // if the record is in the shown month
            if (YearMonth.from(dateTime) == calendarView.yearMonthShown) {
                val dayOfMonth = MonthDay.from(dateTime).dayOfMonth

                // add to the appropriate array list, zero indexed for the array
                monthEntries[dayOfMonth - 1].add(record)
            }
        }
    }

    private fun fillCalendarDays() {
        calendarView.fillDaysBy { dayOfMonth -> monthEntries[dayOfMonth - 1].size > 0 }
    }

    override fun onCreateView(inflaterArg: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Thread {
            // TODO make sure this is done before using entryDao
            entryDao = LogEntryDatabase.instance.entryDao()
        }.start()

        inflater = inflaterArg
        tabView = inflater.inflate(R.layout.fragment_history, container, false) as ScrollView

        calendarView = tabView.findViewById(R.id.calendarView)
        dayInfoLayout = tabView.findViewById(R.id.dayInfoLayout)
        sessionCardsLayout = tabView.findViewById(R.id.sessionCardsLayout)
        numEntriesView = tabView.findViewById(R.id.numRecords)
        fm = activity!!.supportFragmentManager

        // setting up calendar callbacks
        calendarView.onDaySelect = { dayOfMonth ->
            selectedDayOFMonth = dayOfMonth
            showInfoForSelectedDay()
        }

        calendarView.onDayDeselect = {
            // make summary invisible again when no day is selected
            selectedDayOFMonth = null
            showInfoForSelectedDay()
        }

        calendarView.onMonthChange = {
            selectedDayOFMonth = null
            refreshTab()
        }

        tabView.findViewById<Button>(R.id.addRecordButton).setOnClickListener {
            findNavController().run {
                // argument to set date and time in new fragment
                val destinationArgs = Bundle().apply {
                    putString(NewEntryFragment.DATE_TIME, ZonedDateTime.now().toString())
                }
                navigate(R.id.newEntryFragment, destinationArgs)
            }
        }

        activity!!.bindService(Intent(activity, TimerService::class.java), timerConnection, 0)

//        tabView.visibility = GONE

        selectedDayOFMonth = LocalDate.now().dayOfMonth

        return tabView
    }

    override fun onStart() {
        super.onStart()

        refreshTab()
    }

    private fun refreshTab() {
        Thread {
            reloadMonthEntries()
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
