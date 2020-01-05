package com.sjfalken.wellnesstracker

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.lang.NumberFormatException
import java.lang.String.format
import java.time.*
import java.time.format.DateTimeFormatter

interface EntryDatumHolder{
    val value : Any

    // i'm not proud of this one
    companion object {
        fun reduceValueType(value : Any) : Any {
            return try {
                value.toString().toInt()
            } catch (e : NumberFormatException) {
                try {
                    value.toString().toDouble()
                } catch (e : NumberFormatException) {
                    try {
                        JSONArray(value.toString())
                    } catch (e : JSONException) {
                        try {
                            JSONObject(value.toString())
                        } catch (e : JSONException) {
                            value.toString()
                        }
                    }
                }
            }
        }
    }
}

class EntryDatumTextView(context: Context) : TextView(context), EntryDatumHolder {
    override val value: Any
        get() = EntryDatumHolder.reduceValueType(text)
}

class EntryDatumEditText(context: Context) : EditText(context), EntryDatumHolder {
    override val value: Any
        get() = EntryDatumHolder.reduceValueType(text)
}

open class EntryDataLayout(context: Context, private val startingData : JSONObject)
    : LinearLayout(context) {

    private val labelSuffix = ": "
    private val valueIndex = 2

    val data : JSONObject
        get() {
            return JSONObject().apply {
                val keys = startingData.keys()
                var key = keys.next()
                for (rowIndex in 0 until childCount) {
                    val row = getChildAt(rowIndex) as LinearLayout
                    val valueView = row.getChildAt(valueIndex) as EntryDatumHolder
                    val value = valueView.value

                    put(key, value)
                    if (keys.hasNext()) key = keys.next()
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

abstract class JSONConfig {
    abstract class Preset(
        open val name : String?,
        open val data : Any
    )
    class MapPreset(
        override val name : String?,
        override val data : JSONObject
    ) : Preset(name, data)
    class ArrayPreset(
        override val name : String?,
        override val data : JSONArray
    ) : Preset(name, data)

    abstract val presets: List<Preset>


    // TODO recursively check JSONArray and JSONObject data
    fun isValidData(data : Any) : Boolean {
        val preset = presets[0]
        if (preset is MapPreset) {
            if (data !is JSONObject) return false

            val correctKeys = preset.data.keys().asSequence().sorted().toList()
            val givenKeys = data.keys().asSequence().sorted().toList()

            // if the given data doesn't have the same keys as the preset, then return false
            if (correctKeys.withIndex().any{ (i, _) -> givenKeys[i] != correctKeys[i] }) return false

            for (key in correctKeys) {
                if (key == "") return false
                if (preset.data[key].javaClass != data[key].javaClass) return false
            }
        }
        if (preset is ArrayPreset) {
            if (data !is JSONArray) return false

            // for now only JSONObjects are in the JSONArray for exercises
            for (index in 0 until data.length()) {
                if (data[index] !is JSONObject) return false
            }
        }

        return true
    }

    fun getPresetDataByName(name : String) : Any {
        return presets.first { it.name == name }.data
    }
}

abstract class EntryTypeConfig : JSONConfig() {


    abstract fun getBgColor(context: Context) : Int
    abstract fun getDailyReminderTimes(): List<LocalTime>?
    override val presets: List<MapPreset> = listOf()

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
        return EntryDataTextInputLayout(context, presets[0].data)
    }
}

class MeditationConfig: EntryTypeConfig() {
    companion object {
        const val DURATION_MIN = "duration"
    }

    override val presets: List<MapPreset> = listOf(
        MapPreset(null, JSONObject(mapOf(DURATION_MIN to 10)))
    )

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

    override val presets: List<MapPreset> = listOf(
        MapPreset(null, JSONObject(mapOf(RATING to 3)))
    )

    override fun getBgColor(context: Context): Int {
        return context.resources.getColor(R.color.colorMood, null)
    }

    override fun getDailyReminderTimes(): List<LocalTime>? {
        return listOf(
            LocalTime.of(18, 0)
        )
    }

    override fun getDataInputLayout(context: Context): EntryDataLayout {
        return EntryDataMoodInputLayout(context, presets[0].data)
    }
}

class DrugUseConfig : EntryTypeConfig() {
    companion object {
        const val SUBSTANCE = "substance"
        const val FORM = "form"
        const val QUANTITY_GRAMS = "quantity"
    }

    override val presets: List<MapPreset> = listOf(
        MapPreset(null, JSONObject(mapOf(
            SUBSTANCE to "Cannabis",
            FORM to "bowl",
            QUANTITY_GRAMS to 0.3
        )))
    )

    override fun getBgColor(context: Context): Int {
        return context.resources.getColor(R.color.colorDrugUse, null)
    }

    override fun getDailyReminderTimes(): List<LocalTime>? = listOf(
            LocalTime.of(20, 0)
        )
}

class WorkoutConfig : EntryTypeConfig() {
    companion object {
        const val DURATION_MIN = "duration"
        const val EXERCISES = "exercises"
    }

    class ExerciseConfig : JSONConfig() {
        companion object {
            const val PRESET_RUNNING = "running"
            const val PRESET_PULLUPS = "pullups"
        }


        class SetConfig : JSONConfig() {
            companion object {
                const val REPS = "reps"
                const val DURATION = "duration"
                const val WEIGHT = "weight"
                const val PRESET_RUNNING = "running"
                const val PRESET_LIFTING = "lifting"
            }
            override val presets: List<MapPreset> = listOf(
                MapPreset(PRESET_RUNNING, JSONObject(mapOf(
                    REPS to 1,
                    DURATION to Duration.ofMinutes(10).toString(),
                    WEIGHT to JSONObject.NULL
                ))),
                MapPreset(PRESET_LIFTING, JSONObject(mapOf(
                    REPS to 5,
                    DURATION to JSONObject.NULL,
                    WEIGHT to 0
                )))
            )
        }

        override val presets: List<ArrayPreset> = listOf(
            ArrayPreset(PRESET_RUNNING, JSONArray().apply{
                val runningSet =
                    SetConfig().getPresetDataByName(SetConfig.PRESET_RUNNING)
                put(runningSet)
            }),
            ArrayPreset(PRESET_PULLUPS, JSONArray().apply{
                val liftingSet =
                    SetConfig().getPresetDataByName(SetConfig.PRESET_LIFTING)
                put(liftingSet)
                put(liftingSet)
                put(liftingSet)
            })
        )
    }

    override val presets: List<MapPreset> = listOf(
        MapPreset("Basic", JSONObject().apply {
            put(DURATION_MIN, 60)

            val exercises = JSONObject()
            val runningExercise =
                ExerciseConfig().getPresetDataByName(ExerciseConfig.PRESET_RUNNING)
            exercises.put(ExerciseConfig.PRESET_RUNNING, runningExercise)
            val pullupsExercise =
                ExerciseConfig().getPresetDataByName(ExerciseConfig.PRESET_PULLUPS)
            exercises.put(ExerciseConfig.PRESET_PULLUPS, pullupsExercise)

            put(EXERCISES, exercises)
        })
    )

    override fun getBgColor(context: Context): Int {
        return context.resources.getColor(R.color.colorWorkout, null)
    }

//    override fun getDataLayout(entry: Entry, context: Context): EntryDataLayout {
//        val startingData = JSONObject(mapOf(
//            "$DUvRATION (min)" to presets[DURATION] as Int,
//            "exercise count" to (presets[EXERCISES] as JSONArray).length()
//        ))
//        return EntryDataLayout(context, startingData)
//    }

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
