package com.sjfalken.wellnesstracker

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.*
import androidx.room.migration.Migration
import android.content.Context
import org.json.JSONObject
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.room.RawQuery
import java.time.*


class TimeConverter {
    @TypeConverter
    fun dateTimeToSecond(dateTime : ZonedDateTime) : Long {
        return dateTime.toEpochSecond()
    }

    @TypeConverter
    fun secondToDateTime(epochSeconds : Long) : ZonedDateTime {
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault())
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
data class Entry(val dateTime : ZonedDateTime,
                 val type : String,
                 val data : JSONObject = JSONObject()) {



    companion object {
        fun newMeditationEntry(dateTime: ZonedDateTime, durationMinutes : Long) : Entry {
            return Entry(dateTime, EntryTypes.MEDITATION).apply {
                data.put(MeditationConfig.DURATION_MIN, durationMinutes)
            }
        }
        fun isValidEntry(entry : Entry) : Boolean {
            for (key in entry.data.keys()) {
                // if key is empty
                if (key == "")
                    return false

                // if key isn't valid for the given type
                if (EntryTypes.getConfig(entry.type).defaultData.isNull(key))
                    return false
            }


            return true
        }

    }

}

@Dao
interface EntryDao{
    @Query("SELECT * FROM Entry")
    fun getAll() : List<Entry>



    @Insert(onConflict = OnConflictStrategy.FAIL)
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

    @Query("SELECT * FROM Entry WHERE " +
            "dateTime >= :startEpochSecond AND " +
            "dateTime < :endEpochSecond")
    fun getAllWithinDuration(startEpochSecond: Long, endEpochSecond: Long) : List<Entry>

    @Transaction
    fun getAllForDate(date : LocalDate) : List<Entry> {
        val startDateTime = ZonedDateTime.of(date, LocalTime.MIN, ZoneId.systemDefault())
        val endDateTime = ZonedDateTime.of(date, LocalTime.MAX, ZoneId.systemDefault())

        return getAllWithinDuration(startDateTime.toEpochSecond(), endDateTime.toEpochSecond())
    }

    @Transaction
    fun getAllForDateAndType(date : LocalDate, type : String) : List<Entry> {
        val startDateTime = ZonedDateTime.of(date, LocalTime.MIN, ZoneId.systemDefault())
        val endDateTime = ZonedDateTime.of(date, LocalTime.MAX, ZoneId.systemDefault())

        return getAllWithinDurationAndType(startDateTime.toEpochSecond(), endDateTime.toEpochSecond(), type)
    }
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
        const val DB_NAME_PRIMARY = "log-entries.db"
        lateinit var instance : LogEntryDatabase
            private set

        fun insertEntriesFromDbFile(filePath : String) {

            val query = "attach \'$filePath\' as toMerge;" +
                    "BEGIN;insert into Entry select * from toMerge.Entry;COMMIT;detach toMerge;"
            instance.configDao().rawQuery(SimpleSQLiteQuery(query))
        }

        fun checkpoint() {
            instance.configDao().rawQuery(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
        }

        fun init(context: Context, dbName : String) : LogEntryDatabase {
            val newDb = Room.databaseBuilder(context.applicationContext,
                LogEntryDatabase::class.java, dbName)
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                .build()

            if (dbName == DB_NAME_PRIMARY) instance = newDb

            return newDb
        }
    }
}
