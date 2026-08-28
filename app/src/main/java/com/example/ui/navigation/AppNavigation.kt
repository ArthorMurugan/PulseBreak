package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.graphics.Color
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
    object Workout : Screen("workout", "Workout", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter)
    object Reminders : Screen("reminders", "Reminders", Icons.Filled.Notifications, Icons.Outlined.Notifications)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    object ActiveWorkout : Screen("active_workout", "Active", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter)
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
        Screen.Workout,
        Screen.Reminders,
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
                    containerColor = Color(0xFF1C1B1F),
                    tonalElevation = 0.dp,
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
                                selectedIconColor = Color(0xFF1D192B),
                                selectedTextColor = Color(0xFFE8DEF8),
                                indicatorColor = Color(0xFFE8DEF8),
                                unselectedIconColor = Color(0xFFCAC4D0).copy(alpha = 0.7f),
                                unselectedTextColor = Color(0xFFCAC4D0).copy(alpha = 0.7f)
                            ),
                            onClick = {
                                navController.navigate(screen.route) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    // on the back stack as users select items
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        // We'll skip saveState/restoreState temporarily to debug
                                        // if it's causing the "stuck" navigation
                                    }
                                    // Avoid multiple copies of the same destination when
                                    // reselecting the same item
                                    launchSingleTop = true
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
                    }
                )
            }

            composable(Screen.Workout.route) {
                WorkoutSetupScreen(
                    viewModel = workoutViewModel,
                    onNavigateToActiveWorkout = {
                        navController.navigate(Screen.ActiveWorkout.route)
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
        }
    }
}
