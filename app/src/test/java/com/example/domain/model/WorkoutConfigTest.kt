package com.example.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutConfigTest {

    @Test
    fun `legacy duration excludes final rest period`() {
        val config = WorkoutConfig(
            workDurationSec = 30,
            restDurationSec = 15,
            totalRounds = 3
        )

        assertEquals(120, config.totalDurationSec)
    }

    @Test
    fun `planned exercise duration accounts for sets and final rest`() {
        val config = WorkoutConfig(
            plannedExercises = listOf(
                PlannedExercise(
                    exerciseId = "push_up",
                    name = "Push-up",
                    sets = 2,
                    workDurationSec = 20,
                    restDurationSec = 10
                )
            )
        )

        assertEquals(50, config.totalDurationSec)
    }

    @Test
    fun `built in presets have positive durations`() {
        WorkoutConfig.ALL_PRESETS.forEach { preset ->
            assertTrue(preset.totalDurationSec > 0)
        }
    }
}
