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

    @TypeConverter
    fun durationToMillis(duration : Duration) : Long {
        return duration.toMillis()
    }

    @TypeConverter
    fun millisToDuration(duration : Long) : Duration {
        return Duration.ofMillis(duration)
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
        val dataView = TextView(context).apply {
            var dataStr = ""
            for (key in record.data.keys())
                dataStr += "$key: ${record.data[key]}\n"
            text = dataStr
        }

        // main data
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
data class Record(val dateTime : OffsetDateTime, val type : String, val data : JSONObject = JSONObject()) {

    companion object {
        const val MEDITATION = "Meditation"
        fun newMeditation(dateTime: OffsetDateTime, duration: Duration) : Record {
            return Record(dateTime, MEDITATION).apply { data.put("durationMillis", duration.toMillis()) }
        }
    }
}

@TypeConverters(TimeConverter::class)
@Entity
class MeditationRecord(
    @PrimaryKey val dateTime : OffsetDateTime,
    val duration : Duration
)

@Dao
interface RecordDao{
    @Query("SELECT * FROM Record")
    fun getAll() : List<Record>

    @Query("SELECT * FROM MeditationRecord")
    fun getAllOld() : List<MeditationRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(record: Record)

    @Delete
    fun delete(record: Record)
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {

        // getting rid of old table
        database.execSQL("DROP TABLE JournalRecord")

        // creating new record table for all types
        database.execSQL("CREATE TABLE Record (dateTime INTEGER NOT NULL, type TEXT NOT NULL, data TEXT NOT NULL," +
                "PRIMARY KEY (dateTime, type))")
    }
}

val MIGRATION_3_5 = object : Migration(3, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // creating new record table for all types
        database.execSQL("CREATE TABLE Record (dateTime INTEGER NOT NULL, type TEXT NOT NULL, data TEXT NOT NULL," +
                "PRIMARY KEY (dateTime, type))")
    }
}

@Database(entities = arrayOf(Record::class, MeditationRecord::class), version = 5, exportSchema = false)
abstract class RecordDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao

    companion object {
        lateinit var instance : RecordDatabase
            private set


        fun init(context: Context) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                // TODO figure out how to change database name and merge data
                RecordDatabase::class.java, "meditation-records-db")
                .addMigrations(MIGRATION_4_5)
                .addMigrations(MIGRATION_3_5)
                .build()

            // copy data from old table to new one
            for (mr in instance.recordDao().getAllOld()) {
                val dateTime = mr.dateTime
                val type = Record.MEDITATION
                val duration = mr.duration
                val data = JSONObject().apply { put("duration", duration) }
                println(data.toString())

                val newRecord = Record(dateTime, type, data)

                instance.recordDao().insert(newRecord)
            }
        }
    }
}
