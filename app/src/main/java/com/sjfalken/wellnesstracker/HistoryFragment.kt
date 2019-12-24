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
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_main.*

class HistoryFragment : BaseFragment() {

    class DeleteEntryDialogFragment : androidx.fragment.app.DialogFragment() {
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

        val recordsForDay = monthEntries[selectedDayOfMonth - 1]
        Log.d("showInfoForSelectedDay", "number of records for day: ${recordsForDay.size}")

        // remove all the cards, leave the summaryLayout at the beginning
        sessionCardsLayout.removeAllViews()

        sessionCardsLayout.visibility = View.VISIBLE
        // generate a cardview for each session of that day
        for (record in recordsForDay) {
            sessionCardsLayout.addView(getEntryInfoCard(record))
        }

    }

    private fun loadMonthEntries() {
        // Make an array of lists containing the records for each day of the month
        monthEntries = Array(calendarView.yearMonthShown.lengthOfMonth())
            { ArrayList<Entry>(0) }
        val selectedType = (activity!! as MainActivity).selectedType


        val year = calendarView.yearMonthShown.year
        val month = calendarView.yearMonthShown.month
        // for day in month
        for (dayOfMonth in 1..calendarView.yearMonthShown.lengthOfMonth()) {
            val date = LocalDate.of(year, month, dayOfMonth)
            val records = entryDao.getAllForDateAndType(date, selectedType)
            assert(monthEntries[dayOfMonth - 1].size == 0)
            monthEntries[dayOfMonth - 1].addAll(records)
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
            addOnTabSelectedAction { refreshFragmentView() }
        }
        return fragmentView
    }

    override fun onStart() {
        super.onStart()
        refreshFragmentView()
    }

    private fun refreshFragmentView() {
        if (activity == null) return
        Thread {
            loadMonthEntries()
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
