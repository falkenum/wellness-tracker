package com.example.meditationtimer

import android.arch.persistence.room.*
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import java.time.Duration
import java.time.Instant

class TimeConverter {
    @TypeConverter
    fun instantToMillis(time : Instant) : Long {
        return time.toEpochMilli()
    }

    @TypeConverter
    fun millisToInstant(time : Long) : Instant {
        return Instant.ofEpochMilli(time)
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
data class Record(@PrimaryKey val time : Instant, @ColumnInfo val duration : Duration) : Parcelable {

    constructor(parcel: Parcel) :
            this(parcel.readSerializable() as Instant, parcel.readSerializable() as Duration) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(time)
        parcel.writeSerializable(duration)
    }

    override fun describeContents(): Int {
        return hashCode()
    }

    companion object CREATOR : Parcelable.Creator<Record> {
        override fun createFromParcel(parcel: Parcel): Record {
            return Record(parcel)
        }

        override fun newArray(size: Int): Array<Record?> {
            return arrayOfNulls(size)
        }
    }
}
    @TypeConverter
    fun durationToMillis(duration : Duration) : Long {
        return duration.toMillis()
    }
@Dao
interface RecordDao {
    @Query("SELECT * FROM record")
    fun getAll(): List<Record>

    @Insert
    fun insert(drugRecord: Record)

    @Delete
    fun delete(record: Record)
}

@Database(entities = arrayOf(Record::class), version = 1)
abstract class RecordDatabase : RoomDatabase() {
    abstract fun drugRecordDao(): RecordDao

    companion object {
        private lateinit var instance : RecordDatabase
        private lateinit var dao : RecordDao
        private lateinit var records : ArrayList<Record>
        private val lock = RecordDatabase::class

        fun init(context: Context) {
            synchronized(lock) {
                instance = Room.databaseBuilder(context.getApplicationContext(),
                    RecordDatabase::class.java, "meditation-records-db")
                    .build()
                dao = instance.drugRecordDao()
                records = ArrayList(dao.getAll())
            }
        }

        fun add(record : Record) {
            synchronized(lock) {
                records.add(record)
                dao.insert(record)
            }
        }

        fun remove(record : Record) {
            synchronized(lock) {
                records.retainAll { it != record }
                dao.delete(record)
            }
        }
        fun getList() : List<Record> {
            synchronized(lock) {
                return records.toList()
            }
        }
        fun size() : Int {
            synchronized(lock) {
                return records.size
            }
        }
    }
}
