package com.example.meditationtimer

import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.w3c.dom.Attr
import java.time.OffsetDateTime
import java.time.ZonedDateTime


class NewEntryFragment : Fragment() {
    companion object ARGUMENT_KEYS {
        const val ENTRY_TYPE = "com.example.meditationtimer.ENTRY_TYPE"
        const val DATE_TIME = "com.example.meditationtimer.DATE_TIME"
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_new_entry, container, false)

        // gets the type arg specified or defaults to drug use
        val entryType = arguments?.run {
            getString(ENTRY_TYPE)
        } ?: RecordTypes.DRUG_USE

        val dateTime = arguments?.run {
            getString(DATE_TIME)
        } ?: ZonedDateTime.now()


        val dataInputView = RecordTypes.getConfig(entryType).getDataInputView(activity!!)
        val newEntryLayout = rootView.findViewById<LinearLayout>(R.id.newEntryLayout).apply {
            removeAllViews()
            addView(dataInputView)

            val confirmButton = Button(context, null, 0, R.style.button).apply {
                text = "Confirm"
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

                setOnClickListener {
                    val newRecord = Record(OffsetDateTime.now(), entryType, dataInputView.data)

                    Thread {
                        RecordDatabase.instance.recordDao().insert(newRecord)
                    }.start()

                    Toast.makeText(activity!!, "Record added", Toast.LENGTH_SHORT).show()

                    findNavController().navigateUp()
                }
            }

            addView(confirmButton)
        }

//        rootView.findViewById<Button>(R.id.confrimButton).

        return rootView
    }
}
