package com.example.meditationtimer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import java.time.OffsetDateTime

class HomeFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)
        val dataInputView = RecordTypes.getConfig(RecordTypes.DRUG_USE).getDataInputView(activity!!)

        rootView.findViewById<FrameLayout>(R.id.dataInputHolder).addView(dataInputView)
        rootView.findViewById<Button>(R.id.confrimButton).setOnClickListener {
            val newRecord = Record(OffsetDateTime.now(), RecordTypes.DRUG_USE, dataInputView.getData())

            Thread {
                RecordDatabase.instance.recordDao().insert(newRecord)
            }.start()
        }

        return rootView
    }
}