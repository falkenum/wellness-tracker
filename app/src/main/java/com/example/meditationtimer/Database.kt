package com.example.meditationtimer

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.*
import androidx.room.migration.Migration
import android.content.Context
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.room.RawQuery



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



@TypeConverters(JSONConverter::class, TimeConverter::class)
@Entity(primaryKeys = arrayOf("dateTime", "type"))
data class Record(val dateTime : OffsetDateTime, val type : String, val data : JSONObject = JSONObject()) {

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

@Dao
interface RecordDao{
    @Query("SELECT * FROM Record")
    fun getAll() : List<Record>

    @Insert
    fun insert(record: Record)

    @Delete
    fun delete(record: Record)
}

@Dao
interface ConfigDao{
    @RawQuery
    fun rawQuery(supportSQLiteQuery: SupportSQLiteQuery): Int
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE MeditationRecord")
    }
}

@Database(entities = [Record::class], version = 6, exportSchema = false)
abstract class RecordDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
    abstract fun configDao(): ConfigDao

    companion object {
        lateinit var instance : RecordDatabase
            private set

        fun checkpoint() {
            instance.configDao().rawQuery(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
        }

        fun init(context: Context) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                // TODO figure out how to change database name
                RecordDatabase::class.java, "meditation-records-db")
                .addMigrations(MIGRATION_5_6)
                .build()
        }
    }
}
