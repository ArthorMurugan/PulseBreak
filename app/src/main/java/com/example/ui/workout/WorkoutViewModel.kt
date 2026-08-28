package com.example.ui.workout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PulseBreakApp
import com.example.data.database.WorkoutRecord
import com.example.domain.model.WorkoutConfig
import com.example.domain.model.WorkoutPlan
import com.example.domain.model.WorkoutState
import com.example.service.WorkoutForegroundService
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WorkoutSetupState(
    val selectedPresetId: String = WorkoutConfig.PRESET_30_15.id,
    val workDurationSec: Int = 30,
    val restDurationSec: Int = 15,
    val totalRounds: Int = 8,
    val countdownSound: Boolean = true,
    val beepSound: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val countdownWarningSec: Int = 3
)

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PulseBreakApp
    private val repository = app.repository
    private val preferencesRepository = app.preferencesRepository

    private val _setupState = MutableStateFlow(WorkoutSetupState())
    val setupState: StateFlow<WorkoutSetupState> = _setupState.asStateFlow()

    val activeWorkoutState: StateFlow<WorkoutState> = WorkoutForegroundService.activeWorkoutState

    val workoutHistory: StateFlow<List<WorkoutRecord>> = repository.allWorkoutRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val todayWorkoutPlan: StateFlow<WorkoutPlan?> = repository.getAllWorkoutPlans()
        .map { plans ->
            val dayOfWeek = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
            plans.find { it.dayOfWeek == dayOfWeek }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Load default settings if available
        viewModelScope.launch {
            preferencesRepository.userSettingsFlow.collect { settings ->
                _setupState.value = _setupState.value.copy(
                    workDurationSec = settings.defaultWorkSec,
                    restDurationSec = settings.defaultRestSec,
                    totalRounds = settings.defaultRounds,
                    countdownSound = settings.workoutSoundEnabled,
                    beepSound = settings.workoutSoundEnabled,
                    vibrationEnabled = settings.workoutVibrationEnabled,
                    countdownWarningSec = settings.workoutCountdownWarningSec
                )
            }
        }
    }

    fun selectPreset(config: WorkoutConfig) {
        _setupState.value = _setupState.value.copy(
            selectedPresetId = config.id,
            workDurationSec = config.workDurationSec,
            restDurationSec = config.restDurationSec,
            totalRounds = config.totalRounds
        )
    }

    fun updateWorkDuration(sec: Int) {
        _setupState.value = _setupState.value.copy(
            selectedPresetId = "custom",
            workDurationSec = sec
        )
    }

    fun updateRestDuration(sec: Int) {
        _setupState.value = _setupState.value.copy(
            selectedPresetId = "custom",
            restDurationSec = sec
        )
    }

    fun updateTotalRounds(rounds: Int) {
        _setupState.value = _setupState.value.copy(
            selectedPresetId = "custom",
            totalRounds = rounds
        )
    }

    fun toggleCountdownSound(enabled: Boolean) {
        _setupState.value = _setupState.value.copy(countdownSound = enabled)
    }

    fun toggleBeepSound(enabled: Boolean) {
        _setupState.value = _setupState.value.copy(beepSound = enabled)
    }

    fun toggleVibration(enabled: Boolean) {
        _setupState.value = _setupState.value.copy(vibrationEnabled = enabled)
    }

    fun updateCountdownWarning(sec: Int) {
        _setupState.value = _setupState.value.copy(countdownWarningSec = sec)
    }

    fun startTodayPlan() {
        val plan = todayWorkoutPlan.value
        if (plan == null || plan.isRestDay) {
            startWorkout()
            return
        }

        val planned = try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(List::class.java, com.example.domain.model.PlannedExercise::class.java)
            moshi.adapter<List<com.example.domain.model.PlannedExercise>>(type).fromJson(plan.plannedExercisesJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        viewModelScope.launch {
            val enrichedPlanned = repository.enrichPlannedExercises(planned)
            val config = WorkoutConfig(
                id = "planned_today",
                name = plan.planName,
                workDurationSec = plan.defaultWorkSec,
                restDurationSec = plan.defaultRestSec,
                totalRounds = plan.defaultRounds,
                plannedExercises = enrichedPlanned,
                countdownSound = _setupState.value.countdownSound,
                beepSound = _setupState.value.beepSound,
                vibrationEnabled = _setupState.value.vibrationEnabled,
                countdownWarningSec = _setupState.value.countdownWarningSec
            )
            WorkoutForegroundService.start(getApplication(), config)
        }
    }

    fun startWorkout() {
        val s = _setupState.value
        val name = when (s.selectedPresetId) {
            WorkoutConfig.PRESET_30_15.id -> "30 / 15"
            WorkoutConfig.PRESET_45_15.id -> "45 / 15"
            WorkoutConfig.PRESET_60_30.id -> "60 / 30"
            else -> "Custom"
        }

        val config = WorkoutConfig(
            id = s.selectedPresetId,
            name = name,
            workDurationSec = s.workDurationSec,
            restDurationSec = s.restDurationSec,
            totalRounds = s.totalRounds,
            countdownSound = s.countdownSound,
            beepSound = s.beepSound,
            vibrationEnabled = s.vibrationEnabled,
            countdownWarningSec = s.countdownWarningSec
        )

        WorkoutForegroundService.start(getApplication(), config)
    }

    fun pauseWorkout() {
        WorkoutForegroundService.pause(getApplication())
    }

    fun resumeWorkout() {
        WorkoutForegroundService.resume(getApplication())
    }

    fun resetWorkout() {
        WorkoutForegroundService.reset(getApplication())
    }

    fun endWorkout() {
        WorkoutForegroundService.end(getApplication())
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteWorkout(id)
        }
    }
}
