package com.sjfalken.wellnesstracker

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_new_entry.view.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

class NewEntryFragment : BaseFragment() {
    class ArgumentKeys {
        companion object {
            const val NEW_ENTRY_TYPE = "com.sjfalken.wellnesstracker.ENTRY_TYPE"
            const val DATE = "com.sjfalken.wellnesstracker.DATE"
        }
    }

    class SingleTypeDialogFragment: DialogFragment() {
        lateinit var onConfirm: (Int) -> Unit
        var selectedPos = 0
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return AlertDialog.Builder(context!!)
                .setSingleChoiceItems(EntryTypes.getTypes().toTypedArray(), selectedPos) { _, which ->
                    selectedPos = which
                }
                .setPositiveButton("Ok") { _, _ ->
                    onConfirm(selectedPos)
                }
                .create()
        }
    }

    private lateinit var dataInputLayout : JSONObjectLayout

    private var selectedTime = ZonedDateTime.now().toLocalTime()
    private var selectedDate = ZonedDateTime.now().toLocalDate()
    private var selectedType = ""

    private fun updateDataInputType(type : String) {
        selectedType = type

        context!!.run {
            dataInputLayout = EntryTypes.getConfig(type)!!.getDataInputLayout(this)
        }


        view!!.dataInputHolder.run {
            removeAllViews()
            addView(dataInputLayout)
        }

        view!!.typeValueView.text = type
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_entry, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.confirmButton.setOnClickListener {
            onConfirm()
        }

        val dateStrArg = arguments?.getString(ArgumentKeys.DATE)
        val entryDatePicker = EntryDatePicker().apply {
            onConfirm = { date -> selectedDate = date }
            dateValueView = view.findViewById(R.id.dateValueView)


            if (dateStrArg != null) {
                val date = LocalDate.parse(dateStrArg)
                onDateSet(null, date.year, date.monthValue - 1, date.dayOfMonth)
            }
        }
        view.changeDateButton.setOnClickListener {
            entryDatePicker.show(childFragmentManager, "DatePickerDialog")
        }

        view.changeTimeButton.setOnClickListener {
            val fm = childFragmentManager
            EntryTimePicker().apply {
                onTimeSet = { time -> selectedTime = time}
                timeValueView = view.findViewById(R.id.timeValueView)
                show(fm, "TimePickerDialog")
            }
        }

        (arguments?.getString(ArgumentKeys.NEW_ENTRY_TYPE) ?: EntryTypes.getTypes()[0]).run {
            updateDataInputType(this)
        }

        view.changeTypeButton.setOnClickListener {
            val fm = childFragmentManager
            SingleTypeDialogFragment().apply {
                onConfirm = { position ->
                    val type = EntryTypes.getTypes()[position]
                    updateDataInputType(type)
                }

                selectedPos = EntryTypes.getTypes().indexOf(selectedType)
                show(fm, "SingleTypeDialogFragment")
            }
        }

    }

    class EntryDatePicker : DialogFragment(), DatePickerDialog.OnDateSetListener {
        lateinit var dateValueView : TextView
        lateinit var onConfirm : (date : LocalDate) -> Unit
        override fun onDateSet(view: DatePicker?, year: Int, monthIndex: Int, dayOfMonth: Int) {
            val month = monthIndex + 1
            val date = LocalDate.of(year, month, dayOfMonth)
            onConfirm(date)
            dateValueView.text = date.format(DateTimeFormatter.ofPattern("MMM dd, uuuu"))
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the current date as the default date in the picker
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            // Create a new instance of DatePickerDialog and return it
            return DatePickerDialog(context!!, this, year, month, day)
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
