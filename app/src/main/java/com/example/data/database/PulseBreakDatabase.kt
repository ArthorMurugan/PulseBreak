package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WorkoutRecord::class, DailyTrackerRecord::class],
    version = 1,
    exportSchema = false
)
abstract class PulseBreakDatabase : RoomDatabase() {
    abstract fun pulseBreakDao(): PulseBreakDao

    companion object {
        @Volatile
        private var INSTANCE: PulseBreakDatabase? = null

        fun getDatabase(context: Context): PulseBreakDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                                context.applicationContext,
                                PulseBreakDatabase::class.java,
                                "pulsebreak_database"
                            ).fallbackToDestructiveMigration(false).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
