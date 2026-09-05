package com.nocta.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nocta.app.ui.components.NoctaBottomNavigation
import com.nocta.app.ui.screens.coach.CoachScreen
import com.nocta.app.ui.screens.home.HomeScreen
import com.nocta.app.ui.theme.NoctaTextSecondary

/**
 * Screens marked 🧩 in docs/03-SCREENS.md render ComingSoonScreen — a real,
 * navigable placeholder rather than omitted/fabricated content.
 */
@Composable
fun NoctaNavGraph(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in NoctaDestination.bottomNavItems.map { it.route }) {
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
                        navController.navigate(NoctaRoutes.SLEEP_DETAILS.replace("{sessionId}", sessionId))
                    },
                    onOpenCoach = { navController.navigate(NoctaDestination.Coach.route) },
                    onOpenWindDown = { navController.navigate(NoctaRoutes.WIND_DOWN) },
                    onOpenInsights = { navController.navigate(NoctaDestination.Insights.route) },
                    onLogSleep = { navController.navigate(NoctaRoutes.LOG_SLEEP) }
                )
            }
            composable(NoctaDestination.Sleep.route) {
                ComingSoonScreen("Sleep")
            }
            composable(NoctaDestination.Coach.route) {
                CoachScreen()
            }
            composable(NoctaDestination.Insights.route) {
                ComingSoonScreen("Insights")
            }
            composable(NoctaDestination.Profile.route) {
                ComingSoonScreen("Profile")
            }
            composable(NoctaRoutes.LOG_SLEEP) { ComingSoonScreen("Log Sleep") }
            composable(NoctaRoutes.WIND_DOWN) { ComingSoonScreen("Wind-Down Mode") }
            composable(NoctaRoutes.SLEEP_DETAILS) { ComingSoonScreen("Sleep Details") }
        }
    }
}

@Composable
private fun ComingSoonScreen(name: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("$name — coming soon", color = NoctaTextSecondary)
    }
}
