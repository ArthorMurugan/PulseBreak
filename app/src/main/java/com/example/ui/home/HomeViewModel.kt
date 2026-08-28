package com.example.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PulseBreakApp
import com.example.data.database.DailyTrackerRecord
import com.example.data.preferences.UserSettings
import com.example.domain.model.ReminderConfig
import com.example.domain.model.ReminderType
import com.example.domain.model.WorkoutConfig
import com.example.domain.model.WorkoutPlan
import com.example.domain.model.WorkoutState
import com.example.service.ReminderScheduler
import com.example.service.WorkoutForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class NextReminderInfo(
    val title: String,
    val minutesAway: Int,
    val type: ReminderType
)

data class HomeUiState(
    val greeting: String = "Good morning",
    val subtitle: String = "Ready to move?",
    val dailyTracker: DailyTrackerRecord = DailyTrackerRecord(dateKey = ""),
    val userSettings: UserSettings = UserSettings(),
    val activeWorkoutState: WorkoutState = WorkoutState(),
    val nextReminder: NextReminderInfo? = null,
    val selectedWorkoutPreset: WorkoutConfig = WorkoutConfig.PRESET_30_15,
    val weeklyMinutes: List<Int> = List(7) { 0 },
    val todayWorkoutPlan: WorkoutPlan? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PulseBreakApp
    private val repository = app.repository
    private val preferencesRepository = app.preferencesRepository

    private val _selectedPreset = MutableStateFlow(WorkoutConfig.PRESET_30_15)
    val selectedPreset: StateFlow<WorkoutConfig> = _selectedPreset.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getDailyTracker(),
        preferencesRepository.userSettingsFlow,
        preferencesRepository.remindersFlow,
        WorkoutForegroundService.activeWorkoutState,
        _selectedPreset,
        repository.getDailyTrackers(getLast7DayKeys()),
        repository.getAllWorkoutPlans()
    ) { args: Array<Any?> ->
        val dailyRecord = args[0] as? DailyTrackerRecord
        val settings = args[1] as UserSettings
        val reminders = args[2] as List<ReminderConfig>
        val workoutState = args[3] as WorkoutState
        val preset = args[4] as WorkoutConfig
        val weeklyRecords = args[5] as List<DailyTrackerRecord>
        val workoutPlans = args[6] as List<WorkoutPlan>

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        
        val greeting = when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..22 -> "Good evening"
            else -> "Late night"
        }

        val nextReminderInfo = calculateNextReminder(reminders)
        
        val weeklyMinsList = getLast7DayKeys().map { key ->
            weeklyRecords.find { it.dateKey == key }?.workoutMinutes ?: 0
        }

        val todayPlan = workoutPlans.find { it.dayOfWeek == dayOfWeek }

        HomeUiState(
            greeting = greeting,
            subtitle = if (workoutState.isRunning) "Workout in progress" else "Ready to move?",
            dailyTracker = dailyRecord ?: DailyTrackerRecord(dateKey = ""),
            userSettings = settings,
            activeWorkoutState = workoutState,
            nextReminder = nextReminderInfo,
            selectedWorkoutPreset = preset,
            weeklyMinutes = weeklyMinsList,
            todayWorkoutPlan = todayPlan
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    private fun getLast7DayKeys(): List<String> {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return (0..6).reversed().map { offset ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -offset)
            sdf.format(cal.time)
        }
    }

    fun selectPreset(config: WorkoutConfig) {
        _selectedPreset.value = config
    }

    fun quickAddWater() {
        viewModelScope.launch {
            val settings = uiState.value.userSettings
            repository.incrementWater(target = settings.waterDailyTarget)
        }
    }

    fun quickAddMoveBreak() {
        viewModelScope.launch {
            val settings = uiState.value.userSettings
            repository.incrementMoveBreak(target = settings.moveDailyTarget)
        }
    }

    fun startQuickWorkout() {
        val todayPlan = uiState.value.todayWorkoutPlan
        val config = if (todayPlan != null && !todayPlan.isRestDay) {
            val planned = try {
                val moshi = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
                val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.domain.model.PlannedExercise::class.java)
                moshi.adapter<List<com.example.domain.model.PlannedExercise>>(type).fromJson(todayPlan.plannedExercisesJson) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            
            WorkoutConfig(
                id = "planned_today",
                name = todayPlan.planName,
                workDurationSec = todayPlan.defaultWorkSec,
                restDurationSec = todayPlan.defaultRestSec,
                totalRounds = todayPlan.defaultRounds,
                plannedExercises = planned
            )
        } else {
            _selectedPreset.value
        }
        
        viewModelScope.launch {
            WorkoutForegroundService.start(
                getApplication(),
                config.copy(plannedExercises = repository.enrichPlannedExercises(config.plannedExercises))
            )
        }
    }

    private fun calculateNextReminder(reminders: List<ReminderConfig>): NextReminderInfo? {
        val nowMs = System.currentTimeMillis()
        val enabledReminders = reminders.filter { it.isEnabled }
        if (enabledReminders.isEmpty()) return null

        var closestType: ReminderType? = null
        var closestDiffMin = Int.MAX_VALUE

        for (reminder in enabledReminders) {
            val nextMs = ReminderScheduler.calculateNextTriggerTime(reminder, nowMs)
            val diffMin = ((nextMs - nowMs) / (60 * 1000L)).toInt().coerceAtLeast(1)
            if (diffMin < closestDiffMin) {
                closestDiffMin = diffMin
                closestType = reminder.type
            }
        }

        return closestType?.let { type ->
            val title = when (type) {
                ReminderType.WATER -> "Drink water"
                ReminderType.STAND_MOVE -> "Stand & Move"
                ReminderType.STRETCH -> "Stretch break"
            }
            NextReminderInfo(
                title = title,
                minutesAway = closestDiffMin,
                type = type
            )
        }
    }
}
