package com.sjfalken.wellnesstracker

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.TypedValue
import androidx.fragment.app.DialogFragment
import kotlin.math.roundToInt

class Utility {
    companion object {
        fun dpToPx(context : Context, dp : Int) : Int {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
                context.resources.displayMetrics).roundToInt()
        }
    }

    open class InfoDialogFragment : DialogFragment() {
        protected open val messageTag = "INFO"

        var message = "[empty message]"
        val seperator = ": "

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity!!.let {
                val builder = AlertDialog.Builder(it)

                builder.setMessage(messageTag + seperator + message)
                    .setPositiveButton("Ok",
                        DialogInterface.OnClickListener { _, _ -> })
                builder.create()
            }
        }
    }

    class ErrorDialogFragment : InfoDialogFragment() {
        override val messageTag = "ERROR"
    }

    class DebugDialogFragment : InfoDialogFragment() {
        override val messageTag = "DEBUG"
    }

}

