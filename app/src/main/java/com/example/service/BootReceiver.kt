package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == "android.intent.action.QUICKBOOT_POWERON") {
            val prefsRepository = UserPreferencesRepository(context)
            CoroutineScope(Dispatchers.IO).launch {
                val reminders = prefsRepository.remindersFlow.first()
                for (reminder in reminders) {
                    if (reminder.isEnabled) {
                        ReminderScheduler.scheduleReminder(context, reminder)
                    }
                }
            }
        }
    }
}
