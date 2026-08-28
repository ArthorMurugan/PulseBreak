package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.domain.model.WorkoutPlan
import com.example.domain.model.Exercise
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

    @Query("SELECT * FROM daily_tracker_records WHERE dateKey IN (:dateKeys)")
    fun getDailyRecords(dateKeys: List<String>): Flow<List<DailyTrackerRecord>>

    @Query("DELETE FROM daily_tracker_records")
    suspend fun clearAllDailyRecords()

    // Nutrition records
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNutritionRecord(record: NutritionRecord): Long

    @Query("SELECT * FROM nutrition_records WHERE dateKey = :dateKey ORDER BY timestampMs DESC")
    fun getNutritionRecords(dateKey: String): Flow<List<NutritionRecord>>

    @Query("DELETE FROM nutrition_records WHERE id = :id")
    suspend fun deleteNutritionRecord(id: Long)

    // Weight records
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightRecord(record: WeightRecord)

    @Query("SELECT * FROM weight_records ORDER BY dateKey ASC")
    fun getAllWeightRecords(): Flow<List<WeightRecord>>

    @Query("SELECT * FROM weight_records WHERE dateKey = :dateKey")
    suspend fun getWeightRecord(dateKey: String): WeightRecord?

    // Workout Plans
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutPlan(plan: WorkoutPlan)

    @Query("SELECT * FROM workout_plans")
    fun getAllWorkoutPlans(): Flow<List<WorkoutPlan>>

    @Query("SELECT * FROM workout_plans WHERE dayOfWeek = :dayOfWeek")
    suspend fun getWorkoutPlan(dayOfWeek: Int): WorkoutPlan?

    // Exercises
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<Exercise>)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getExerciseCount(): Int

    @Query("SELECT * FROM exercises")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE name LIKE :query OR bodyPart LIKE :query OR target LIKE :query OR equipment LIKE :query ORDER BY CASE WHEN name LIKE :query THEN 0 ELSE 1 END, name LIMIT :limit")
    fun searchExercises(query: String, limit: Int = 100): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id IN (:ids)")
    suspend fun getExercisesByIds(ids: List<String>): List<Exercise>
}
