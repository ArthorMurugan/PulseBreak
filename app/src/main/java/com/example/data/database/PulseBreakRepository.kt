package com.example.data.database

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PulseBreakRepository(private val dao: PulseBreakDao) {

    companion object {
        fun getTodayKey(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }

    val allWorkoutRecords: Flow<List<WorkoutRecord>> = dao.getAllWorkoutRecords()

    fun getTodayWorkouts(): Flow<List<WorkoutRecord>> {
        val midnightCalendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return dao.getWorkoutRecordsSince(midnightCalendar.timeInMillis)
    }

    suspend fun saveCompletedWorkout(
        presetName: String,
        workSec: Int,
        restSec: Int,
        totalRounds: Int,
        completedRounds: Int,
        totalDurationSec: Int
    ): Long {
        val record = WorkoutRecord(
            presetName = presetName,
            workDurationSec = workSec,
            restDurationSec = restSec,
            totalRounds = totalRounds,
            completedRounds = completedRounds,
            totalDurationSec = totalDurationSec,
            completedAtEpochMs = System.currentTimeMillis()
        )
        val id = dao.insertWorkoutRecord(record)

        // Increment today's workout minutes in daily tracker
        val todayKey = getTodayKey()
        val existing = dao.getDailyRecord(todayKey) ?: DailyTrackerRecord(dateKey = todayKey)
        val addedMinutes = (totalDurationSec + 59) / 60
        dao.insertOrUpdateDailyRecord(
            existing.copy(
                workoutMinutes = existing.workoutMinutes + addedMinutes
            )
        )

        return id
    }

    fun getDailyTracker(dateKey: String = getTodayKey()): Flow<DailyTrackerRecord?> {
        return dao.getDailyRecordFlow(dateKey)
    }

    suspend fun getOrCreateDailyTracker(dateKey: String = getTodayKey()): DailyTrackerRecord {
        val existing = dao.getDailyRecord(dateKey)
        if (existing != null) return existing
        val newRecord = DailyTrackerRecord(dateKey = dateKey)
        dao.insertOrUpdateDailyRecord(newRecord)
        return newRecord
    }

    suspend fun incrementWater(dateKey: String = getTodayKey(), target: Int = 8): Int {
        val current = getOrCreateDailyTracker(dateKey)
        val newCount = current.waterDrankCount + 1
        dao.insertOrUpdateDailyRecord(
            current.copy(
                waterDrankCount = newCount,
                waterTargetCount = target
            )
        )
        return newCount
    }

    suspend fun decrementWater(dateKey: String = getTodayKey()): Int {
        val current = getOrCreateDailyTracker(dateKey)
        val newCount = (current.waterDrankCount - 1).coerceAtLeast(0)
        dao.insertOrUpdateDailyRecord(current.copy(waterDrankCount = newCount))
        return newCount
    }

    suspend fun incrementMoveBreak(dateKey: String = getTodayKey(), target: Int = 8): Int {
        val current = getOrCreateDailyTracker(dateKey)
        val newCount = current.moveBreaksCount + 1
        dao.insertOrUpdateDailyRecord(
            current.copy(
                moveBreaksCount = newCount,
                moveTargetCount = target
            )
        )
        return newCount
    }

    suspend fun deleteWorkout(id: Long) {
        dao.deleteWorkoutRecord(id)
    }

    suspend fun clearAllHistory() {
        dao.clearAllWorkoutRecords()
        dao.clearAllDailyRecords()
    }
}
