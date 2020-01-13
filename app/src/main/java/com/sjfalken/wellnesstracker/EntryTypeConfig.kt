package com.sjfalken.wellnesstracker

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.findFragment
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.NumberFormatException
import java.lang.String.format
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.HashMap

interface EntryDatumHolder{
    val datumValue : Any

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

class KeyValueLinearLayout(context: Context, key : String, value : Any, private val inputMode: Boolean)
    : LinearLayout(context), EntryDatumHolder {

    private fun getKeyView(key : String) : TextView {
        return TextView(context).apply {
            text = "$key: "
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
        }
    }

    private fun getValueView(value : Any) : View {
        return if (!inputMode) TextView(context).apply {
            text = when(value) {
                is Double -> format(Locale.getDefault(), "%.2f", value)
                else -> value.toString()
            }
        }

        else EditText(context).apply {
                width = Utility.dpToPx(context, 100)
                text.insert(0, value.toString())
                textAlignment = View.TEXT_ALIGNMENT_CENTER

                val valueNumeric = value.toString().toDoubleOrNull()
                //if it's a number, change keyboard
                if (valueNumeric != null) {
                    inputType = InputType.TYPE_CLASS_NUMBER
                }
                else {
                    inputType = InputType.TYPE_CLASS_TEXT
                }
            }
    }
    init {
        orientation = HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        val spaceView = Space(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT).apply {
                weight = 1f
            }
        }

        addView(getKeyView(key))
        addView(spaceView)
        getValueView(value).apply { gravity = Gravity.END; addView(this) }
    }

    override val datumValue: Any
        get() = EntryDatumHolder.reduceValueType((getChildAt(2) as TextView).text)
}

open class JSONObjectLayout(context: Context, private val startingData : JSONObject)
    : LinearLayout(context) {

    val data : JSONObject
        get() {
            return JSONObject().apply {
                val keys = startingData.keys()
                var key = keys.next()
                for (rowIndex in 0 until childCount) {
                    val row = getChildAt(rowIndex) as EntryDatumHolder
                    val value = row.datumValue

                    put(key, value)
                    if (keys.hasNext()) key = keys.next()
                }
            }
        }

    // to suppress a warning
    final override fun addView(child: View?) {
        super.addView(child)
    }

    protected open fun getRowLayout(key: String, value: Any) : View {
        return KeyValueLinearLayout(context, key, value, false)
    }

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        for (key in startingData.keys()) {
            addView(getRowLayout(key, startingData[key]))
        }
    }
}

