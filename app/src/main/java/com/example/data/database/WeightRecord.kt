package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_records")
data class WeightRecord(
    @PrimaryKey
    val dateKey: String, // format "YYYY-MM-DD"
    val weightKg: Float,
    val timestampMs: Long = System.currentTimeMillis()
)
