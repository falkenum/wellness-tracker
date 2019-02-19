package com.example.meditationtimer

import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.lang.Exception
import java.lang.String.format
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import kotlin.math.roundToInt


class HistoryFragment : Fragment() {

    private lateinit var tabView: ConstraintLayout

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        tabView = inflater.inflate(R.layout.tab_history, container, false) as ConstraintLayout
        return tabView
    }
}

