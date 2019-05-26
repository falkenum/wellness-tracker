package com.example.meditationtimer

import android.app.Dialog
import android.content.*
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AlertDialog
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.*
import org.json.JSONObject
import java.lang.IllegalStateException
import java.time.*
import android.database.sqlite.SQLiteConstraintException

class HistoryFragment : androidx.fragment.app.Fragment() {

    class DeleteRecordDialogFragment() : androidx.fragment.app.DialogFragment() {
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

    class NewRecordDialogFragment : androidx.fragment.app.DialogFragment() {
        lateinit var onConfirm : (LocalTime, String, JSONObject) -> Unit
        private lateinit var dialogView : LinearLayout
        private lateinit var confirmButton : Button
        private lateinit var timePicker: TimePicker
        private lateinit var dataView: RecordDataView
        private lateinit var chosenType: String

        inner class TypeButton(type : String) : Button(activity) {
            init {
                text = type
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                setOnClickListener {
                    // set up the next page
                    dialogView.removeAllViews()
                    chosenType = type

                    timePicker = TimePicker(context)
                    dialogView.addView(timePicker)
                    dataView = RecordTypes.getConfig(type).getDataInputView(context)
                    dialogView.addView(dataView)

                    // show confirm button
                    confirmButton.visibility = View.VISIBLE
                }
            }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(context!!)


            dialogView = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                gravity = Gravity.CENTER

                for (type in RecordTypes.getTypes())
                    addView(TypeButton(type))

                // TODO fix this, this is a hack to get the keyboard to appear on the second page of the dialog
                // I don't know why this works
                addView(EditText(context))
            }


            builder.setView(dialogView)
                .setPositiveButton("Confirm") { _, _ ->
                        val time = LocalTime.of(timePicker.hour, timePicker.minute)
                        val data = dataView.data

                        onConfirm(time, chosenType, data)
                    }
                // default behavior on cancel, do nothing
                .setNegativeButton("Cancel") {_, _ -> }

            // Create the AlertDialog object and return it

            return builder.create().apply {
                // button isn't available until dialog is shown
                setOnShowListener {
                    confirmButton = getButton(AlertDialog.BUTTON_POSITIVE)
                    // by default have the confirm button be invisible, until on the second page
                    confirmButton.visibility = GONE
                }
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
    private lateinit var fm: androidx.fragment.app.FragmentManager
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
            onConfirm = { time, type, data ->

                // get complete dateTime based on current selected year, month, and day
                val yearMonth = calendarView.yearMonthShown
                val date = LocalDate.of(yearMonth.year, yearMonth.month, selectedDayOFMonth!!)
                val dateTime = OffsetDateTime.of(date, time, OffsetDateTime.now().offset)
                Thread {
                    try {
                        // add the new record retrieved from the dialog
                        recordDao.insert(Record(dateTime, type, data))
                    } catch (e : SQLiteConstraintException) {
                        // TODO error message,
                        //  this happens when multiple records of the same type are added for the same clock minute
                    }

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
            // TODO make sure this is done before using recordDao
            recordDao = RecordDatabase.instance.recordDao()
        }.start()

        inflater = inflaterArg
        tabView = inflater.inflate(R.layout.fragment_history, container, false) as ScrollView

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

//        Thread {
//            Thread.sleep(2000)
//            activity!!.runOnUiThread {
//                val x = findNavController().navigateUp().toString()
//
//                Log.d("debug", x)
//            }
//        }.start()
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
