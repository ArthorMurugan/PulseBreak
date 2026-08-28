package com.example.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.WorkoutPhase
import com.example.ui.components.CircularProgressRing
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PulseGreenLight
import com.example.ui.theme.PulseGreenPrimary
import com.example.ui.theme.RestCoralLight
import com.example.ui.theme.RestCoralPrimary
import java.util.Locale

@Composable
fun ActiveWorkoutScreen(
    viewModel: WorkoutViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.activeWorkoutState.collectAsStateWithLifecycle()

    val isWork = state.phase == WorkoutPhase.WORK
    val isRest = state.phase == WorkoutPhase.REST
    val isFinished = state.phase == WorkoutPhase.FINISHED

    val accentPrimary = when {
        isWork -> PulseGreenPrimary
        isRest -> RestCoralPrimary
        isFinished -> PulseGreenPrimary
        else -> PulseGreenPrimary
    }

    val accentLight = when {
        isWork -> PulseGreenLight
        isRest -> RestCoralLight
        isFinished -> PulseGreenLight
        else -> PulseGreenLight
    }

    val phaseText = when (state.phase) {
        WorkoutPhase.WORK -> "WORK"
        WorkoutPhase.REST -> "REST"
        WorkoutPhase.PREPARE -> "PREPARE"
        WorkoutPhase.FINISHED -> "COMPLETED"
        WorkoutPhase.IDLE -> "READY"
    }

    val minutes = state.secondsRemaining / 60
    val seconds = state.secondsRemaining % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
            .testTag("active_workout_screen")
    ) {
        // Top Bar: Dismiss/Close button & Total elapsed time
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .testTag("close_active_workout_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close View",
                    tint = Color.White
                )
            }

            val elapsedMin = state.totalElapsedSeconds / 60
            val elapsedSec = state.totalElapsedSeconds % 60
            Text(
                text = "Elapsed: ${String.format(Locale.getDefault(), "%02d:%02d", elapsedMin, elapsedSec)}",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                ),
                color = Color(0xFF94A3B8)
            )
        }

        if (isFinished) {
            // Finished Celebration View
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(PulseGreenPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Workout Completed",
                        tint = PulseGreenPrimary,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "WORKOUT COMPLETE",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "All ${state.totalRounds} rounds finished in ${state.totalElapsedSeconds / 60}m ${state.totalElapsedSeconds % 60}s",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(36.dp))

                Button(
                    onClick = {
                        viewModel.resetWorkout()
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("finish_done_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PulseGreenPrimary,
                        contentColor = DarkBackground
                    )
                ) {
                    Text(
                        text = "DONE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        } else {
            // Main Active Interval Interface
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Round Header: "ROUND 4 / 10"
                Text(
                    text = "ROUND ${state.currentRound} / ${state.totalRounds}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Phase Badge: "WORK" or "REST"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(accentPrimary.copy(alpha = 0.2f))
                        .border(1.dp, accentPrimary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = phaseText,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        ),
                        color = accentPrimary
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Giant Circular Progress Ring with Timer
                CircularProgressRing(
                    progress = state.progress,
                    size = 270.dp,
                    strokeWidth = 16.dp,
                    primaryColor = accentPrimary,
                    secondaryColor = accentLight,
                    trackColor = Color(0xFF1E293B)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = timeFormatted,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-1).sp
                            ),
                            color = Color.White
                        )

                        AnimatedVisibility(
                            visible = state.isPaused,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = "PAUSED",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                ),
                                color = RestCoralPrimary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Next Phase Preview Card
                val nextPhaseName = when (state.nextPhase) {
                    WorkoutPhase.WORK -> "WORK"
                    WorkoutPhase.REST -> "REST"
                    WorkoutPhase.FINISHED -> "FINISH"
                    else -> ""
                }
                val nextMinutes = state.nextPhaseDurationSec / 60
                val nextSec = state.nextPhaseDurationSec % 60
                val nextTimeFormatted = String.format(Locale.getDefault(), "%02d:%02d", nextMinutes, nextSec)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF161E2E))
                        .border(1.dp, Color(0xFF283548), RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "NEXT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "$nextPhaseName · $nextTimeFormatted",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White
                        )
                    }
                }
            }

            // Bottom Controls Row: PAUSE / RESUME, RESET, END WORKOUT
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Reset Button
                    OutlinedButton(
                        onClick = { viewModel.resetWorkout() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("workout_reset_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF94A3B8)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF334155))
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RESET",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    // Pause / Resume Button
                    Button(
                        onClick = {
                            if (state.isPaused) viewModel.resumeWorkout() else viewModel.pauseWorkout()
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(56.dp)
                            .testTag("workout_pause_resume_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isPaused) PulseGreenPrimary else Color(0xFF1E293B),
                            contentColor = if (state.isPaused) DarkBackground else Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (state.isPaused) "Resume" else "Pause",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.isPaused) "RESUME" else "PAUSE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                // End Workout Button
                Button(
                    onClick = {
                        viewModel.endWorkout()
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("workout_end_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RestCoralPrimary.copy(alpha = 0.15f),
                        contentColor = RestCoralPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "End Workout",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "END WORKOUT",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}
