package com.example.domain.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class AudioHapticFeedback(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Plays a pure sine wave tone asynchronously without external file dependencies.
     */
    fun playTone(freqHz: Float, durationMs: Int, volume: Float = 0.9f) {
        scope.launch {
            try {
                val sampleRate = 44100
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val generatedSnd = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val angle = 2.0 * Math.PI * i / (sampleRate / freqHz)
                    // Apply a brief fade-in/fade-out envelope to avoid audio clicks
                    val envelope = when {
                        i < sampleRate * 0.01 -> i / (sampleRate * 0.01)
                        i > numSamples - sampleRate * 0.01 -> (numSamples - i) / (sampleRate * 0.01)
                        else -> 1.0
                    }
                    val sample = (sin(angle) * 32767 * volume * envelope).toInt().toShort()
                    generatedSnd[i] = sample
                }

                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(numSamples * 2, minBufferSize)

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(generatedSnd, 0, generatedSnd.size)
                audioTrack.play()

                // Wait for playback and release
                kotlinx.coroutines.delay(durationMs.toLong() + 50)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                // Silently handle any audio track errors
            }
        }
    }

    fun playCountdownBeep(beepSound: Boolean = true, vibration: Boolean = true) {
        if (beepSound) {
            playTone(freqHz = 880f, durationMs = 120, volume = 0.7f) // A5 pip
        }
        if (vibration) {
            vibrate(pattern = longArrayOf(0, 80), amplitudes = intArrayOf(0, 150))
        }
    }

    fun playWorkIntervalAlert(beepSound: Boolean = true, vibration: Boolean = true) {
        if (beepSound) {
            scope.launch {
                playTone(freqHz = 1046.5f, durationMs = 280, volume = 1.0f) // High C6 tone
            }
        }
        if (vibration) {
            // Crisp double pulse
            vibrate(pattern = longArrayOf(0, 120, 80, 180), amplitudes = intArrayOf(0, 255, 0, 255))
        }
    }

    fun playRestIntervalAlert(beepSound: Boolean = true, vibration: Boolean = true) {
        if (beepSound) {
            scope.launch {
                playTone(freqHz = 587.33f, durationMs = 280, volume = 0.9f) // D5 calm lower tone
            }
        }
        if (vibration) {
            vibrate(pattern = longArrayOf(0, 250), amplitudes = intArrayOf(0, 180))
        }
    }

    fun playCompletionFanfare(beepSound: Boolean = true, vibration: Boolean = true) {
        if (beepSound) {
            scope.launch {
                playTone(freqHz = 523.25f, durationMs = 150) // C5
                kotlinx.coroutines.delay(160)
                playTone(freqHz = 659.25f, durationMs = 150) // E5
                kotlinx.coroutines.delay(160)
                playTone(freqHz = 783.99f, durationMs = 150) // G5
                kotlinx.coroutines.delay(160)
                playTone(freqHz = 1046.5f, durationMs = 450) // High C6
            }
        }
        if (vibration) {
            vibrate(
                pattern = longArrayOf(0, 100, 80, 100, 80, 250),
                amplitudes = intArrayOf(0, 180, 0, 200, 0, 255)
            )
        }
    }

    private fun vibrate(pattern: LongArray, amplitudes: IntArray) {
        try {
            if (vibrator == null || !vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (vibrator.hasAmplitudeControl() && pattern.size == amplitudes.size) {
                    val effect = VibrationEffect.createWaveform(pattern, amplitudes, -1)
                    vibrator.vibrate(effect)
                } else {
                    val effect = VibrationEffect.createWaveform(pattern, -1)
                    vibrator.vibrate(effect)
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            // Ignore vibration permission or hardware exceptions
        }
    }
}
