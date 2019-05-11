package com.example.meditationtimer

import android.content.Context
import androidx.cardview.widget.CardView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.lang.Exception
import java.time.*
import java.time.format.DateTimeFormatter


abstract class RecordDataInputView(context: Context) : FrameLayout(context) {
    init {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }
    abstract fun getData() : JSONObject
}


abstract class RecordTypeConfig {
//    class ReminderProtocol(notification: Notification, dailyReminderTimes : List<LocalTime>)

    abstract fun getBgColor(context: Context) : Int
    abstract fun getDataView(record : Record, context : Context) : View
    abstract fun getDataInputView(context: Context) : RecordDataInputView
    abstract fun getDailyReminderTimes(): List<LocalTime>?
}

class MeditationConfig: RecordTypeConfig() {
    override fun getBgColor(context: Context): Int {
        return context.resources.getColor(R.color.colorMeditation, null)
    }

    override fun getDataView(record: Record, context: Context): View {
        return TextView(context).apply {
            val duration = Duration.parse(record.data.getString("duration"))
            text = "duration: ${duration.toMinutes()} min"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        }
    }

    override fun getDataInputView(context: Context): RecordDataInputView {
        return object : RecordDataInputView(context) {
            init {
                LayoutInflater.from(context)
                    .inflate(R.layout.view_meditation_data_input, this, true)
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            }
            override fun getData(): JSONObject {
                val durationView = findViewById<TextView>(R.id.durationView)
                val duration = Duration.ofMinutes(durationView.text.toString().toLong())
                return JSONObject().apply { put("duration", duration) }
            }
        }
    }

    override fun getDailyReminderTimes(): List<LocalTime>? {
        // 8am every day
        return List(1) { LocalTime.of(8, 0) }
    }
}

class MoodConfig : RecordTypeConfig() {
    override fun getBgColor(context: Context): Int {
        return context.resources.getColor(R.color.colorMood, null)
    }

    override fun getDataView(record: Record, context: Context): View {
        return TextView(context).apply {
            text = "rating: ${record.data.getInt("rating")}"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        }
    }

    override fun getDataInputView(context: Context): RecordDataInputView {
        return object : RecordDataInputView(context) {
            init {
                // getting a rating view where the user selects a number 1 to 5
                addView(RatingLayout(context))
            }
            override fun getData(): JSONObject {
                val rating = (getChildAt(0) as RatingLayout).selectedNumber
                return JSONObject().apply { put("rating", rating)}
            }
        }
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

    override fun getBgColor(context: Context): Int {
        return context.resources.getColor(R.color.colorDrugUse, null)
    }

    override fun getDataView(record: Record, context: Context): View {
        return TextView(context).apply {
            val substance = record.data.getString(SUBSTANCE)
            val form = record.data.getString(FORM)
            val quantity = record.data.getDouble(QUANTITY_GRAMS)

            text = "substance: $substance\nform: $form\nquantity: $quantity"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        }
    }

    override fun getDataInputView(context: Context): RecordDataInputView {
        return object : RecordDataInputView(context) {
            init {
                LayoutInflater.from(context)
                    .inflate(R.layout.view_drug_use_data_input, this, true)
            }
            override fun getData(): JSONObject {
                val substance = findViewById<EditText>(R.id.substanceView).text
                val form = findViewById<EditText>(R.id.formView).text
                val quantity = findViewById<EditText>(R.id.quantityView).text.toString().toDouble()
                return JSONObject().apply {
                    put(SUBSTANCE, substance)
                    put(FORM, form)
                    put(QUANTITY_GRAMS, quantity)
                }
            }
        }
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
