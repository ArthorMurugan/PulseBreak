package com.example.domain.model

enum class ReminderType(val title: String, val defaultIntervalMin: Int) {
    WATER("Water", 45),
    STAND_MOVE("Stand & Move", 60),
    STRETCH("Stretch", 120)
}

data class ReminderConfig(
    val id: String,
    val type: ReminderType,
    val isEnabled: Boolean = true,
    val intervalMinutes: Int = 45,
    val startHour: Int = 8,
    val startMinute: Int = 0,
    val endHour: Int = 20,
    val endMinute: Int = 0,
    val activeDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7), // 1 = Monday .. 7 = Sunday
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val lastTriggeredEpochMs: Long = 0L,
    val nextScheduledEpochMs: Long = 0L
) {
    companion object {
        fun createDefaults(): List<ReminderConfig> = listOf(
            ReminderConfig(
                id = "rem_water",
                type = ReminderType.WATER,
                isEnabled = true,
                intervalMinutes = 45,
                startHour = 8,
                startMinute = 0,
                endHour = 20,
                endMinute = 0,
                soundEnabled = true,
                vibrationEnabled = true
            ),
            ReminderConfig(
                id = "rem_stand",
                type = ReminderType.STAND_MOVE,
                isEnabled = true,
                intervalMinutes = 60,
                startHour = 9,
                startMinute = 0,
                endHour = 18,
                endMinute = 0,
                soundEnabled = true,
                vibrationEnabled = true
            ),
            ReminderConfig(
                id = "rem_stretch",
                type = ReminderType.STRETCH,
                isEnabled = false,
                intervalMinutes = 120,
                startHour = 10,
                startMinute = 0,
                endHour = 19,
                endMinute = 0,
                soundEnabled = true,
                vibrationEnabled = true
            )
        )
    }
}
