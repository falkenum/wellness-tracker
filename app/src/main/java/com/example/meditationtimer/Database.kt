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
data class Entry(val dateTime : OffsetDateTime,
                 val type : String,
                 val data : JSONObject = JSONObject()) {



    companion object {
        fun newMeditationEntry(dateTime: OffsetDateTime, duration: Duration) : Entry {
            return Entry(dateTime, EntryTypes.MEDITATION).apply {
                data.put(MeditationConfig.DURATION, duration)
            }
        }
    }
}

@Dao
interface EntryDao{
    @Query("SELECT * FROM Entry")
    fun getAll() : List<Entry>

    @Insert
    fun insert(entry: Entry)

    @Delete
    fun delete(entry: Entry)

    @Query("SELECT * FROM Entry WHERE " +
            "type = :type AND " +
            "dateTime >= :startEpochSecond AND " +
            "dateTime < :endEpochSecond")
    fun getAllWithinDurationAndType(startEpochSecond : Long,
                                    endEpochSecond : Long,
                                    type: String) : List<Entry>
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

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE Record RENAME TO Entry")
    }
}

@Database(entities = [Entry::class], version = 7, exportSchema = false)
abstract class LogEntryDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun configDao(): ConfigDao

    companion object {
        private const val DB_NAME = "log-entries.db"
        lateinit var instance : LogEntryDatabase
            private set

        fun checkpoint() {
            instance.configDao().rawQuery(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
        }

        fun init(context: Context) {
            instance = Room.databaseBuilder(context.applicationContext,
                LogEntryDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                .build()
        }
    }
}
