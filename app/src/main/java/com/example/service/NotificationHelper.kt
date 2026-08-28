package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.model.ReminderType
import com.example.domain.model.WorkoutPhase
import com.example.domain.model.WorkoutState
import java.util.Locale

object NotificationHelper {

    const val CHANNEL_WORKOUT_ID = "pulsebreak_workout_channel"
    const val CHANNEL_REMINDERS_ID = "pulsebreak_reminders_channel"

    const val NOTIFICATION_ID_WORKOUT = 1001
    const val NOTIFICATION_ID_WATER = 2001
    const val NOTIFICATION_ID_STAND = 2002
    const val NOTIFICATION_ID_STRETCH = 2003

    const val ACTION_PAUSE = "com.example.pulsebreak.ACTION_PAUSE"
    const val ACTION_RESUME = "com.example.pulsebreak.ACTION_RESUME"
    const val ACTION_END = "com.example.pulsebreak.ACTION_END"

    const val ACTION_RECORD_WATER = "com.example.pulsebreak.ACTION_RECORD_WATER"
    const val ACTION_RECORD_MOVE = "com.example.pulsebreak.ACTION_RECORD_MOVE"
    const val ACTION_SNOOZE_REMINDER = "com.example.pulsebreak.ACTION_SNOOZE_REMINDER"
    const val EXTRA_REMINDER_TYPE = "extra_reminder_type"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val workoutChannel = NotificationChannel(
                CHANNEL_WORKOUT_ID,
                "Active Workout Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active interval countdown and controls"
                setShowBadge(false)
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                "Wellness Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Periodic notifications for hydration and movement breaks"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(workoutChannel)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    fun buildWorkoutNotification(context: Context, state: WorkoutState): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to_workout", true)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val minutes = state.secondsRemaining / 60
        val seconds = state.secondsRemaining % 60
        val timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

        val phaseLabel = when (state.phase) {
            WorkoutPhase.WORK -> "WORK"
            WorkoutPhase.REST -> "REST"
            WorkoutPhase.PREPARE -> "PREPARE"
            WorkoutPhase.FINISHED -> "COMPLETED"
            WorkoutPhase.IDLE -> "IDLE"
        }

        val contentTitle = if (state.isPaused) {
            "Workout Paused"
        } else {
            "Workout in progress"
        }

        val contentText = "Round ${state.currentRound} of ${state.totalRounds} · $phaseLabel · $timeString"

        val builder = NotificationCompat.Builder(context, CHANNEL_WORKOUT_ID)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Add Pause or Resume action
        if (state.isPaused) {
            val resumeIntent = Intent(context, WorkoutNotificationReceiver::class.java).apply {
                action = ACTION_RESUME
            }
            val resumePending = PendingIntent.getBroadcast(
                context,
                1,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePending)
        } else {
            val pauseIntent = Intent(context, WorkoutNotificationReceiver::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePending = PendingIntent.getBroadcast(
                context,
                2,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePending)
        }

        // Add End action
        val endIntent = Intent(context, WorkoutNotificationReceiver::class.java).apply {
            action = ACTION_END
        }
        val endPending = PendingIntent.getBroadcast(
            context,
            3,
            endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "End", endPending)

        return builder.build()
    }

    fun buildReminderNotification(context: Context, type: ReminderType): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            10,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, text, notificationId, recordAction) = when (type) {
            ReminderType.WATER -> Quadruple(
                "Time to drink some water",
                "Take a short hydration break.",
                NOTIFICATION_ID_WATER,
                ACTION_RECORD_WATER
            )
            ReminderType.STAND_MOVE -> Quadruple(
                "Time to move",
                "Stand up and move for a couple of minutes.",
                NOTIFICATION_ID_STAND,
                ACTION_RECORD_MOVE
            )
            ReminderType.STRETCH -> Quadruple(
                "Time to stretch",
                "Do a quick 60-second stretch and reset your posture.",
                NOTIFICATION_ID_STRETCH,
                ACTION_RECORD_MOVE
            )
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Action: Record / Mark Done
        val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = recordAction
            putExtra(EXTRA_REMINDER_TYPE, type.name)
        }
        val donePending = PendingIntent.getBroadcast(
            context,
            notificationId + 10,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val doneLabel = if (type == ReminderType.WATER) "I Drank" else "I Moved"
        builder.addAction(android.R.drawable.checkbox_on_background, doneLabel, donePending)

        // Action: Snooze 10 min
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_REMINDER
            putExtra(EXTRA_REMINDER_TYPE, type.name)
        }
        val snoozePending = PendingIntent.getBroadcast(
            context,
            notificationId + 20,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze 10m", snoozePending)

        return builder.build()
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
