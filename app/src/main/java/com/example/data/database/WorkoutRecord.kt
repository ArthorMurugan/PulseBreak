package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_records")
data class WorkoutRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val presetName: String,
    val workDurationSec: Int,
    val restDurationSec: Int,
    val totalRounds: Int,
    val completedRounds: Int,
    val totalDurationSec: Int,
    val completedAtEpochMs: Long = System.currentTimeMillis()
)
