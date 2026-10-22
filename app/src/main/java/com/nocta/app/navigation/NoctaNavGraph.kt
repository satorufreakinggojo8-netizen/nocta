package com.nocta.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nocta.app.ui.components.NoctaBottomNavigation
import com.nocta.app.ui.screens.coach.CoachScreen
import com.nocta.app.ui.screens.details.SleepDetailScreen
import com.nocta.app.ui.screens.home.HomeScreen
import com.nocta.app.ui.screens.insights.InsightsScreen
import com.nocta.app.ui.screens.journal.JournalScreen
import com.nocta.app.ui.screens.log.LogSleepScreen
import com.nocta.app.ui.screens.optimize.OptimizeScreen
import com.nocta.app.ui.screens.profile.ProfileScreen
import com.nocta.app.ui.screens.sleep.SleepScreen
import com.nocta.app.ui.theme.NoctaBackground

@Composable
fun NoctaNavGraph(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = NoctaBackground,
        bottomBar = {
            if (currentRoute in bottomNavDestinations.map { it.route }) {
                NoctaBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(NoctaDestination.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = NoctaDestination.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(NoctaDestination.Home.route) {
                HomeScreen(
                    onOpenSleepDetails = { sessionId ->
                        navController.navigate(
                            NoctaRoutes.SLEEP_DETAILS.replace("{sessionId}", sessionId)
                        )
                    },
                    onOpenCoach = { navController.navigate(NoctaDestination.Coach.route) },
                    onOpenWindDown = {
                        navController.navigate(NoctaDestination.Optimize.route) {
                            launchSingleTop = true
                        }
                    },
                    onOpenInsights = { navController.navigate(NoctaDestination.Insights.route) },
                    onLogSleep = { navController.navigate(NoctaRoutes.LOG_SLEEP) }
                )
            }
            composable(NoctaDestination.Sleep.route) {
                SleepScreen(
                    onOpenSleepDetails = { sessionId ->
                        navController.navigate(
                            NoctaRoutes.SLEEP_DETAILS.replace("{sessionId}", sessionId)
                        )
                    },
                    onLogSleep = { navController.navigate(NoctaRoutes.LOG_SLEEP) },
                    onOpenJournal = { navController.navigate(NoctaRoutes.JOURNAL) }
                )
            }
            composable(NoctaRoutes.JOURNAL) {
                JournalScreen(onBack = { navController.popBackStack() })
            }
            composable(NoctaDestination.Optimize.route) {
                OptimizeScreen()
            }
            composable(NoctaDestination.Coach.route) {
                CoachScreen()
            }
            composable(NoctaDestination.Insights.route) {
                InsightsScreen()
            }
            composable(NoctaDestination.Profile.route) {
                ProfileScreen()
            }
            composable(NoctaRoutes.LOG_SLEEP) {
                LogSleepScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.popBackStack()
                    }
                )
            }
            // Legacy wind-down route: renders the Optimize routine directly so
            // any external caller lands on real content, not a placeholder.
            composable(NoctaRoutes.WIND_DOWN) { OptimizeScreen() }
            composable(
                route = NoctaRoutes.SLEEP_DETAILS,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) {
                SleepDetailScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
