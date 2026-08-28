package com.example.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.example.data.database.PulseBreakDatabase
import com.example.data.database.PulseBreakRepository
import com.example.domain.model.WorkoutConfig
import com.example.domain.model.WorkoutPhase
import com.example.domain.model.WorkoutState
import com.example.domain.timer.AudioHapticFeedback
import com.example.domain.timer.WorkoutTimerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkoutForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var stateCollectJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        instance = this
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        NotificationHelper.createNotificationChannels(this)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PulseBreak::WorkoutWakeLock"
        ).apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_START -> {
                val workSec = intent.getIntExtra(EXTRA_WORK_SEC, 30)
                val restSec = intent.getIntExtra(EXTRA_REST_SEC, 15)
                val rounds = intent.getIntExtra(EXTRA_ROUNDS, 8)
                val name = intent.getStringExtra(EXTRA_NAME) ?: "Interval"
                val sound = intent.getBooleanExtra(EXTRA_SOUND, true)
                val vibration = intent.getBooleanExtra(EXTRA_VIBRATION, true)
                val warning = intent.getIntExtra(EXTRA_WARNING, 3)
                val plannedJson = intent.getStringExtra(EXTRA_PLANNED_JSON)

                var plannedList = emptyList<com.example.domain.model.PlannedExercise>()
                if (plannedJson != null) {
                    val moshi = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
                    val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.domain.model.PlannedExercise::class.java)
                    plannedList = moshi.adapter<List<com.example.domain.model.PlannedExercise>>(type).fromJson(plannedJson) ?: emptyList()
                }

                val config = WorkoutConfig(
                    id = "active_session",
                    name = name,
                    workDurationSec = workSec,
                    restDurationSec = restSec,
                    totalRounds = rounds,
                    countdownSound = sound,
                    beepSound = sound,
                    vibrationEnabled = vibration,
                    countdownWarningSec = warning,
                    plannedExercises = plannedList
                )

                startWorkoutSession(config)
            }

            ACTION_PAUSE -> pauseWorkout()
            ACTION_RESUME -> resumeWorkout()
            ACTION_RESET -> resetWorkout()
            ACTION_END -> endWorkout()
        }

        return START_NOT_STICKY
    }

    private fun startWorkoutSession(config: WorkoutConfig) {
        wakeLock?.acquire(config.totalDurationSec * 1000L + 60000L) // Safe wakelock timeout

        if (engine == null) {
            val audioHaptic = AudioHapticFeedback(applicationContext)
            val repository = PulseBreakRepository(PulseBreakDatabase.getDatabase(applicationContext).pulseBreakDao())
            engine = WorkoutTimerEngine(
                audioHaptic = audioHaptic,
                onWorkoutCompleted = { completedConfig, durationSec ->
                    serviceScope.launch {
                        repository.saveCompletedWorkout(
                            presetName = completedConfig.name,
                            workSec = completedConfig.workDurationSec,
                            restSec = completedConfig.restDurationSec,
                            totalRounds = completedConfig.totalRounds,
                            completedRounds = completedConfig.totalRounds,
                            totalDurationSec = durationSec
                        )
                        stopServiceAndForeground()
                    }
                }
            )
        }

        engine?.startWorkout(config)

        // Show foreground notification
        val notification = NotificationHelper.buildWorkoutNotification(
            this,
            engine?.workoutState?.value ?: WorkoutState(config = config)
        )
        startForeground(NotificationHelper.NOTIFICATION_ID_WORKOUT, notification)

        // Observe timer state
        stateCollectJob?.cancel()
        stateCollectJob = serviceScope.launch {
            engine?.workoutState?.collect { state ->
                _activeWorkoutState.value = state

                if (state.isRunning) {
                    val updatedNotification = NotificationHelper.buildWorkoutNotification(
                        this@WorkoutForegroundService,
                        state
                    )
                    notificationManager.notify(NotificationHelper.NOTIFICATION_ID_WORKOUT, updatedNotification)
                }

                if (state.phase == WorkoutPhase.FINISHED || (!state.isRunning && state.phase == WorkoutPhase.IDLE)) {
                    stopServiceAndForeground()
                }
            }
        }
    }

    fun pauseWorkout() {
        engine?.pauseWorkout()
    }

    fun resumeWorkout() {
        engine?.resumeWorkout()
    }

    fun resetWorkout() {
        engine?.resetWorkout()
        stopServiceAndForeground()
    }

    fun endWorkout() {
        engine?.endWorkout()
        stopServiceAndForeground()
    }

    private fun stopServiceAndForeground() {
        try {
            stateCollectJob?.cancel()
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_WORKOUT)
            stopSelf()
        } catch (e: Exception) {
            // Safe cleanup
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        engine?.release()
        engine = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.pulsebreak.ACTION_START"
        const val ACTION_PAUSE = "com.example.pulsebreak.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.pulsebreak.ACTION_RESUME"
        const val ACTION_RESET = "com.example.pulsebreak.ACTION_RESET"
        const val ACTION_END = "com.example.pulsebreak.ACTION_END"

        const val EXTRA_WORK_SEC = "extra_work_sec"
        const val EXTRA_REST_SEC = "extra_rest_sec"
        const val EXTRA_ROUNDS = "extra_rounds"
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_SOUND = "extra_sound"
        const val EXTRA_VIBRATION = "extra_vibration"
        const val EXTRA_WARNING = "extra_warning"
        const val EXTRA_PLANNED_JSON = "extra_planned_json"

        private var instance: WorkoutForegroundService? = null
        private var engine: WorkoutTimerEngine? = null

        private val _activeWorkoutState = MutableStateFlow(WorkoutState())
        val activeWorkoutState: StateFlow<WorkoutState> = _activeWorkoutState.asStateFlow()

        fun start(context: Context, config: WorkoutConfig) {
            val intent = Intent(context, WorkoutForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_WORK_SEC, config.workDurationSec)
                putExtra(EXTRA_REST_SEC, config.restDurationSec)
                putExtra(EXTRA_ROUNDS, config.totalRounds)
                putExtra(EXTRA_NAME, config.name)
                putExtra(EXTRA_SOUND, config.beepSound)
                putExtra(EXTRA_VIBRATION, config.vibrationEnabled)
                putExtra(EXTRA_WARNING, config.countdownWarningSec)
                
                if (config.plannedExercises.isNotEmpty()) {
                    val moshi = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
                    val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.domain.model.PlannedExercise::class.java)
                    val json = moshi.adapter<List<com.example.domain.model.PlannedExercise>>(type).toJson(config.plannedExercises)
                    putExtra(EXTRA_PLANNED_JSON, json)
                }
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun pause(context: Context) {
            val intent = Intent(context, WorkoutForegroundService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, WorkoutForegroundService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun reset(context: Context) {
            val intent = Intent(context, WorkoutForegroundService::class.java).apply {
                action = ACTION_RESET
            }
            context.startService(intent)
        }

        fun end(context: Context) {
            val intent = Intent(context, WorkoutForegroundService::class.java).apply {
                action = ACTION_END
            }
            context.startService(intent)
        }
    }
}
