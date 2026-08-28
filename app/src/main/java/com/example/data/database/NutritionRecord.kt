package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nutrition_records")
data class NutritionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateKey: String, // format "YYYY-MM-DD"
    val mealName: String,
    val proteinGrams: Int = 0,
    val carbsGrams: Int = 0,
    val fatGrams: Int = 0,
    val timestampMs: Long = System.currentTimeMillis()
)
