package com.example.domain.timer

import com.example.domain.model.WorkoutConfig
import com.example.domain.model.WorkoutPhase
import com.example.domain.model.WorkoutState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkoutTimerEngine(
    private val audioHaptic: AudioHapticFeedback? = null,
    private val onWorkoutCompleted: ((WorkoutConfig, Int) -> Unit)? = null
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var timerJob: Job? = null

    private val _workoutState = MutableStateFlow(WorkoutState())
    val workoutState: StateFlow<WorkoutState> = _workoutState.asStateFlow()

    fun startWorkout(config: WorkoutConfig) {
        timerJob?.cancel()
        
        val initialExercise = config.plannedExercises.firstOrNull()
        val initialPhase = WorkoutPhase.WORK
        val initialRemaining = initialExercise?.workDurationSec ?: config.workDurationSec

        _workoutState.value = WorkoutState(
            phase = initialPhase,
            currentRound = 1,
            totalRounds = config.totalRounds,
            secondsRemaining = initialRemaining,
            totalPhaseSeconds = initialRemaining,
            isPaused = false,
            isRunning = true,
            totalElapsedSeconds = 0,
            config = config,
            currentExercise = initialExercise,
            currentSet = 1,
            totalSets = initialExercise?.sets ?: 1,
            reps = initialExercise?.reps ?: 0
        )

        // Cue initial work alert
        audioHaptic?.playWorkIntervalAlert(
            beepSound = config.beepSound,
            vibration = config.vibrationEnabled
        )
        audioHaptic?.speak(initialExercise?.name ?: "Work")

        startTimerLoop()
    }

    fun pauseWorkout() {
        if (_workoutState.value.isRunning && !_workoutState.value.isPaused) {
            _workoutState.value = _workoutState.value.copy(isPaused = true)
        }
    }

    fun resumeWorkout() {
        if (_workoutState.value.isRunning && _workoutState.value.isPaused) {
            _workoutState.value = _workoutState.value.copy(isPaused = false)
        }
    }

    fun resetWorkout() {
        timerJob?.cancel()
        val currentConfig = _workoutState.value.config
        _workoutState.value = WorkoutState(
            phase = WorkoutPhase.IDLE,
            currentRound = 1,
            totalRounds = currentConfig.totalRounds,
            secondsRemaining = currentConfig.workDurationSec,
            totalPhaseSeconds = currentConfig.workDurationSec,
            isPaused = false,
            isRunning = false,
            totalElapsedSeconds = 0,
            config = currentConfig
        )
    }

    fun endWorkout() {
        timerJob?.cancel()
        val currentState = _workoutState.value
        val completedDuration = currentState.totalElapsedSeconds
        val completedRounds = if (currentState.phase == WorkoutPhase.WORK) {
            (currentState.currentRound - 1).coerceAtLeast(0)
        } else {
            currentState.currentRound
        }

        if (currentState.isRunning && completedDuration > 5) {
            onWorkoutCompleted?.invoke(currentState.config, completedDuration)
        }

        _workoutState.value = currentState.copy(
            phase = WorkoutPhase.IDLE,
            isRunning = false,
            isPaused = false
        )
    }

    fun release() {
        timerJob?.cancel()
        audioHaptic?.release()
    }

    private fun startTimerLoop() {
        timerJob = scope.launch {
            while (true) {
                delay(1000)
                val current = _workoutState.value
                if (!current.isRunning || current.isPaused || current.phase == WorkoutPhase.FINISHED || current.phase == WorkoutPhase.IDLE) {
                    continue
                }

                val newRemaining = current.secondsRemaining - 1
                val newElapsed = current.totalElapsedSeconds + 1

                if (newRemaining > 0) {
                    // Check if we need to play countdown warning (e.g. 3, 2, 1 before change)
                    if (current.config.countdownSound && newRemaining <= current.config.countdownWarningSec) {
                        audioHaptic?.playCountdownBeep(
                            beepSound = current.config.beepSound,
                            vibration = current.config.vibrationEnabled
                        )
                    }

                    _workoutState.value = current.copy(
                        secondsRemaining = newRemaining,
                        totalElapsedSeconds = newElapsed
                    )
                } else {
                    // Transition to next phase
                    handlePhaseTransition(current, newElapsed)
                }
            }
        }
    }

    private fun handlePhaseTransition(current: WorkoutState, totalElapsed: Int) {
        val config = current.config
        val plannedList = config.plannedExercises

        if (plannedList.isEmpty()) {
            handleLegacyPhaseTransition(current, totalElapsed)
            return
        }

        val currentIndex = plannedList.indexOf(current.currentExercise)
        val currentEx = current.currentExercise

        when (current.phase) {
            WorkoutPhase.WORK -> {
                // Switch to REST (if there's more sets or more exercises)
                val isLastSet = current.currentSet >= (currentEx?.sets ?: 1)
                val isLastExercise = currentIndex >= plannedList.size - 1
                val isCardioRhythm = plannedList.all { it.exerciseId.startsWith("cardio_") }

                if (isLastSet && isLastExercise && (!isCardioRhythm || current.currentRound >= current.totalRounds)) {
                    completeWorkout(config, totalElapsed)
                } else {
                    val nextDuration = currentEx?.restDurationSec ?: 15
                    _workoutState.value = current.copy(
                        phase = WorkoutPhase.REST,
                        secondsRemaining = nextDuration,
                        totalPhaseSeconds = nextDuration,
                        totalElapsedSeconds = totalElapsed
                    )
                    audioHaptic?.playRestIntervalAlert(beepSound = config.beepSound, vibration = config.vibrationEnabled)
                    audioHaptic?.speak("Rest")
                }
            }

            WorkoutPhase.REST -> {
                val isLastSet = current.currentSet >= (currentEx?.sets ?: 1)
                val isCardioRhythm = plannedList.all { it.exerciseId.startsWith("cardio_") }
                
                if (isLastSet) {
                    // Next exercise
                    val nextEx = plannedList.getOrNull(currentIndex + 1) ?: if (isCardioRhythm) plannedList.firstOrNull() else null
                    if (nextEx != null) {
                        _workoutState.value = current.copy(
                            phase = WorkoutPhase.WORK,
                            currentRound = if (isCardioRhythm && currentIndex >= plannedList.size - 1) current.currentRound + 1 else current.currentRound,
                            currentExercise = nextEx,
                            currentSet = 1,
                            totalSets = nextEx.sets,
                            reps = nextEx.reps,
                            secondsRemaining = nextEx.workDurationSec,
                            totalPhaseSeconds = nextEx.workDurationSec,
                            totalElapsedSeconds = totalElapsed
                        )
                        audioHaptic?.playWorkIntervalAlert(beepSound = config.beepSound, vibration = config.vibrationEnabled)
                        audioHaptic?.speak(nextEx.name)
                    } else {
                        completeWorkout(config, totalElapsed)
                    }
                } else {
                    // Next set of current exercise
                    val nextSet = current.currentSet + 1
                    val nextDuration = currentEx?.workDurationSec ?: 30
                    _workoutState.value = current.copy(
                        phase = WorkoutPhase.WORK,
                        currentSet = nextSet,
                        secondsRemaining = nextDuration,
                        totalPhaseSeconds = nextDuration,
                        totalElapsedSeconds = totalElapsed
                    )
                    audioHaptic?.playWorkIntervalAlert(beepSound = config.beepSound, vibration = config.vibrationEnabled)
                    audioHaptic?.speak("Set $nextSet")
                }
            }
            else -> {}
        }
    }

    private fun completeWorkout(config: WorkoutConfig, totalElapsed: Int) {
        _workoutState.value = _workoutState.value.copy(
            phase = WorkoutPhase.FINISHED,
            secondsRemaining = 0,
            totalPhaseSeconds = 0,
            isRunning = false,
            isPaused = false,
            totalElapsedSeconds = totalElapsed
        )
        audioHaptic?.playCompletionFanfare(beepSound = config.beepSound, vibration = config.vibrationEnabled)
        audioHaptic?.speak("Workout Completed")
        onWorkoutCompleted?.invoke(config, totalElapsed)
    }

    private fun handleLegacyPhaseTransition(current: WorkoutState, totalElapsed: Int) {
        val config = current.config

        when (current.phase) {
            WorkoutPhase.WORK -> {
                if (current.currentRound >= current.totalRounds) {
                    completeWorkout(config, totalElapsed)
                } else {
                    // Switch to REST
                    val nextDuration = config.restDurationSec
                    _workoutState.value = current.copy(
                        phase = WorkoutPhase.REST,
                        secondsRemaining = nextDuration,
                        totalPhaseSeconds = nextDuration,
                        totalElapsedSeconds = totalElapsed
                    )
                    audioHaptic?.playRestIntervalAlert(
                        beepSound = config.beepSound,
                        vibration = config.vibrationEnabled
                    )
                    audioHaptic?.speak("Rest")
                }
            }

            WorkoutPhase.REST -> {
                // Switch back to WORK for next round
                val nextRound = current.currentRound + 1
                val nextDuration = config.workDurationSec
                _workoutState.value = current.copy(
                    phase = WorkoutPhase.WORK,
                    currentRound = nextRound,
                    secondsRemaining = nextDuration,
                    totalPhaseSeconds = nextDuration,
                    totalElapsedSeconds = totalElapsed
                )
                audioHaptic?.playWorkIntervalAlert(
                    beepSound = config.beepSound,
                    vibration = config.vibrationEnabled
                )
                audioHaptic?.speak("Work")
            }

            else -> {}
        }
    }
}
