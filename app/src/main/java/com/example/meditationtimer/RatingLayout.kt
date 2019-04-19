package com.example.meditationtimer

import android.content.Context
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView

const val HIGHLIGHTED = 100
const val NOT_HIGHLIGHTED = 0

class RatingLayout(context: Context) : LinearLayout(context) {
    inner class NumberView(shownNum : Int) : TextView(context) {

        init {
            text = shownNum.toString()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)

            // Color used to highlight
            background = context.getDrawable(R.color.colorPrimary)

            // not selected by default
            background.alpha = NOT_HIGHLIGHTED

            setOnClickListener {
                // use the index of this textview as the number selected, add one to offset zero index
                val parent = parent as LinearLayout
                val index = parent.indexOfChild(this)
                val number = index + 1

                // remove old highlight
                parent.getChildAt(selectedNumber - 1).background.alpha = NOT_HIGHLIGHTED

                // add new highlight
                background.alpha = HIGHLIGHTED
            }
        }
    }

    var selectedNumber : Int = 3
    init {
        orientation = HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

        // populating with number views
        for (i in 1..5)  addView(NumberView(i))
    }
}
