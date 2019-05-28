package com.example.meditationtimer

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.lang.Exception
import java.time.*
import java.time.format.DateTimeFormatter


open class RecordDataView(context: Context, startingData : JSONObject)
    : LinearLayout(context) {

    protected val textSizeSp = 18f
    private val labelSuffix = ':'
    private val labelIndex = 0
    private val valueIndex = 2

    val data : JSONObject
        get() {
            return JSONObject().apply {

                for (rowIndex in 0 until childCount) {
                    val row = getChildAt(rowIndex) as LinearLayout

                    val labelView = row.getChildAt(labelIndex) as TextView
                    val valueView = row.getChildAt(valueIndex) as TextView

                    // remove colon at the end
                    val label = labelView.text.toString().filter { it != labelSuffix }
                    val value = valueView.text.toString()

                    put(label, value)
                }
            }
        }
    fun put(key : String, value : String) {
        // find the row with this key
        for (childIndex in 0 until childCount) {
            val currentRow = getChildAt(childIndex) as LinearLayout
            val labelView = currentRow.getChildAt(labelIndex) as TextView
            val label = labelView.text.toString()

            // if this is the row with the key
            if (label == key) {
                val valueView = currentRow.getChildAt(valueIndex) as TextView
                valueView.text = value
            }
        }
    }

    private fun getLabelView(label : String) : TextView {
        return TextView(context).apply {
//            layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f)
            text = label + labelSuffix
//            setBackgroundColor(context.getColor(R.color.colorAccent))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
        }
    }

    // to suppress a warning
    final override fun addView(child: View?) {
        super.addView(child)
    }

    protected open fun getValueView(value : String) : TextView {
        return TextView(context).apply {
            //                setBackgroundColor(context.getColor(R.color.colorAccent))
            text = value
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
        }
    }

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

        for (label in startingData.keys()) {
            val newRow = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    // I don't understand why this works, but it makes the children have RIGHT gravity too
                    gravity = Gravity.END
                }

                val value = startingData.getString(label)
                val spaceView = Space(context).apply { minimumWidth = Utility.dpToPx(context, 10) }

                addView(getLabelView(label))
                addView(spaceView)
                addView(getValueView(value))
            }

            addView(newRow)
        }
    }
}

class RecordDataInputView(context: Context, startingData : JSONObject)
    : RecordDataView(context, startingData) {
    override fun getValueView(value: String) : TextView {
        return EditText(context).apply {
            width = Utility.dpToPx(context, 100)
            text.insert(0, value)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
        }
    }
}


abstract class RecordTypeConfig {

    abstract val defaultData : JSONObject

    abstract fun getBgColor(context: Context) : Int
    abstract fun getDailyReminderTimes(): List<LocalTime>?

    fun getDataView(record: Record, context: Context): View {
        return RecordDataView(context, record.data)
    }

    open fun getDataInputView(context: Context): RecordDataInputView {
        return RecordDataInputView(context, defaultData)
    }
}

class MeditationConfig: RecordTypeConfig() {
    companion object {
        const val DURATION = "duration"
    }

    override val defaultData : JSONObject = JSONObject(mapOf(DURATION to "10"))

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

    override val defaultData : JSONObject = JSONObject(mapOf(RATING to "3"))

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

    override val defaultData : JSONObject = JSONObject(mapOf(
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
        LayoutInflater.from(context).inflate(R.layout.view_entry_card, this, true)
    }

    fun insertRecordData(record : Record) {
        val timeStamp = record.dateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
        val titleStr = "${record.type} at $timeStamp"
        val bgColor = RecordTypes.getConfig(record.type).getBgColor(context)
        val dataView = RecordTypes.getConfig(record.type).getDataView(record, context)

        findViewById<TextView>(R.id.recordTitle).text = titleStr
        findViewById<LinearLayout>(R.id.recordDataLayout).addView(dataView)

        (getChildAt(0) as androidx.cardview.widget.CardView).apply {
            // set card color for the specific type
            val entryCardIcon = findViewById<FrameLayout>(R.id.entryCardIconHolder).background as GradientDrawable
            entryCardIcon.setColor(bgColor)

        }
    }

    fun setOnDelete(onDelete: () -> Unit) {
        findViewById<Button>(R.id.deleteButton).setOnClickListener {
            onDelete()
        }
    }
}
