package com.example.meditationtimer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import java.time.OffsetDateTime
import java.time.ZonedDateTime


class NewEntryFragment : Fragment(), TabLayout.OnTabSelectedListener {

    companion object ARGUMENT_KEYS {
        const val ENTRY_TYPE = "com.example.meditationtimer.ENTRY_TYPE"
        const val DATE_TIME = "com.example.meditationtimer.DATE_TIME"
    }

    lateinit var dataInputView : RecordDataInputView
    lateinit var rootView : View

    private fun updateDataInputType(type : String) {

        dataInputView = RecordTypes.getConfig(type).getDataInputView(activity!!)

        rootView.findViewById<FrameLayout>(R.id.dataInputHolder)!!.run {
            removeAllViews()
            addView(dataInputView)
        }
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        updateDataInputType(tab!!.text.toString())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_new_entry, container, false)

        // gets the type arg specified or defaults to drug use
        val entryType = (activity as MainActivity).selectedType
        updateDataInputType(entryType)

        val dateTime = arguments?.run {
            getString(DATE_TIME)
        } ?: ZonedDateTime.now()

        activity!!.findViewById<TabLayout>(R.id.tabLayout).run {
            addOnTabSelectedListener(this@NewEntryFragment)
        }

        rootView.findViewById<Button>(R.id.confirmButton).setOnClickListener {
            val newRecord = Record(OffsetDateTime.now(), entryType, dataInputView.data)

            Thread {
                RecordDatabase.instance.recordDao().insert(newRecord)
            }.start()

            Toast.makeText(activity!!, "Record added", Toast.LENGTH_SHORT).show()

            findNavController().navigateUp()
        }

        return rootView
    }
}
