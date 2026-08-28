package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PulseBreakDao {
    // Workout records
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutRecord(record: WorkoutRecord): Long

    @Query("SELECT * FROM workout_records ORDER BY completedAtEpochMs DESC")
    fun getAllWorkoutRecords(): Flow<List<WorkoutRecord>>

    @Query("SELECT * FROM workout_records WHERE completedAtEpochMs >= :startEpochMs ORDER BY completedAtEpochMs DESC")
    fun getWorkoutRecordsSince(startEpochMs: Long): Flow<List<WorkoutRecord>>

    @Query("DELETE FROM workout_records WHERE id = :id")
    suspend fun deleteWorkoutRecord(id: Long)

    @Query("DELETE FROM workout_records")
    suspend fun clearAllWorkoutRecords()

    // Daily tracker records
    @Query("SELECT * FROM daily_tracker_records WHERE dateKey = :dateKey")
    fun getDailyRecordFlow(dateKey: String): Flow<DailyTrackerRecord?>

    @Query("SELECT * FROM daily_tracker_records WHERE dateKey = :dateKey")
    suspend fun getDailyRecord(dateKey: String): DailyTrackerRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyRecord(record: DailyTrackerRecord)

    @Query("DELETE FROM daily_tracker_records")
    suspend fun clearAllDailyRecords()
}
