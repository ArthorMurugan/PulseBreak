package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_tracker_records")
data class DailyTrackerRecord(
    @PrimaryKey
    val dateKey: String, // format "YYYY-MM-DD"
    val waterDrankCount: Int = 0,
    val waterTargetCount: Int = 8,
    val moveBreaksCount: Int = 0,
    val moveTargetCount: Int = 8,
    val workoutMinutes: Int = 0,
    val stepCount: Int = 0,
    val stepTargetCount: Int = 6000
)
