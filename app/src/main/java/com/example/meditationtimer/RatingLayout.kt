package com.example.meditationtimer

import android.content.Context
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import java.lang.NumberFormatException

const val HIGHLIGHTED = 100
const val NOT_HIGHLIGHTED = 0

class RatingLayout(context: Context) : LinearLayout(context) {
    inner class NumberView(shownNum : Int) : TextView(context) {
        var highlighted = false
            set(value) {
                field = value
                if (value) background.alpha = HIGHLIGHTED
                else background.alpha = NOT_HIGHLIGHTED
            }

        init {
            setPadding(5, 5, 5, 5)
            text = shownNum.toString()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)

            // Color used to highlight
            background = context.getDrawable(R.color.colorPrimary)

            // not selected by default
            highlighted = false

            setOnClickListener {
                // use the index of this textview as the number selected, add one to offset zero index
                val parent = parent as LinearLayout
                val index = parent.indexOfChild(this)
                selectedNumber = index + 1

                // remove old highlight
                val currentNumber = parent.getChildAt(selectedNumber - 1) as NumberView
                currentNumber.highlighted = false

                // add new highlight
                highlighted = true
            }
        }
    }

    var selectedNumber : Int = 3

    init {
        orientation = HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

        // populating with number views
        for (i in 1..5)  addView(NumberView(i))

        // highlight number 3 by default
        (getChildAt(2) as NumberView).highlighted = true
    }
}
