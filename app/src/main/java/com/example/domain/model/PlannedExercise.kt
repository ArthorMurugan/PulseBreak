package com.example.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlannedExercise(
    val exerciseId: String,
    val name: String,
    val sets: Int = 3,
    val reps: Int = 12,
    val workDurationSec: Int = 30,
    val restDurationSec: Int = 15,
    val gifUrl: String = "",
    val bodyPart: String = ""
)
