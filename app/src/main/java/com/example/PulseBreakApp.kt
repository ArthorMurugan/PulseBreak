package com.example

import android.app.Application
import com.example.data.database.PulseBreakDatabase
import com.example.data.database.PulseBreakRepository
import com.example.data.preferences.UserPreferencesRepository
import com.example.service.NotificationHelper
import com.example.service.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PulseBreakApp : Application() {

    lateinit var database: PulseBreakDatabase
        private set

    lateinit var repository: PulseBreakRepository
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = PulseBreakDatabase.getDatabase(this)
        repository = PulseBreakRepository(database.pulseBreakDao())
        preferencesRepository = UserPreferencesRepository(this)

        NotificationHelper.createNotificationChannels(this)

        // Initialize scheduled reminders on first launch or startup
        CoroutineScope(Dispatchers.IO).launch {
            val reminders = preferencesRepository.remindersFlow.first()
            for (reminder in reminders) {
                if (reminder.isEnabled) {
                    ReminderScheduler.scheduleReminder(this@PulseBreakApp, reminder)
                }
            }
        }
    }

    companion object {
        lateinit var instance: PulseBreakApp
            private set
    }
}
