package com.sjfalken.wellnesstracker

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.lang.String.format
import java.time.*
import java.time.format.DateTimeFormatter

interface EntryDatumHolder{
    val value : String
}

class EntryDatumTextView(context: Context) : TextView(context), EntryDatumHolder {
    override val value: String
        get() = text.toString()
}

class EntryDatumEditText(context: Context) : EditText(context), EntryDatumHolder {
    override val value: String
        get() = text.toString()
}

open class EntryDataLayout(context: Context, private val startingData : JSONObject)
    : LinearLayout(context) {

    private val labelSuffix = ": "
    private val labelIndex = 0
    private val valueIndex = 2

    val data : JSONObject
        get() {
            return JSONObject().apply {

                for (rowIndex in 0 until childCount) {
                    val row = getChildAt(rowIndex) as LinearLayout

                    val labelView = row.getChildAt(labelIndex) as TextView
                    val valueView = row.getChildAt(valueIndex) as EntryDatumHolder
                    // remove suffix
                    val label = labelView.text.toString().removeSuffix(labelSuffix)
                    val value = valueView.value

                    put(label, value)
                }
            }
        }

    private fun getLabelView(label : String) : TextView {
        return TextView(context).apply {
            text = label + labelSuffix
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
        }
    }

    // to suppress a warning
    final override fun addView(child: View?) {
        super.addView(child)
    }

    protected open fun getValueView(value : String) : EntryDatumHolder {
        return EntryDatumTextView(context).apply {

            text = if (value.toIntOrNull() != null) value
            else if (value.toDoubleOrNull() != null) format("%.2f", value.toDouble())
            else value
        }
    }

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        for (label in startingData.keys()) {
            val newRow = LinearLayout(context).apply {
//                setBackgroundColor(R.color.colorAccent)
                orientation = HORIZONTAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

                val spaceView = Space(context).apply {
                    layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT).apply {
                        weight = 1f
                    }
                }

                val value = startingData.getString(label)
                addView(getLabelView(label))
                addView(spaceView)
                (getValueView(value) as View).apply { gravity = Gravity.END; addView(this) }
            }

            addView(newRow)
        }
    }
}


abstract class EntryTypeConfig {

    abstract val defaultData : JSONObject

    abstract fun getBgColor(context: Context) : Int
    abstract fun getDailyReminderTimes(): List<LocalTime>?

    class EntryDataTextInputLayout(context: Context, startingData : JSONObject)
        : EntryDataLayout(context, startingData) {
        override fun getValueView(value: String) : EntryDatumHolder {
            return EntryDatumEditText(context).apply {
                width = Utility.dpToPx(context, 100)
                text.insert(0, value)
                textAlignment = View.TEXT_ALIGNMENT_CENTER

                val valueNumeric = value.toDoubleOrNull()
                //if it's a number, change keyboard
                if (valueNumeric != null) {
                    inputType = InputType.TYPE_CLASS_NUMBER
                }
                else {
                    inputType = InputType.TYPE_CLASS_TEXT
                }
            }
        }
    }

    open fun getDataLayout(entry: Entry, context: Context): EntryDataLayout {
        return EntryDataLayout(context, entry.data)
    }

    open fun getDataInputLayout(context: Context): EntryDataLayout {
        return EntryDataTextInputLayout(context, defaultData)
    }
}

class MeditationConfig: EntryTypeConfig() {
    companion object {
        const val DURATION_MIN = "duration"
    }

    override val defaultData : JSONObject = JSONObject(mapOf(DURATION_MIN to "10"))

    override fun getBgColor(context: Context): Int {
        return context.resources.getColor(R.color.colorMeditation, null)
    }

    override fun getDailyReminderTimes(): List<LocalTime>? {
        // 8am every day
        return listOf(
            LocalTime.of(8, 0)
        )
    }
}

class MoodConfig : EntryTypeConfig() {
    companion object {
        const val RATING = "rating"
    }

    class EntryDataMoodInputLayout(context: Context, startingData : JSONObject)
        : EntryDataLayout(context, startingData) {
        override fun getValueView(value: String): EntryDatumHolder {
            return RatingLayout(context)
        }
    }

    override val defaultData : JSONObject = JSONObject(mapOf(RATING to "3"))

    override fun getBgColor(context: Context): Int {
        return context.resources.getColor(R.color.colorMood, null)
    }

    override fun getDailyReminderTimes(): List<LocalTime>? {
        return listOf(
            LocalTime.of(18, 0)
        )
    }

