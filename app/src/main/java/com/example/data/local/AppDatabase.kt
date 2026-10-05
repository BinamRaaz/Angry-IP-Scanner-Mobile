package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ScanHistoryDao
import com.example.data.local.dao.SpeedTestDao
import com.example.data.local.entities.ScanSessionEntity
import com.example.data.local.entities.ScannedHostEntity
import com.example.data.local.entities.SpeedTestEntity

@Database(
    entities = [
        ScanSessionEntity::class,
        ScannedHostEntity::class,
        SpeedTestEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun speedTestDao(): SpeedTestDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "angry_ip_scanner.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
