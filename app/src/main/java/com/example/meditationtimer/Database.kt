package com.example.meditationtimer

import android.arch.persistence.room.*
import android.content.Context
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

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

@TypeConverters(TimeConverter::class)
@Entity
data class MeditationRecord(
    @PrimaryKey val dateTime : OffsetDateTime,
    @ColumnInfo val duration : Duration
)

@Dao
interface RecordDao {
    @Query("SELECT * FROM meditationRecord")
    fun getAll(): List<MeditationRecord>

    @Insert
    fun insert(record: MeditationRecord)

    @Delete
    fun delete(record: MeditationRecord)
}

@Database(entities = arrayOf(MeditationRecord::class), version = 3, exportSchema = false)
abstract class RecordDatabase : RoomDatabase() {
    abstract fun drugRecordDao(): RecordDao

    companion object {
        private lateinit var instance : RecordDatabase
        private lateinit var dao : RecordDao
        private val lock = RecordDatabase::class

        private lateinit var _records : ArrayList<MeditationRecord>
        val records : List<MeditationRecord> get() {
            synchronized(lock) {
                return _records.toList()
            }
        }


        fun init(context: Context) {
            synchronized(lock) {
                instance = Room.databaseBuilder(context.getApplicationContext(),
                    RecordDatabase::class.java, "meditation-records-db")
                    .fallbackToDestructiveMigration()
                    .build()
                dao = instance.drugRecordDao()
                _records = ArrayList(dao.getAll())
            }
        }

        fun add(record : MeditationRecord) {
            synchronized(lock) {
                _records.add(record)
                dao.insert(record)
            }
        }

        fun remove(record : MeditationRecord) {
            synchronized(lock) {
                _records.retainAll { it != record }
                dao.delete(record)
            }
        }

        fun size() : Int {
            synchronized(lock) {
                return _records.size
            }
        }
    }
}
