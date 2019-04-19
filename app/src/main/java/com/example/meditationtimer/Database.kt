package com.example.meditationtimer

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.*
import android.arch.persistence.room.migration.Migration
import android.content.Context
import android.support.v7.widget.CardView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
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

class TimeConverter {
    @TypeConverter
    fun dateTimeToSecond(dateTime : OffsetDateTime) : Long {
        return dateTime.toEpochSecond()
    }

    @TypeConverter
    fun secondToDateTime(epochSeconds : Long) : OffsetDateTime {
        return OffsetDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault())
    }
}

class JSONConverter {
    @TypeConverter
    fun JSONtoString(data : JSONObject) : String = data.toString()
    @TypeConverter
    fun stringToJSON(data : String) : JSONObject = JSONObject(data)
}


class RecordCardView(context: Context) : CardView(context) {

    init {
        // TODO remove redundant CardView in the hierarchy
        LayoutInflater.from(context).inflate(R.layout.view_session_record_card, this, true)
    }

    fun insertRecordData(record : Record) {

        val timeStamp = record.dateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
        val titleStr = "${record.type} at $timeStamp"

        // title
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

@TypeConverters(JSONConverter::class, TimeConverter::class)
@Entity(primaryKeys = arrayOf("dateTime", "type"))
open class Record(val dateTime : OffsetDateTime, val type : String, val data : JSONObject = JSONObject()) {

    fun getDataView(context: Context) : View {
        return RecordTypes.getDataView(this, context)
    }

    companion object {
        fun newMeditationRecord(dateTime: OffsetDateTime, duration: Duration) : Record {
            return Record(dateTime, RecordTypes.MEDITATION).apply {
                data.put("duration", duration)
            }
        }

        fun newMoodRecord(dateTime: OffsetDateTime, rating : Int) : Record {
            return Record(dateTime, RecordTypes.MOOD).apply {
                data.put("rating", rating)
            }
        }
    }
}

abstract class RecordDataInputView(context: Context) : FrameLayout(context) {
    abstract fun getData() : JSONObject
}

abstract class RecordTypeConfig {
    abstract fun getDataView(record : Record, context : Context) : View
    abstract fun getDataInputView(context: Context) : View
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
            override fun getData(): JSONObject {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
    }
}

@Dao
interface RecordDao{
    @Query("SELECT * FROM Record")
    fun getAll() : List<Record>

    @Insert
    fun insert(record: Record)

    @Delete
    fun delete(record: Record)
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE MeditationRecord")
    }
}

@Database(entities = arrayOf(Record::class), version = 6, exportSchema = false)
abstract class RecordDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao

    companion object {
        lateinit var instance : RecordDatabase
            private set


        fun init(context: Context) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                // TODO figure out how to change database name and merge data
                RecordDatabase::class.java, "meditation-records-db")
                .addMigrations(MIGRATION_5_6)
                .build()
        }
    }
}
