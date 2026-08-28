package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StackedLineChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StackedLineChart
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.reminders.RemindersScreen
import com.example.ui.reminders.RemindersViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.workout.ActiveWorkoutScreen
import com.example.ui.workout.WorkoutSetupScreen
import com.example.ui.workout.WorkoutViewModel

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Planner : Screen("planner", "Planner", Icons.Filled.DateRange, Icons.Outlined.DateRange)
    object Workout : Screen("workout", "Quick", Icons.Filled.FlashOn, Icons.Outlined.FlashOn)
    object History : Screen("history", "History", Icons.Filled.StackedLineChart, Icons.Outlined.StackedLineChart)
    object Settings : Screen("settings", "Settings", Icons.Filled.Tune, Icons.Outlined.Tune)
    object ActiveWorkout : Screen("active_workout", "Active", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter)
    object Reminders : Screen("reminders", "Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications)
    object Nutrition : Screen("nutrition", "Nutrition", Icons.Filled.Notifications, Icons.Outlined.Notifications)
    object Progress : Screen("progress", "Progress", Icons.Filled.History, Icons.Outlined.History)
    object Library : Screen("library", "Library", Icons.Filled.History, Icons.Outlined.History)
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Planner,
        Screen.Workout,
        Screen.History,
        Screen.Settings
    )

    // Show bottom bar on all screens except ActiveWorkout
    val showBottomBar = currentDestination?.route != Screen.ActiveWorkout.route

    // Share ViewModels across screens if needed
    val homeViewModel: HomeViewModel = viewModel()
    val workoutViewModel: WorkoutViewModel = viewModel()
    val remindersViewModel: RemindersViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    bottomNavItems.forEach { screen ->
                        // Use hierarchy for more robust selection check
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Home.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToWorkout = {
                        navController.navigate(Screen.Workout.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToActiveWorkout = {
                        navController.navigate(Screen.ActiveWorkout.route)
                    },
                    onNavigateToReminders = {
                        navController.navigate(Screen.Reminders.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToHistory = {
                        navController.navigate(Screen.History.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToPlanner = {
                        navController.navigate(Screen.Planner.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToProgress = {
                        navController.navigate(Screen.Progress.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Workout.route) {
                WorkoutSetupScreen(
                    viewModel = workoutViewModel,
                    onNavigateToActiveWorkout = {
                        navController.navigate(Screen.ActiveWorkout.route)
                    },
                    onNavigateToLibrary = {
                        navController.navigate(Screen.Library.route)
                    }
                )
            }

            composable(Screen.History.route) {
                com.example.ui.workout.HistoryScreen(
                    viewModel = workoutViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.ActiveWorkout.route) {
                ActiveWorkoutScreen(
                    viewModel = workoutViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Reminders.route) {
                RemindersScreen(
                    viewModel = remindersViewModel
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel
                )
            }

            composable(Screen.Planner.route) {
                com.example.ui.planner.WeeklyPlannerScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onStartWorkout = {
                        homeViewModel.startQuickWorkout()
                        navController.navigate(Screen.ActiveWorkout.route)
                    }
                )
            }

            composable(Screen.Nutrition.route) {
                com.example.ui.nutrition.NutritionScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Progress.route) {
                com.example.ui.progress.ProgressScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Library.route) {
                com.example.ui.library.LibraryScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
