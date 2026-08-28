package com.example.domain.model

enum class WorkoutPhase {
    IDLE,
    PREPARE, // Optional quick 3s countdown before round 1 starts
    WORK,
    REST,
    FINISHED
}

data class WorkoutState(
    val phase: WorkoutPhase = WorkoutPhase.IDLE,
    val currentRound: Int = 1,
    val totalRounds: Int = 8,
    val secondsRemaining: Int = 30,
    val totalPhaseSeconds: Int = 30,
    val isPaused: Boolean = false,
    val isRunning: Boolean = false,
    val totalElapsedSeconds: Int = 0,
    val config: WorkoutConfig = WorkoutConfig.PRESET_30_15
) {
    val progress: Float
        get() = if (totalPhaseSeconds > 0) {
            (totalPhaseSeconds - secondsRemaining).toFloat() / totalPhaseSeconds.toFloat()
        } else 0f

    val nextPhase: WorkoutPhase
        get() = when (phase) {
            WorkoutPhase.IDLE -> WorkoutPhase.WORK
            WorkoutPhase.PREPARE -> WorkoutPhase.WORK
            WorkoutPhase.WORK -> if (currentRound >= totalRounds) WorkoutPhase.FINISHED else WorkoutPhase.REST
            WorkoutPhase.REST -> WorkoutPhase.WORK
            WorkoutPhase.FINISHED -> WorkoutPhase.IDLE
        }

    val nextPhaseDurationSec: Int
        get() = when (nextPhase) {
            WorkoutPhase.WORK -> config.workDurationSec
            WorkoutPhase.REST -> config.restDurationSec
            else -> 0
        }
}
