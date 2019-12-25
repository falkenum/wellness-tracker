package com.sjfalken.wellnesstracker

import android.content.Context
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView


class RatingLayout(context: Context) : LinearLayout(context), EntryDatumHolder {
    companion object {
        const val HIGHLIGHTED = 99
        const val NOT_HIGHLIGHTED = 0
    }
    override val value: String
        get() = getChildAt(selectedIndex).id.toString()

    inner class NumberView(shownNum : Int) : TextView(context) {
        var highlighted = false
            set(value) {
                field = value
                if (value) background.alpha = HIGHLIGHTED
                else background.alpha = NOT_HIGHLIGHTED
            }

        init {
            id = shownNum

            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            val padding = 40
            setPadding(padding, 0, padding, 0)
            text = if (shownNum > 0) "+$shownNum" else shownNum.toString()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)

            // Color used to highlight
            setBackgroundColor(resources.getColor(R.color.colorPrimary, null))

            // not selected by default
            highlighted = false

            setOnClickListener {
                // remove old highlight
                (getChildAt(selectedIndex) as NumberView).highlighted = false

                // change selected to be this numberView
                selectedIndex = id + 2

                // add new highlight
                highlighted = true
            }
        }
    }

    private var selectedIndex : Int = 2

    init {
        orientation = HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

        // populating with number views
        for (i in -2..2)  addView(NumberView(i))

        (getChildAt(selectedIndex) as NumberView).highlighted = true
    }

}
