package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.AppNavigation
import com.example.ui.navigation.Screen
import com.example.ui.theme.PulseBreakTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        checkActivityRecognitionPermission()
    }

    private val requestActivityRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startStepTracker()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val navigateToWorkout = intent?.getBooleanExtra("navigate_to_workout", false) ?: false
        val preferencesRepository = (application as PulseBreakApp).preferencesRepository

        setContent {
            val settings by preferencesRepository.userSettingsFlow.collectAsStateWithLifecycle(
                initialValue = com.example.data.preferences.UserSettings()
            )

            PulseBreakTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColorEnabled
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    LaunchedEffect(Unit) {
                        checkNotificationPermission()
                    }

                    LaunchedEffect(intent) {
                        if (navigateToWorkout) {
                            navController.navigate(Screen.ActiveWorkout.route) {
                                // Pop everything up to home so we don't have multiple instances
                                popUpTo(Screen.Home.route) { inclusive = false }
                            }
                        }
                    }

                    AppNavigation(
                        navController = navController,
                        startDestination = Screen.Home.route
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                checkActivityRecognitionPermission()
            }
        } else {
            checkActivityRecognitionPermission()
        }
    }

    private fun checkActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                requestActivityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            } else {
                startStepTracker()
            }
        } else {
            startStepTracker()
        }
    }

    private fun startStepTracker() {
        val intent = Intent(this, com.example.service.StepTrackerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
