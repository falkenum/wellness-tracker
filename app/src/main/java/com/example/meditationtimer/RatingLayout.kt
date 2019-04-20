package com.example.meditationtimer

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

const val HIGHLIGHTED = 99
const val NOT_HIGHLIGHTED = 0

class RatingLayout(context: Context) : LinearLayout(context) {
    val numbers = ArrayList<NumberView> ()

    inner class NumberView(shownNum : Int) : TextView(context) {
        var highlighted = false
            set(value) {
                field = value
                if (value) background.alpha = HIGHLIGHTED
                else background.alpha = NOT_HIGHLIGHTED
            }

        init {
            numbers.add(this)
            id = shownNum

            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            val padding = 10
            setPadding(padding, 0, padding, 0)
            text = shownNum.toString()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f)

            // Color used to highlight
            setBackgroundColor(resources.getColor(R.color.colorPrimary, null))

            // not selected by default
            highlighted = false

            setOnClickListener {
                // remove old highlight
                numbers[selectedNumber - 1].highlighted = false

                // change selected to be this numberView
                selectedNumber = id

                // add new highlight
                highlighted = true
            }
        }
    }

    var selectedNumber : Int = 3

    init {
        orientation = HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {asdf

        // populating with number views
        for (i in 1..5)  addView(NumberView(i))

        // highlight number 3 by default
        numbers[2].highlighted = true
    }
}
