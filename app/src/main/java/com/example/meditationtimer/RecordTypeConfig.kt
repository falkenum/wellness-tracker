package com.example.meditationtimer

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.*
import android.arch.persistence.room.migration.Migration
import android.content.Context
import android.support.v7.widget.CardView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

abstract class RecordTypeConfig {
    abstract fun getDataView(record : Record, context : Context) : View
    abstract fun getDataInputView(context: Context) : View

    abstract class RecordDataInputView(context: Context) : FrameLayout(context) {
        abstract fun getData() : JSONObject
    }
}

class MeditationConfig: RecordTypeConfig() {
    override fun getDataView(record: Record, context: Context): View {
        return TextView(context).apply {
            val duration = Duration.parse(record.data.getString("duration"))
            text = "duration: ${duration.toMinutes()} min"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        }
    }

    override fun getDataInputView(context: Context): View {
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
}

class MoodConfig : RecordTypeConfig() {
    override fun getDataView(record: Record, context: Context): View {
        return TextView(context).apply {
            text = "rating: ${record.data.getInt("rating")}"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        }
    }

    override fun getDataInputView(context: Context): View {
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
}

class RecordTypes {

    companion object {
        const val MEDITATION = "Meditation"
        const val MOOD = "Mood"

        private val recordTypeConfigs : HashMap<String, RecordTypeConfig> = hashMapOf(
            MEDITATION to MeditationConfig(),
            MOOD to MoodConfig()
        )

        fun getTypes() : List<String> {
            return recordTypeConfigs.keys.toList()
        }

        fun getDataView(record: Record, context: Context) : View {
            return recordTypeConfigs[record.type]!!.getDataView(record, context)
        }

        fun getDataInputView(type : String, context: Context) : View {
            return recordTypeConfigs[type]!!.getDataInputView(context)
        }
    }
}

class RecordCardView(context: Context) : CardView(context) {

    init {
        // TODO remove redundant CardView in the hierarchy
        LayoutInflater.from(context).inflate(R.layout.view_session_record_card, this, true)
    }

    fun insertRecordData(record : Record) {
        val timeStamp = record.dateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
        val titleStr = "${record.type} at $timeStamp"

        findViewById<TextView>(R.id.recordTitle).text = titleStr

        val dataView = record.getDataView(context)
        findViewById<LinearLayout>(R.id.recordDataLayout).addView(dataView)
    }

    fun setOnDelete(onDelete: () -> Unit) {
        findViewById<Button>(R.id.deleteButton).setOnClickListener {
            onDelete()
        }
    }
}
