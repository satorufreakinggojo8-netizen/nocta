package com.nocta.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NoctaDestination(val route: String, val label: String, val icon: ImageVector) {
    data object Home : NoctaDestination("home", "Home", Icons.Outlined.Home)
    data object Sleep : NoctaDestination("sleep", "Sleep", Icons.Outlined.Bedtime)
    data object Coach : NoctaDestination("coach", "Coach", Icons.Outlined.SmartToy)
    data object Insights : NoctaDestination("insights", "Insights", Icons.Outlined.Insights)
    data object Profile : NoctaDestination("profile", "Profile", Icons.Outlined.Person)

    companion object {
        val bottomNavItems = listOf(Home, Sleep, Coach, Insights, Profile)
    }
}

// Routes reachable outside the bottom nav (pushed on top of it).
object NoctaRoutes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val ONBOARDING = "onboarding"
    const val SLEEP_PROFILE = "sleep_profile"
    const val LOG_SLEEP = "log_sleep"
    const val SLEEP_DETAILS = "sleep_details/{sessionId}"
    const val AI_CONVERSATION = "ai_conversation/{conversationId}"
    const val WIND_DOWN = "wind_down"
    const val SETTINGS = "settings"
}
