package com.example.meditationtimer

import android.content.Context
import android.text.Editable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import org.json.JSONObject
import java.lang.Exception
import java.nio.ReadOnlyBufferException
import java.time.*
import java.time.format.DateTimeFormatter


class RecordDataView(context: Context, startingData : JSONObject, val readOnly : Boolean)
    : LinearLayout(context) {

    private val textSizeSp = 18f

    val data : JSONObject
        get() {
            return JSONObject()
        }

    private fun getLabelView(label : String) : TextView {
        return TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            text = label + ": "
//            setBackgroundColor(context.getColor(R.color.colorAccent))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
        }
    }

    private fun getValueView(value : String) : TextView {
        val view = if (readOnly) {
            return TextView(context).apply {
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                text = value
//                setBackgroundColor(context.getColor(R.color.colorAccent))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
            }
        }
        else {
            EditText(context).apply {
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    //            inputType = TextView.LAYER_TYPE_NONE

//                setBackgroundColor(context.getColor(R.color.colorPrimary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
            }
        }

        return view
    }

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

        for (label in startingData.keys()) {
            val newRow = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

                val value = startingData.getString(label)
                addView(getLabelView(label))
                addView(getValueView(value))
            }

            addView(newRow)
        }
    }
}


abstract class RecordTypeConfig {

    abstract val defaultData : JSONObject

    abstract fun getBgColor(context: Context) : Int
    abstract fun getDailyReminderTimes(): List<LocalTime>?

    fun getDataView(record: Record, context: Context): View {
        return RecordDataView(context, record.data, true)
    }

    open fun getDataInputView(context: Context): RecordDataView {
        return RecordDataView(context, defaultData, false)
    }
}

class MeditationConfig: RecordTypeConfig() {
    companion object {
        const val DURATION = "duration"
    }

    override val defaultData : JSONObject = JSONObject(mutableMapOf(DURATION to "10"))

    override fun getBgColor(context: Context): Int {
        return context.resources.getColor(R.color.colorMeditation, null)
    }

    override fun getDailyReminderTimes(): List<LocalTime>? {
        // 8am every day
        return List(1) { LocalTime.of(8, 0) }
    }
}

class MoodConfig : RecordTypeConfig() {
    companion object {
        const val RATING = "rating"
    }

    override val defaultData : JSONObject = JSONObject(mutableMapOf(RATING to "3"))

    override fun getBgColor(context: Context): Int {
        return context.resources.getColor(R.color.colorMood, null)
    }

    override fun getDailyReminderTimes(): List<LocalTime>? {
        // 10am and 6pm
        return List(2) {
            when (it) {
                0 -> LocalTime.of(10, 0)
                1 -> LocalTime.of(18, 0)
                else -> throw Exception("shouldn't happen")
            }
        }
    }
}

class DrugUseConfig : RecordTypeConfig() {
    companion object {
        const val SUBSTANCE = "substance"
        const val FORM = "form"
        const val QUANTITY_GRAMS = "quantity"
    }

    override val defaultData : JSONObject = JSONObject(mutableMapOf(
        SUBSTANCE to "Cannabis",
        FORM to "wax",
        QUANTITY_GRAMS to "0.1"
    ))

    override fun getBgColor(context: Context): Int {
        return context.resources.getColor(R.color.colorDrugUse, null)
    }

    override fun getDailyReminderTimes(): List<LocalTime>? = null
}

class RecordTypes {

    companion object {
        const val MEDITATION = "Meditation"
        const val MOOD = "Mood"
        const val DRUG_USE = "Drug use"

        private val recordTypeConfigs : HashMap<String, RecordTypeConfig> = hashMapOf(
            MEDITATION to MeditationConfig(),
            MOOD to MoodConfig(),
            DRUG_USE to DrugUseConfig()
        )

        fun getTypes() : List<String> {
            return recordTypeConfigs.keys.toList()
        }

        fun getConfig(type : String) : RecordTypeConfig {
            return recordTypeConfigs[type]!!
        }
    }
}

class RecordCardView(context: Context) : androidx.cardview.widget.CardView(context) {

    init {
        // TODO remove redundant CardView in the hierarchy
        LayoutInflater.from(context).inflate(R.layout.view_session_record_card, this, true)
    }

    fun insertRecordData(record : Record) {
        val timeStamp = record.dateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
        val titleStr = "${record.type} at $timeStamp"
        val bgColor = RecordTypes.getConfig(record.type).getBgColor(context)
        val dataView = RecordTypes.getConfig(record.type).getDataView(record, context)

        findViewById<TextView>(R.id.recordTitle).text = titleStr
        findViewById<LinearLayout>(R.id.recordDataLayout).addView(dataView)

        (getChildAt(0) as androidx.cardview.widget.CardView).setCardBackgroundColor(bgColor)
    }

    fun setOnDelete(onDelete: () -> Unit) {
        findViewById<Button>(R.id.deleteButton).setOnClickListener {
            onDelete()
        }
    }
}
