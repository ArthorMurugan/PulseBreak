package com.example.domain.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercises",
    indices = [Index(value = ["name"]), Index(value = ["bodyPart"]), Index(value = ["target"])]
)
data class Exercise(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val bodyPart: String = "",
    val equipment: String = "",
    val target: String = "",
    val gifUrl: String = ""
)
