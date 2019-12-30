package com.sjfalken.wellnesstracker

import android.app.Dialog
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.time.*

class HistoryFragment : BaseFragment() {



    class DeleteEntryDialogFragment : androidx.fragment.app.DialogFragment() {
        lateinit var messageStr : String
        lateinit var onConfirmDelete : () -> Unit

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity!!.let {
                // Use the Builder class for convenient dialog construction
                val builder = AlertDialog.Builder(it)
                builder.setMessage(messageStr)
                    .setPositiveButton("Yes") { _, _ ->
                            onConfirmDelete()
                        }
                    .setNegativeButton("No") { _, _ -> }
                // Create the AlertDialog object and return it
                builder.create()
            }
        }
    }

    private lateinit var monthEntries : Array< ArrayList<Entry> >
    private lateinit var fragmentView: ScrollView
    private lateinit var calendarView : CalendarView
    private lateinit var sessionCardsLayout : LinearLayout
    private lateinit var inflater: LayoutInflater
    private lateinit var fm: androidx.fragment.app.FragmentManager
    private lateinit var entryDao: EntryDao


    private val timerConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val timerServiceBinder = (binder as TimerService.TimerBinder)

            timerServiceBinder.getService().onTimerFinishTasks.add {
                refreshFragmentView()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }
    }

    private fun getEntryInfoCard(entry : Entry) : EntryCardView {

       return EntryCardView(activity!!).apply {
           insertEntryData(entry)
           setBackgroundColor(activity!!.getColor(R.color.appBackground))
           setOnDelete {
               val deleteAction = {
                   Thread {
                       // delete entry
                       entryDao.delete(entry)
                       refreshFragmentView()
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
        val selectedDayOfMonth = calendarView.selectedDate?.dayOfMonth
        if (selectedDayOfMonth == null) {
            sessionCardsLayout.visibility = View.GONE
            return
        }

        val entriesForDay = monthEntries[selectedDayOfMonth - 1]
        Log.d("showInfoForSelectedDay", "number of records for day: ${entriesForDay.size}")

        // remove all the cards, leave the summaryLayout at the beginning
        sessionCardsLayout.removeAllViews()

        sessionCardsLayout.visibility = View.VISIBLE
        // generate a cardview for each session of that day
        for (entry in entriesForDay) {
            val entryCopy = entriesList.find {
                it.dateTime.toEpochSecond() == entry.dateTime.toEpochSecond()
            }

            if (entryCopy != null) {
                throw Exception("entry copy found")
            }
            entriesList.add(entry)
            sessionCardsLayout.addView(getEntryInfoCard(entry))
        }
        entriesList.removeAll { true }
    }

    private val entriesList = mutableListOf<Entry>()

    private fun loadMonthEntries() {
        // Make an array of lists containing the records for each day of the month
        monthEntries = Array(calendarView.yearMonthShown.lengthOfMonth())
            { ArrayList<Entry>(0) }

        val selectedTypes = (parentFragment as HomeFragment).selectedTypes
        val year = calendarView.yearMonthShown.year
        val month = calendarView.yearMonthShown.month
        // for day in month
        for (dayOfMonth in 1..calendarView.yearMonthShown.lengthOfMonth()) {

            for (type in selectedTypes) {
                val date = LocalDate.of(year, month, dayOfMonth)
                val entries = entryDao.getAllForDateAndType(date, type)
                val dayOfMonthIndex = dayOfMonth - 1
                assert(monthEntries[dayOfMonthIndex].size == 0)

                for ((i, a) in entries.withIndex()) {
                    for ((j, b) in entries.withIndex()) {
                        if (i != j && a.dateTime.toEpochSecond() == b.dateTime.toEpochSecond())
                            throw java.lang.Exception("copy found")
                    }
                }

                monthEntries[dayOfMonthIndex].addAll(entries)
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
        fragmentView = inflater.inflate(R.layout.fragment_history, container, false) as ScrollView

        calendarView = fragmentView.findViewById(R.id.calendarView)
        sessionCardsLayout = fragmentView.findViewById(R.id.sessionCardsLayout)
        fm = activity!!.supportFragmentManager

        // setting up calendar callbacks
        calendarView.onDaySelect = {
            showInfoForSelectedDay()
        }

        calendarView.onDayDeselect = {
            // make summary invisible again when no day is selected
            showInfoForSelectedDay()
        }

        calendarView.onMonthChange = {
            refreshFragmentView()
        }

        (activity!! as MainActivity).apply {
            bindService(Intent(activity, TimerService::class.java), timerConnection, 0)
        }

        (parentFragment as HomeFragment).addOnTypesSelectedAction { refreshFragmentView() }
        return fragmentView
    }

    override fun onStart() {
        super.onStart()
        refreshFragmentView()
    }

    private val lock = HistoryFragment::class
    private fun refreshFragmentView() {
        if (activity == null) return
        Thread {
            synchronized(lock) {
                loadMonthEntries()
            }
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
