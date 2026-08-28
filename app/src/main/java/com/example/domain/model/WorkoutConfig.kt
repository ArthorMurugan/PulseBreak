package com.example.domain.model

data class WorkoutConfig(
    val id: String = "preset_custom",
    val name: String = "Custom",
    val workDurationSec: Int = 30,
    val restDurationSec: Int = 15,
    val totalRounds: Int = 8,
    val countdownSound: Boolean = true,
    val beepSound: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val countdownWarningSec: Int = 3 // 3-second countdown before phase transition
) {
    val totalDurationSec: Int
        get() = (workDurationSec + restDurationSec) * totalRounds - restDurationSec // Final rest is skipped on completion

    companion object {
        val PRESET_30_15 = WorkoutConfig(
            id = "preset_30_15",
            name = "30 / 15",
            workDurationSec = 30,
            restDurationSec = 15,
            totalRounds = 8,
            countdownSound = true,
            beepSound = true,
            vibrationEnabled = true,
            countdownWarningSec = 3
        )

        val PRESET_45_15 = WorkoutConfig(
            id = "preset_45_15",
            name = "45 / 15",
            workDurationSec = 45,
            restDurationSec = 15,
            totalRounds = 8,
            countdownSound = true,
            beepSound = true,
            vibrationEnabled = true,
            countdownWarningSec = 3
        )

        val PRESET_60_30 = WorkoutConfig(
            id = "preset_60_30",
            name = "60 / 30",
            workDurationSec = 60,
            restDurationSec = 30,
            totalRounds = 6,
            countdownSound = true,
            beepSound = true,
            vibrationEnabled = true,
            countdownWarningSec = 3
        )

        val ALL_PRESETS = listOf(PRESET_30_15, PRESET_45_15, PRESET_60_30)
    }
}
