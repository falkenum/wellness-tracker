package com.example.meditationtimer

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.lang.Exception
import java.lang.String.format
import java.time.*
import java.time.format.DateTimeFormatter


open class EntryDataView(context: Context, startingData : JSONObject)
    : LinearLayout(context) {

    protected val textSizeSp = 18f
    private val labelSuffix = ": "
    private val labelIndex = 0
    private val valueIndex = 1

    val data : JSONObject
        get() {
            return JSONObject().apply {

                for (rowIndex in 0 until childCount) {
                    val row = getChildAt(rowIndex) as LinearLayout

                    val labelView = row.getChildAt(labelIndex) as TextView
                    val valueView = row.getChildAt(valueIndex) as TextView

                    // remove suffix
                    val label = labelView.text.toString().filterIndexed { index, c ->
                        index < length() - labelSuffix.length
                    }
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
            text = label + labelSuffix
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
        }
    }

    // to suppress a warning
    final override fun addView(child: View?) {
        super.addView(child)
    }

    protected open fun getValueView(value : String) : TextView {
        return TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)

            val valueNumeric = value.toDoubleOrNull()

            //if it's a floating point, limit decimal places
            text = if (valueNumeric != null) {
                format("%.2f", valueNumeric)
            }
            else {
                value
            }
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
                gravity = Gravity.START
            }

                val value = startingData.getString(label)

//                val spaceView = Space(context).apply { minimumWidth = Utility.dpToPx(context, 10) }

                addView(getLabelView(label))
//                addView(spaceView)
                addView(getValueView(value))
            }

            addView(newRow)
        }
    }
}

class EntryDataInputView(context: Context, startingData : JSONObject)
    : EntryDataView(context, startingData) {
    override fun getValueView(value: String) : TextView {
        return EditText(context).apply {
            width = Utility.dpToPx(context, 100)
            text.insert(0, value)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
        }
    }
}


abstract class EntryTypeConfig {

    abstract val defaultData : JSONObject

    abstract fun getBgColor(context: Context) : Int
    abstract fun getDailyReminderTimes(): List<LocalTime>?

    fun getDataView(entry: Entry, context: Context): View {
        return EntryDataView(context, entry.data)
    }

    open fun getDataInputView(context: Context): EntryDataInputView {
        return EntryDataInputView(context, defaultData)
    }
}

class MeditationConfig: EntryTypeConfig() {
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

class MoodConfig : EntryTypeConfig() {
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

class DrugUseConfig : EntryTypeConfig() {
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

class EntryTypes {

    companion object {
        const val MEDITATION = "Meditation"
        const val MOOD = "Mood"
        const val DRUG_USE = "Drug use"

        private val ENTRY_TYPE_CONFIGS : HashMap<String, EntryTypeConfig> = hashMapOf(
            MEDITATION to MeditationConfig(),
            MOOD to MoodConfig(),
            DRUG_USE to DrugUseConfig()
        )

        fun getTypes() : List<String> {
            return ENTRY_TYPE_CONFIGS.keys.toList()
        }

        fun getConfig(type : String) : EntryTypeConfig {
            return ENTRY_TYPE_CONFIGS[type]!!
        }
    }
}

class EntryCardView(context: Context) : androidx.cardview.widget.CardView(context) {

    init {
        // TODO remove redundant CardView in the hierarchy
        LayoutInflater.from(context).inflate(R.layout.view_entry_card, this, true)
    }

    fun insertEntryData(entry : Entry) {
        val timeStamp = entry.dateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
        val titleStr = "${entry.type} at $timeStamp"
        val bgColor = EntryTypes.getConfig(entry.type).getBgColor(context)
        val dataView = EntryTypes.getConfig(entry.type).getDataView(entry, context)

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