    override fun getDataInputLayout(context: Context): EntryDataLayout {
        return EntryDataMoodInputLayout(context, defaultData)
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
        FORM to "bowl",
        QUANTITY_GRAMS to "0.3"
    ))

    override fun getBgColor(context: Context): Int {
        return context.resources.getColor(R.color.colorDrugUse, null)
    }

    override fun getDailyReminderTimes(): List<LocalTime>? = listOf(
            LocalTime.of(20, 0)
        )

}

// list of exercises: exercise name, list of sets (number of reps, duration and weight added for each)
class WorkoutConfig : EntryTypeConfig() {
    companion object {
        const val DURATION = "duration"
        const val EXERCISES = "exercises"
    }

    class ExerciseConfig {
        companion object {
            const val NAME = "name"
            const val SETS = "sets"
        }

        class SetConfig {
            companion object {
                const val REPS = "reps"
                const val DURATION = "duration"
                const val WEIGHT = "weight"
            }
        }
    }

    override val defaultData : JSONObject = JSONObject(mapOf(
        DURATION to 60,
        EXERCISES to JSONArray(listOf(
            JSONObject(mapOf(
                ExerciseConfig.NAME to "running",
                ExerciseConfig.SETS to JSONArray(listOf(
                    JSONObject(mapOf(
                        ExerciseConfig.SetConfig.REPS to 1,
                        ExerciseConfig.SetConfig.DURATION to Duration.ofMinutes(10).toString(),
                        ExerciseConfig.SetConfig.WEIGHT to JSONObject.NULL
                    ))
                ))
            )),
            JSONObject(mapOf(
                ExerciseConfig.NAME to "pullups",
                ExerciseConfig.SETS to JSONArray(listOf(
                    JSONObject(mapOf(
                        ExerciseConfig.SetConfig.REPS to 5,
                        ExerciseConfig.SetConfig.DURATION to JSONObject.NULL,
                        ExerciseConfig.SetConfig.WEIGHT to 0
                    )),
                    JSONObject(mapOf(
                        ExerciseConfig.SetConfig.REPS to 5,
                        ExerciseConfig.SetConfig.DURATION to JSONObject.NULL,
                        ExerciseConfig.SetConfig.WEIGHT to 0
                    )),
                    JSONObject(mapOf(
                        ExerciseConfig.SetConfig.REPS to 5,
                        ExerciseConfig.SetConfig.DURATION to JSONObject.NULL,
                        ExerciseConfig.SetConfig.WEIGHT to 0
                    ))
                ))
            ))
        ))
    ))

    override fun getBgColor(context: Context): Int {
        return context.resources.getColor(R.color.colorWorkout, null)
    }

    override fun getDataLayout(entry: Entry, context: Context): EntryDataLayout {
        val startingData = JSONObject(mapOf(
            "$DURATION (min)" to defaultData[DURATION] as Int,
            "exercise count" to (defaultData[EXERCISES] as JSONArray).length()
        ))
        return EntryDataLayout(context, startingData)
    }

    override fun getDailyReminderTimes(): List<LocalTime>? = null
}

class EntryTypes {

    companion object {
        const val MEDITATION = "Meditation"
        const val MOOD = "Mood"
        const val DRUG_USE = "Drug use"
        const val WORKOUT = "Workout"

        private val ENTRY_TYPE_CONFIGS : HashMap<String, EntryTypeConfig> = hashMapOf(
            MEDITATION to MeditationConfig(),
            MOOD to MoodConfig(),
            DRUG_USE to DrugUseConfig(),
            WORKOUT to WorkoutConfig()
        )

        fun getTypes() : List<String> {
            return ENTRY_TYPE_CONFIGS.keys.toList().sorted()
        }

        fun getConfig(type : String) : EntryTypeConfig? {
            return ENTRY_TYPE_CONFIGS[type]
        }
    }
}

class EntryCardView(context: Context) : androidx.cardview.widget.CardView(context) {

    init {
        LayoutInflater.from(context).inflate(R.layout.view_entry_card, this, true)
    }

    fun insertEntryData(entry : Entry) {
        val timeStamp = entry.dateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
        val titleStr = "${entry.type} at $timeStamp"
        val bgColor = EntryTypes.getConfig(entry.type)!!.getBgColor(context)
        val dataView = EntryTypes.getConfig(entry.type)!!.getDataLayout(entry, context)

        findViewById<TextView>(R.id.entryTitle).text = titleStr
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
