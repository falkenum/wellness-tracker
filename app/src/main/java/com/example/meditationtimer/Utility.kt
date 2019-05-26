package com.example.meditationtimer

import android.content.Context
import android.util.TypedValue
import kotlin.math.roundToInt

class Utility {
    companion object {
        fun dpToPx(context : Context, dp : Int) : Int {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
                context.resources.displayMetrics).roundToInt()
        }
    }
}