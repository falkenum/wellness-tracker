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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_new_entry.view.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

class NewEntryFragment : BaseFragment() {
    class ArgumentKeys {
        companion object {
            const val ENTRY_TYPE = "com.sjfalken.wellnesstracker.ENTRY_TYPE"
            const val DATE_TIME = "com.sjfalken.wellnesstracker.DATE_TIME"
        }
    }

    private lateinit var dataInputLayout : EntryDataLayout

    private var selectedTime = ZonedDateTime.now().toLocalTime()
    private var selectedDate = ZonedDateTime.now().toLocalDate()
    private var selectedType = ""

    private fun updateDataInputType(type : String) {
        context!!.run {
            dataInputLayout = EntryTypes.getConfig(type).getDataInputLayout(this)
        }


        view!!.dataInputHolder.run {
            removeAllViews()
            addView(dataInputLayout)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_entry, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.confirmButton.setOnClickListener {
            onConfirm()
        }

        view.changeDateButton.setOnClickListener {
            val fm = childFragmentManager
            EntryDatePicker().apply {
                onDateSet = { date -> selectedDate = date }
                dateValueView = view.findViewById(R.id.dateValueView)
                show(fm, "DatePickerDialog")
            }
        }

        view.changeTimeButton.setOnClickListener {
            val fm = childFragmentManager
            EntryTimePicker().apply {
                onTimeSet = { time -> selectedTime = time}
                timeValueView = view.findViewById(R.id.timeValueView)
                show(fm, "TimePickerDialog")
            }
        }

        view.singleTypeListView.apply {
            choiceMode = ListView.CHOICE_MODE_SINGLE
            adapter = ArrayAdapter(context!!, android.R.layout.simple_list_item_single_choice,
                EntryTypes.getTypes())
            divider = null

            setOnItemClickListener { _, _, position, _ ->
                val type = EntryTypes.getTypes()[position]
                updateDataInputType(type)
                selectedType = type
            }

            val type = arguments?.getString(ArgumentKeys.ENTRY_TYPE)
            val pos = if (type == null) 0 else EntryTypes.getTypes().indexOf(type)
            performItemClick(this, pos, 0)
        }
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

    private fun onConfirm() {
        val localDateTime = LocalDateTime.of(selectedDate, selectedTime)
        val dateTime = ZonedDateTime.of(localDateTime, ZoneId.systemDefault())
        val newEntry = Entry(dateTime, selectedType, dataInputLayout.data)
        Thread {
            if (!Entry.isValidEntry(newEntry)) {
                Utility.ErrorDialogFragment().apply {
                    message = "Could not add invalid entry $newEntry"
                }.show(childFragmentManager, null)
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
