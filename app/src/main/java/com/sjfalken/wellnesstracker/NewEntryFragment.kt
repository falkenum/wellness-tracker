package com.sjfalken.wellnesstracker

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*


class NewEntryFragment : BaseFragment() {

    companion object ARGUMENT_KEYS {
        const val ENTRY_TYPE = "com.sjfalken.wellnesstracker.ENTRY_TYPE"
        const val DATE_TIME = "com.sjfalken.wellnesstracker.DATE_TIME"
    }

    lateinit var dataInputView : EntryDataInputView
    lateinit var rootView : View
    lateinit var fm : FragmentManager

    private var selectedTime = ZonedDateTime.now().toLocalTime()
    private var selectedDate = ZonedDateTime.now().toLocalDate()

    private fun updateDataInputType(type : String) {
        activity?.run {
            dataInputView = EntryTypes.getConfig(type).getDataInputView(this)
        }


        rootView.findViewById<FrameLayout>(R.id.dataInputHolder)!!.run {
            removeAllViews()
            addView(dataInputView)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_new_entry, container, false)
        fm = activity!!.supportFragmentManager

        val dateTime = arguments?.run {
            getString(DATE_TIME)
        } ?: ZonedDateTime.now()

        val selectedType = (activity!! as MainActivity).run {
            addOnTabSelectedAction { tab ->
                updateDataInputType(tab.text.toString())
            }
            selectedType
        }

        rootView.findViewById<Button>(R.id.confirmButton).setOnClickListener {
            onConfirm()
        }

        rootView.findViewById<Button>(R.id.modifyTimeButton).setOnClickListener {
            getTime()
        }

        rootView.findViewById<Button>(R.id.modifyDateButton).setOnClickListener {
            getDate()
        }

        // set the initial data input
        updateDataInputType(selectedType)

        return rootView
    }

    class EntryDatePicker : DialogFragment(), DatePickerDialog.OnDateSetListener {
        lateinit var dateValueView : TextView
        lateinit var onDateSet : (date : LocalDate) -> Unit
        override fun onDateSet(view: DatePicker?, year: Int, monthIndex: Int, dayOfMonth: Int) {
            val month = monthIndex + 1
            val date = LocalDate.of(year, month, dayOfMonth)
            onDateSet(date)
            dateValueView.text = date.format(DateTimeFormatter.ofPattern("MMM dd, uuuu"))
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the current date as the default date in the picker
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            // Create a new instance of DatePickerDialog and return it
            return DatePickerDialog(activity!!, this, year, month, day)
        }
    }
    private fun getDate() {
        EntryDatePicker().apply {
            onDateSet = { date -> selectedDate = date }
            dateValueView = rootView.findViewById(R.id.dateValueView)
            show(fm, "DatePickerDialog")
        }
    }

    class EntryTimePicker : DialogFragment(), TimePickerDialog.OnTimeSetListener {
        lateinit var timeValueView : TextView
        lateinit var onTimeSet : (time : LocalTime) -> Unit

        override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
            val time = LocalTime.of(hourOfDay, minute)
            onTimeSet(time)
            timeValueView.text = time.format(DateTimeFormatter.ofPattern("hh:mm a"))
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the current time as the default values for the picker
            val c = Calendar.getInstance()
            val hour = c.get(Calendar.HOUR_OF_DAY)
            val minute = c.get(Calendar.MINUTE)

            // Create a new instance of TimePickerDialog and return it
            return TimePickerDialog(activity, this,
                hour, minute, false)
        }
    }

    private fun getTime() {
        EntryTimePicker().apply {
            onTimeSet = { time -> selectedTime = time}
            timeValueView = rootView.findViewById(R.id.timeValueView)
            show(fm, "TimePickerDialog")
        }
    }

    private fun onConfirm() {
        val selectedType = (activity!! as MainActivity).selectedType
        val localDateTime = LocalDateTime.of(selectedDate, selectedTime)
        val dateTime = ZonedDateTime.of(localDateTime, ZoneId.systemDefault())
        val newEntry = Entry(dateTime, selectedType, dataInputView.data)

        Thread {
            if (!Entry.isValidEntry(newEntry)) {
                Utility.ErrorDialogFragment().apply {
                    message = "Could not add invalid entry $newEntry"
                }.show(fragmentManager!!, null)
            }
            else {
                LogEntryDatabase.instance.entryDao().insert(newEntry)
                activity!!.runOnUiThread {
                    Toast.makeText(activity!!, "Entry added", Toast.LENGTH_SHORT).show()

                    findNavController().navigateUp()
                }
            }
        }.start()
    }
}
