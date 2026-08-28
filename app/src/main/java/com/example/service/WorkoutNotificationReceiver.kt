package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WorkoutNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            NotificationHelper.ACTION_PAUSE -> {
                WorkoutForegroundService.pause(context)
            }
            NotificationHelper.ACTION_RESUME -> {
                WorkoutForegroundService.resume(context)
            }
            NotificationHelper.ACTION_END -> {
                WorkoutForegroundService.end(context)
            }
        }
    }
}
