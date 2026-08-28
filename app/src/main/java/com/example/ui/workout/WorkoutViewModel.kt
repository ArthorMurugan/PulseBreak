package com.example.ui.workout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PulseBreakApp
import com.example.data.database.WorkoutRecord
import com.example.domain.model.WorkoutConfig
import com.example.domain.model.WorkoutState
import com.example.service.WorkoutForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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
