package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.domain.model.WorkoutPlan
import com.example.domain.model.Exercise

@Database(
    entities = [
        WorkoutRecord::class, 
        DailyTrackerRecord::class, 
        NutritionRecord::class, 
        WeightRecord::class, 
        WorkoutPlan::class,
        Exercise::class
    ],
    version = 5,
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
                            )
                            .fallbackToDestructiveMigration(true)
                            .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
