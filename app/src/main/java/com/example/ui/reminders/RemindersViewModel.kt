package com.example.ui.reminders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PulseBreakApp
import com.example.data.database.DailyTrackerRecord
import com.example.data.preferences.UserSettings
import com.example.domain.model.ReminderConfig
import com.example.domain.model.ReminderType
import com.example.service.NotificationHelper
import com.example.service.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RemindersUiState(
    val reminders: List<ReminderConfig> = ReminderConfig.createDefaults(),
    val dailyTracker: DailyTrackerRecord = DailyTrackerRecord(dateKey = ""),
    val userSettings: UserSettings = UserSettings()
)

class RemindersViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PulseBreakApp
    private val repository = app.repository
    private val preferencesRepository = app.preferencesRepository

    val uiState: StateFlow<RemindersUiState> = combine(
        preferencesRepository.remindersFlow,
        repository.getDailyTracker(),
        preferencesRepository.userSettingsFlow
    ) { reminders, daily, settings ->
        RemindersUiState(
            reminders = reminders,
            dailyTracker = daily ?: DailyTrackerRecord(dateKey = ""),
            userSettings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RemindersUiState()
    )

    fun toggleReminder(reminderId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val currentList = uiState.value.reminders.toMutableList()
            val index = currentList.indexOfFirst { it.id == reminderId }
            if (index != -1) {
                val updated = currentList[index].copy(isEnabled = isEnabled)
                currentList[index] = updated
                preferencesRepository.saveReminders(currentList)

                if (isEnabled) {
                    ReminderScheduler.scheduleReminder(getApplication(), updated)
                } else {
                    ReminderScheduler.cancelReminder(getApplication(), updated)
                }
            }
        }
    }

    fun updateReminderConfig(
        reminderId: String,
        intervalMinutes: Int,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean
    ) {
        viewModelScope.launch {
            val currentList = uiState.value.reminders.toMutableList()
            val index = currentList.indexOfFirst { it.id == reminderId }
            if (index != -1) {
                val updated = currentList[index].copy(
                    intervalMinutes = intervalMinutes,
                    startHour = startHour,
                    startMinute = startMinute,
                    endHour = endHour,
                    endMinute = endMinute,
                    soundEnabled = soundEnabled,
                    vibrationEnabled = vibrationEnabled
                )
                currentList[index] = updated
                preferencesRepository.saveReminders(currentList)

                if (updated.isEnabled) {
                    ReminderScheduler.scheduleReminder(getApplication(), updated)
                }
            }
        }
    }

    fun addWaterDrink() {
        viewModelScope.launch {
            val settings = uiState.value.userSettings
            repository.incrementWater(target = settings.waterDailyTarget)
        }
    }

    fun removeWaterDrink() {
        viewModelScope.launch {
            repository.decrementWater()
        }
    }

    fun addMoveBreak() {
        viewModelScope.launch {
            val settings = uiState.value.userSettings
            repository.incrementMoveBreak(target = settings.moveDailyTarget)
        }
    }

    fun snooze(type: ReminderType, minutes: Int = 10) {
        ReminderScheduler.snoozeReminder(getApplication(), type, minutes)
    }

    fun testReminder(type: ReminderType) {
        NotificationHelper.createNotificationChannels(getApplication())
        val notification = NotificationHelper.buildReminderNotification(getApplication(), type)
        val notifId = when (type) {
            ReminderType.WATER -> NotificationHelper.NOTIFICATION_ID_WATER
            ReminderType.STAND_MOVE -> NotificationHelper.NOTIFICATION_ID_STAND
            ReminderType.STRETCH -> NotificationHelper.NOTIFICATION_ID_STRETCH
        }
        val notificationManager = getApplication<Application>().getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(notifId, notification)
    }
}
