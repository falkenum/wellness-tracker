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
import java.time.OffsetDateTime

class NewEntryFragment : Fragment() {
    companion object ARGUMENT_KEYS {
        const val ENTRY_TYPE = "com.example.meditationtimer.ENTRY_TYPE"
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_new_entry, container, false)

        // gets the type arg specified or defaults to drug use
        val entryType = arguments?.run {
            getString(ENTRY_TYPE)
        } ?: RecordTypes.DRUG_USE

        val dataInputView = RecordTypes.getConfig(entryType).getDataInputView(activity!!)
        rootView.findViewById<FrameLayout>(R.id.dataInputHolder).apply {
            removeAllViews()
            addView(dataInputView)
        }

        rootView.findViewById<Button>(R.id.confrimButton).setOnClickListener {
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
