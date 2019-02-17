package com.example.meditationtimer

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

class HistoryFragment : Fragment() {

    fun getCell() : TextView {
        return TextView(activity).apply{ text = "0" }
    }

    fun getRow() : TableRow {
        val row = TableRow(activity)

        for (i in 0..6) {
            row.addView(getCell())
        }

        return row
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val tabView = inflater.inflate(R.layout.tab_history, container, false)
        val calendar = tabView.findViewById<TableLayout>(R.id.calendarTable)

        for (i in 0..3) {
            calendar.addView(getRow())
        }

        return tabView
    }
}

