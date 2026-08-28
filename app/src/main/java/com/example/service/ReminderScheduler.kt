package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.domain.model.ReminderConfig
import com.example.domain.model.ReminderType
import java.util.Calendar

object ReminderScheduler {

    const val ACTION_TRIGGER_REMINDER = "com.example.pulsebreak.ACTION_TRIGGER_REMINDER"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_REMINDER_TYPE = "extra_reminder_type"

    fun scheduleReminder(context: Context, reminder: ReminderConfig) {
        if (!reminder.isEnabled) {
            cancelReminder(context, reminder)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val nextTriggerMs = calculateNextTriggerTime(reminder)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_REMINDER_TYPE, reminder.type.name)
        }

        val requestCode = reminder.type.ordinal * 100 + 1
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerMs,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // In case of exact alarm permission restriction on newer Android
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                nextTriggerMs,
                pendingIntent
            )
        }
    }

    fun snoozeReminder(context: Context, type: ReminderType, snoozeMinutes: Int = 10) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerMs = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_REMINDER_ID, "snooze_${type.name}")
            putExtra(EXTRA_REMINDER_TYPE, type.name)
        }

        val requestCode = type.ordinal * 100 + 50
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerMs,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerMs,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context, reminder: ReminderConfig) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_REMINDER_TYPE, reminder.type.name)
        }
        val requestCode = reminder.type.ordinal * 100 + 1
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun calculateNextTriggerTime(reminder: ReminderConfig, fromEpochMs: Long = System.currentTimeMillis()): Long {
        val now = Calendar.getInstance().apply { timeInMillis = fromEpochMs }
        val startCal = Calendar.getInstance().apply {
            timeInMillis = fromEpochMs
            set(Calendar.HOUR_OF_DAY, reminder.startHour)
            set(Calendar.MINUTE, reminder.startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            timeInMillis = fromEpochMs
            set(Calendar.HOUR_OF_DAY, reminder.endHour)
            set(Calendar.MINUTE, reminder.endMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If today is after end time or now is past today's active window
        if (now.after(endCal)) {
            // Schedule for tomorrow's start time
            startCal.add(Calendar.DAY_OF_YEAR, 1)
            return startCal.timeInMillis
        } else if (now.before(startCal)) {
            // Schedule for today's start time
            return startCal.timeInMillis
        } else {
            // Inside active window: now + interval
            val candidate = now.timeInMillis + (reminder.intervalMinutes * 60 * 1000L)
            return if (candidate > endCal.timeInMillis) {
                // If candidate exceeds end of day, schedule for tomorrow's start time
                startCal.add(Calendar.DAY_OF_YEAR, 1)
                startCal.timeInMillis
            } else {
                candidate
            }
        }
    }
}
