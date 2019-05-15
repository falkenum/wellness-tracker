package com.example.meditationtimer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import org.w3c.dom.Text
import java.time.OffsetDateTime

class HomeFragment : Fragment() {
    private lateinit var rootView : View
    private lateinit var dataInputView : RecordDataInputView
    private lateinit var spinner : Spinner

    private fun setInputType(type : String) {
        dataInputView = RecordTypes.getConfig(type).getDataInputView(activity!!)
        rootView.findViewById<FrameLayout>(R.id.dataInputHolder).apply {
            removeAllViews()
            addView(dataInputView)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_home, container, false)

        spinner = rootView.findViewById<Spinner>(R.id.spinner).apply {
            adapter = ArrayAdapter<String>(
                activity!!,
                android.R.layout.simple_spinner_item,
                RecordTypes.getTypes()
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    (view as? TextView)?.text?.toString()?.also { type ->
                        setInputType(type)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) { }
            }
        }

        savedInstanceState?.getInt(SELECTED_ITEM_POS)?.also { spinnerPosition ->
            spinner.setSelection(spinnerPosition)
        }

        rootView.findViewById<Button>(R.id.confrimButton).setOnClickListener {
            val newRecord = Record(OffsetDateTime.now(), spinner.selectedItem as String, dataInputView.getData())

            Thread {
                RecordDatabase.instance.recordDao().insert(newRecord)

            }.start()

            Toast.makeText(activity!!, "Record added", Toast.LENGTH_SHORT).show()
        }



        return rootView
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(SELECTED_ITEM_POS, spinner.selectedItemPosition)
    }

    companion object SavedInstanceItems {
        const val SELECTED_ITEM_POS = "com.example.meditationtimer.SELECTED_ITEM_POS"
    }
}