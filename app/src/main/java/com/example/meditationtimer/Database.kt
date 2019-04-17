package com.example.meditationtimer

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.*
import android.arch.persistence.room.migration.Migration
import android.content.Context
import android.support.v7.widget.CardView
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import org.w3c.dom.Text
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass

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
        LayoutInflater.from(context).inflate(R.layout.view_session_record_card, this, true)
    }

    fun insertRecordData(record : Record) {
        val timeStamp = record.dateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
        val titleStr = "${record.type} at $timeStamp"

        // title
        findViewById<TextView>(R.id.recordTitle).text = titleStr

        // create a view containing the JSON data
        findViewById<TextView>(R.id.recordDataText).apply {
            var dataStr = ""
            for (key in record.data.keys())
                dataStr += "$key: ${record.data[key]}"
            text = dataStr
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        }
    }

    fun setOnDelete(onDelete: () -> Unit) {
        findViewById<Button>(R.id.deleteButton).setOnClickListener {
            onDelete()
        }
    }
}

@TypeConverters(JSONConverter::class, TimeConverter::class)
@Entity(primaryKeys = arrayOf("dateTime", "type"))
data class Record(val dateTime : OffsetDateTime, val type : String, val data : JSONObject = JSONObject()) {

    companion object {
        const val MEDITATION = "Meditation"

        fun newMeditation(dateTime: OffsetDateTime, duration: Duration) : Record {
            return Record(dateTime, MEDITATION).apply { data.put("duration", duration) }
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
