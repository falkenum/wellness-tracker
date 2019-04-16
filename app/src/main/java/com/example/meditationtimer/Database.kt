package com.example.meditationtimer

import android.arch.persistence.room.*
import android.content.Context
import android.support.v7.widget.CardView
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

//    @TypeConverter
//    fun durationToMillis(duration : Duration) : Long {
//        return duration.toMillis()
//    }
//
//    @TypeConverter
//    fun millisToDuration(duration : Long) : Duration {
//        return Duration.ofMillis(duration)
//    }
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
            return Record(dateTime, MEDITATION).apply { data.put("duration", duration) }
        }
    }
}

//interface Record {
//    val dateTime: OffsetDateTime
//    fun getType() : String
//    fun getDataView(context: Context) : View
//    fun normalize() : Record
//
//    interface Creator<T : Record> {
//        fun createFrom(record: Record) : T
//    }
//}

//class MeditationRecord(
//    override val dateTime : OffsetDateTime,
//    val duration : Duration
//) : Record {
//    override fun getType(): String = "Meditation"
//
//    override fun getDataView(context: Context): View {
//        return TextView(context).apply {
//            text = "duration: ${duration.toMinutes()} minutes"
//            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
//        }
//    }
//
//    override fun normalize(): Record {
//        TODO()
//    }
//
//    companion object : Record.Creator<MeditationRecord> {
//        override fun createFrom(record: Record): MeditationRecord {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//        }
//    }
//}
//
//class JournalRecord (
//    override val dateTime : OffsetDateTime,
//    val body : String
//) : Record {
//    override fun getType(): String = "Journal entry"
//
//    override fun getDataView(context: Context): View {
//        return TextView(context).apply {
//            text = body
//        }
//    }
//
//    override fun normalize(): Record {
//        TODO()
//    }
//}

@Dao
interface RecordDao{
    @Query("SELECT * FROM Record")
    fun getAll() : List<Record>

    @Insert
    fun insert(record: Record)

    @Delete
    fun delete(record: Record)
}

//@Dao
//abstract class RecordDao{
//    @Query("SELECT * FROM JournalRecord")
//    protected abstract fun getAllJournal(): List<JournalRecord>
//
//    @Query("SELECT * FROM MeditationRecord")
//    protected abstract fun getAllMeditation(): List<MeditationRecord>
//
//    @Transaction
//    fun getAll(): List<Record> {
//        return ArrayList<Record>(0).apply {
//            val meditationRecords = getAllMeditation()
//            val journalRecords = getAllJournal()
//            addAll(meditationRecords)
//            addAll(journalRecords)
//        }.toList()
//    }
//
//    @Insert
//    protected abstract fun insertMeditation(record: MeditationRecord)
//
//    @Insert
//    protected abstract fun insertJournal(record: JournalRecord)
//
//    @Transaction
//    fun insert(record: Record) {
//        when (record) {
//            is MeditationRecord -> insertMeditation(record)
//            is JournalRecord -> insertJournal(record)
//            else -> throw Exception("type not implemented")
//        }
//    }
//
//    @Delete
//    protected abstract fun deleteMeditation(record: MeditationRecord)
//
//    @Delete
//    protected abstract fun deleteJournal(record: JournalRecord)
//
//    @Transaction
//    fun delete(record: Record) {
//        when (record) {
//            is MeditationRecord -> deleteMeditation(record)
//            is JournalRecord -> deleteJournal(record)
//            else -> throw Exception("type not implemented")
//        }
//    }
//}


@Database(entities = arrayOf(Record::class), version = 4, exportSchema = false)
abstract class RecordDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao

    companion object {
        lateinit var instance : RecordDatabase
            private set


        fun init(context: Context) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                // TODO figure out how to change database name and merge data
                RecordDatabase::class.java, "meditation-records-db")
                .build()
        }
    }
}
