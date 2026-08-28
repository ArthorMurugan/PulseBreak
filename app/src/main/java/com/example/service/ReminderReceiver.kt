package com.example.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.database.PulseBreakDatabase
import com.example.data.database.PulseBreakRepository
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.model.ReminderType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val reminderTypeName = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_TYPE)
        val reminderType = try {
            if (reminderTypeName != null) ReminderType.valueOf(reminderTypeName) else null
        } catch (e: Exception) {
            null
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val repository = PulseBreakRepository(PulseBreakDatabase.getDatabase(context).pulseBreakDao())
        val prefsRepository = UserPreferencesRepository(context)

        when (action) {
            ReminderScheduler.ACTION_TRIGGER_REMINDER -> {
                if (reminderType != null) {
                    NotificationHelper.createNotificationChannels(context)
                    val notification = NotificationHelper.buildReminderNotification(context, reminderType)
                    val notifId = when (reminderType) {
                        ReminderType.WATER -> NotificationHelper.NOTIFICATION_ID_WATER
                        ReminderType.STAND_MOVE -> NotificationHelper.NOTIFICATION_ID_STAND
                        ReminderType.STRETCH -> NotificationHelper.NOTIFICATION_ID_STRETCH
                    }
                    notificationManager.notify(notifId, notification)

                    // Reschedule next regular reminder
                    CoroutineScope(Dispatchers.IO).launch {
                        val reminders = prefsRepository.remindersFlow.first()
                        val current = reminders.find { it.type == reminderType }
                        if (current != null && current.isEnabled) {
                            ReminderScheduler.scheduleReminder(context, current)
                        }
                    }
                }
            }

            NotificationHelper.ACTION_RECORD_WATER -> {
                notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_WATER)
                CoroutineScope(Dispatchers.IO).launch {
                    val settings = prefsRepository.userSettingsFlow.first()
                    repository.incrementWater(target = settings.waterDailyTarget)
                }
            }

            NotificationHelper.ACTION_RECORD_MOVE -> {
                val notifId = if (reminderType == ReminderType.STRETCH) {
                    NotificationHelper.NOTIFICATION_ID_STRETCH
                } else {
                    NotificationHelper.NOTIFICATION_ID_STAND
                }
                notificationManager.cancel(notifId)
                CoroutineScope(Dispatchers.IO).launch {
                    val settings = prefsRepository.userSettingsFlow.first()
                    repository.incrementMoveBreak(target = settings.moveDailyTarget)
                }
            }

            NotificationHelper.ACTION_SNOOZE_REMINDER -> {
                if (reminderType != null) {
                    val notifId = when (reminderType) {
                        ReminderType.WATER -> NotificationHelper.NOTIFICATION_ID_WATER
                        ReminderType.STAND_MOVE -> NotificationHelper.NOTIFICATION_ID_STAND
                        ReminderType.STRETCH -> NotificationHelper.NOTIFICATION_ID_STRETCH
                    }
                    notificationManager.cancel(notifId)
                    ReminderScheduler.snoozeReminder(context, reminderType, snoozeMinutes = 10)
                }
            }
        }
    }
}
