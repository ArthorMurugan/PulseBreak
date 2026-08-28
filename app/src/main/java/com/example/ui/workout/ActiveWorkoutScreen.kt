package com.example.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.WorkoutPhase
import com.example.ui.components.CircularProgressRing
import com.example.ui.components.ExerciseMedia
import com.example.ui.components.exerciseAccentColor
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

    // Pulse animation during work
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val bgColor by animateColorAsState(
        targetValue = when {
            isWork -> MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha)
            isRest -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
            else -> MaterialTheme.colorScheme.background
        },
        label = "BackgroundColor"
    )

    val accentPrimary = when {
        isWork -> exerciseAccentColor(state.currentExercise?.bodyPart.orEmpty(), MaterialTheme.colorScheme)
        isRest -> MaterialTheme.colorScheme.secondary
        isFinished -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val accentLight = when {
        isWork -> MaterialTheme.colorScheme.primaryContainer
        isRest -> MaterialTheme.colorScheme.secondaryContainer
        isFinished -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val phaseText = when (state.phase) {
        WorkoutPhase.WORK -> "GO!"
        WorkoutPhase.REST -> "REST"
        WorkoutPhase.PREPARE -> "READY?"
        WorkoutPhase.FINISHED -> "DONE!"
        WorkoutPhase.IDLE -> "READY"
    }

    val minutes = state.secondsRemaining / 60
    val seconds = state.secondsRemaining % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
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
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                    .testTag("close_active_workout_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close View",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val elapsedMin = state.totalElapsedSeconds / 60
            val elapsedSec = state.totalElapsedSeconds % 60
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "TIME: ${String.format(Locale.getDefault(), "%02d:%02d", elapsedMin, elapsedSec)}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Workout Completed",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(72.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "WORKOUT SMASHED!",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "You finished ${state.totalRounds} rounds of beast mode.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(36.dp))

                Button(
                    onClick = {
                        viewModel.resetWorkout()
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("finish_done_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "FINISH SESSION",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
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
                // Exercise Image/GIF
                if (isWork && state.currentExercise != null) {
                    Card(
                        modifier = Modifier
                            .size(148.dp)
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        colors = CardDefaults.cardColors(containerColor = accentLight.copy(alpha = 0.55f))
                    ) {
                        ExerciseMedia(
                            mediaUrl = state.currentExercise!!.gifUrl,
                            contentDescription = state.currentExercise!!.name,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                // Round Header
                Text(
                    text = state.currentExercise?.name?.uppercase() ?: "ROUND ${state.currentRound} / ${state.totalRounds}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                if (state.currentExercise != null) {
                    Text(
                        text = "SET ${state.currentSet} OF ${state.totalSets}  •  ${state.reps} REPS",
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                        color = accentPrimary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                FlipTimerCard(
                    timeFormatted = timeFormatted,
                    phaseText = phaseText,
                    progress = state.progress,
                    accentColor = accentPrimary,
                    accentContainer = accentLight,
                    isPaused = state.isPaused
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Next Phase Preview Card
                val plannedExercises = state.config.plannedExercises
                val currentExerciseIndex = plannedExercises.indexOf(state.currentExercise)
                val nextExercise = plannedExercises.getOrNull(currentExerciseIndex + 1)
                val nextPhaseName = when {
                    state.phase == WorkoutPhase.REST && nextExercise != null -> "NEXT: ${nextExercise.name.uppercase()}"
                    state.nextPhase == WorkoutPhase.WORK -> "NEXT WORK"
                    state.nextPhase == WorkoutPhase.REST -> "NEXT REST"
                    state.nextPhase == WorkoutPhase.FINISHED -> "FINISH"
                    else -> ""
                }
                val nextMinutes = state.nextPhaseDurationSec / 60
                val nextSec = state.nextPhaseDurationSec % 60
                val nextTimeFormatted = String.format(Locale.getDefault(), "%02d:%02d", nextMinutes, nextSec)

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "$nextPhaseName IN $nextTimeFormatted",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Bottom Controls Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Pause / Resume Button
                    Button(
                        onClick = {
                            if (state.isPaused) viewModel.resumeWorkout() else viewModel.pauseWorkout()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .testTag("workout_pause_resume_button"),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isPaused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (state.isPaused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (state.isPaused) "Resume" else "Pause",
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.isPaused) "RESUME" else "PAUSE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    // Reset Button (as an icon button for space)
                    IconButton(
                        onClick = { viewModel.resetWorkout() },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("workout_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // End Workout Button
                TextButton(
                    onClick = {
                        viewModel.endWorkout()
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("workout_end_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "QUIT WORKOUT",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun FlipTimerCard(
    timeFormatted: String,
    phaseText: String,
    progress: Float,
    accentColor: Color,
    accentContainer: Color,
    isPaused: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = accentContainer.copy(alpha = 0.32f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = phaseText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    ),
                    color = accentColor
                )
                AnimatedVisibility(visible = isPaused, enter = fadeIn(), exit = fadeOut()) {
                    Surface(
                        modifier = Modifier.padding(start = 8.dp),
                        color = accentColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "PAUSED",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                timeFormatted.forEachIndexed { index, character ->
                    if (character == ':') {
                        Text(
                            text = ":",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            color = accentColor
                        )
                    } else {
                        FlipDigit(
                            digit = character,
                            key = "$index-$character",
                            accentColor = accentColor
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                color = accentColor,
                trackColor = accentContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun FlipDigit(digit: Char, key: String, accentColor: Color) {
    Surface(
        modifier = Modifier.size(width = 43.dp, height = 52.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = digit,
                transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                label = "timer_digit_$key"
            ) { currentDigit ->
                Text(
                    text = currentDigit.toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = accentColor
                )
            }
        }
    }
}
