package com.example.meditationtimer

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlin.math.roundToInt

class Utility {
    companion object {
        fun dpToPx(context : Context, dp : Int) : Int {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
                context.resources.displayMetrics).roundToInt()
        }
    }
}

class DebugDialogFragment : DialogFragment() {

    var message = ""
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity!!.let {
            val builder = AlertDialog.Builder(it)

            builder.setMessage("DEBUG: $message")
                .setPositiveButton("Ok",
                    DialogInterface.OnClickListener { _, _ -> })
            builder.create()
        }
    }
}
