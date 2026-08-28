package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_plans")
data class WorkoutPlan(
    @PrimaryKey
    val dayOfWeek: Int, // 1 (Sunday) to 7 (Saturday) from Calendar.DAY_OF_WEEK
    val planName: String, // e.g., "HIIT Cardio", "Rest Day", "Leg Day"
    val isRestDay: Boolean = false,
    val workoutConfigId: String? = null, // Link to a specific preset if applicable
    val exerciseIds: String = "", // Comma-separated list of Exercise IDs (Legacy)
    val plannedExercisesJson: String = "", // JSON list of PlannedExercise
    val defaultWorkSec: Int = 30,
    val defaultRestSec: Int = 15,
    val defaultRounds: Int = 8
)