class JSONObjectInputLayout(context: Context, data : JSONObject): JSONObjectLayout(context, data) {
    override fun getRowLayout(key: String, value: Any): View {
        return KeyValueLinearLayout(context, key, value, true)
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

    open fun getDataLayout(
        context: Context,
        entry: Entry
    ): JSONObjectLayout {
        return JSONObjectLayout(context, entry.data)
    }

    open fun getExpandedDataLayout(context: Context, entry: Entry) : View
            = getDataLayout(context, entry)

    open fun getDataInputLayout(context: Context): JSONObjectLayout {
        return JSONObjectInputLayout(context, presets[0].data)
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
        : JSONObjectLayout(context, startingData) {
        override fun getRowLayout(key: String, value: Any): View {
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

    override fun getDataInputLayout(context: Context): JSONObjectLayout {
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
        const val SETS = "sets"
    }

    class SetConfig : JSONConfig() {
        companion object {
            const val EXERCISE = "exercise"
            const val REPS = "reps"
            const val DURATION = "duration"
            const val DISTANCE_MILES = "distance"
            const val WEIGHT = "weight"
            const val RUNNING = "running"
            const val PULLUPS = "pullups"
            const val DIPS = "dips"
            const val LIFTING = "lifting"
        }
        override val presets: List<MapPreset> = listOf(
            MapPreset(RUNNING, JSONObject(mapOf(
                EXERCISE to RUNNING,
                DURATION to Duration.ofMinutes(10).toString(),
                DISTANCE_MILES to 1.0
            ))),
            MapPreset(LIFTING, JSONObject(mapOf(
                EXERCISE to JSONObject.NULL,
                REPS to 5,
                WEIGHT to -30
            )))
        )

    }

    override val presets: List<MapPreset> = listOf(
        MapPreset("Basic", JSONObject().apply {
            val sets = JSONArray()
            val runningSet = SetConfig().getPresetDataByName(SetConfig.RUNNING)
            val pullupsSet = (SetConfig().getPresetDataByName(SetConfig.LIFTING) as JSONObject).apply {
                put(SetConfig.EXERCISE, SetConfig.PULLUPS)
            }
            val dipsSet = (SetConfig().getPresetDataByName(SetConfig.LIFTING) as JSONObject).apply {
                put(SetConfig.EXERCISE, SetConfig.DIPS)
            }
            sets.put(runningSet)
            sets.put(pullupsSet)
            sets.put(dipsSet)
            sets.put(pullupsSet)
            sets.put(dipsSet)
            sets.put(pullupsSet)
            sets.put(dipsSet)

            put(DURATION_MIN, 60)
            put(SETS, sets)
        })
    )

    override fun getBgColor(context: Context): Int {
        return context.resources.getColor(R.color.colorWorkout, null)
    }

    override fun getDataLayout(
        context: Context,
        entry: Entry
    ): JSONObjectLayout {
        val startingData = JSONObject(mapOf(
            "$DURATION_MIN (min)" to presets[0].data[DURATION_MIN] as Int,
            "set count" to (presets[0].data[SETS] as JSONArray).length()
        ))
        return JSONObjectLayout(context, startingData)
    }

    class SetInputDialogFragment : DialogFragment() {
        var setToEdit : JSONObject? = null
        var onDelete : (() -> Unit)? = null
        var onConfirm : ((JSONObject) -> Unit)? = null

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(context)
            var defaultData = SetConfig().presets[0].data
            if (setToEdit != null) {
                builder.setNegativeButton("delete") {_, _ -> onDelete?.invoke()}
                defaultData = setToEdit!!
            }

            val inputView = JSONObjectInputLayout(context!!, defaultData)

            builder.setPositiveButton("confirm") {_, _ -> onConfirm?.invoke(inputView.data)}
                .setNeutralButton("cancel") {_, _ ->}

            builder.setView(inputView)

            return builder.create()
        }
    }

    class SetsInputLayout(context: Context, defaultSets: JSONArray)
        : LinearLayout(context), EntryDatumHolder {

        private fun getSetCard(set : JSONObject) : CardView {
            val setCardContent = JSONObjectLayout(context, set)

            return (LayoutInflater.from(context)
                .inflate(R.layout.view_exercise_card, this, false) as CardView).apply {

                val cardView = this
                id = View.generateViewId()
                addView(setCardContent)
                setOnClickListener {
                    val fm = findFragment<BaseFragment>().childFragmentManager
                    SetInputDialogFragment().apply {
                        setToEdit = set
                        onDelete = { this@SetsInputLayout.removeView(cardView) }
                        show(fm, "SetInput")
                    }
                }
            }
        }

        init {
            orientation = VERTICAL

            val setInputDialogFragment = SetInputDialogFragment()
            val setInputDialogFragmentTag = "SetInput"

            val titleRow = LinearLayout(context).apply {
                val titleLayoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER_VERTICAL
                }
                addView(TextView(context).apply {
                    text = "$SETS: ${defaultSets.length()}"
                    layoutParams = titleLayoutParams
                })

                val spaceLayoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT).apply {
                    weight = 1f
                }
                addView(Space(context).apply { layoutParams = spaceLayoutParams })

                addView(Button(context).apply {
                    text = "add"
                    setOnClickListener {
                        val fm = findFragment<BaseFragment>().childFragmentManager
                        setInputDialogFragment.apply {
                            onConfirm = { this@SetsInputLayout.addView(getSetCard(it)) }
                            show(fm, setInputDialogFragmentTag)
                        }
                    }
                })

            }


            addView(titleRow)
            for (i in 0 until defaultSets.length()) {
                val set = defaultSets[i] as JSONObject
                addView(getSetCard(set))
            }
        }

        override val datumValue: Any
            get() = JSONArray().apply {
                // 1 to skip title row
                for (i in 1 until childCount) {
                    val cardContent = (getChildAt(i) as CardView).getChildAt(0) as JSONObjectLayout
                    put(cardContent.data)
                }
            }
    }

    override fun getDataInputLayout(context: Context): JSONObjectLayout {
        return object : JSONObjectLayout(context, presets[0].data) {

            override fun getRowLayout(key: String, value: Any): View {
                return if (key != SETS) KeyValueLinearLayout(context, key, value, true)
                else SetsInputLayout(context, value as JSONArray)
            }
        }
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
        val dataView = EntryTypes.getConfig(entry.type)!!.getDataLayout(context, entry)

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
