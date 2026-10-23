package com.nocta.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NoctaDestination(val route: String, val label: String, val icon: ImageVector) {
    data object Home : NoctaDestination("home", "Home", Icons.Outlined.Home)
    data object Sleep : NoctaDestination("sleep", "Sleep", Icons.Outlined.Bedtime)
    data object Optimize : NoctaDestination("optimize", "Optimize", Icons.Outlined.Nightlight)
    data object Coach : NoctaDestination("coach", "Coach", Icons.Outlined.SmartToy)
    data object Insights : NoctaDestination("insights", "Insights", Icons.Outlined.Insights)
    data object Profile : NoctaDestination("profile", "Profile", Icons.Outlined.Person)

    companion object {
        /**
         * Kept as a plain list; the bar reads [bottomNavDestinations] instead.
         * That top-level list initializes the destination objects directly and
         * avoids the sealed-class companion being touched while the nested
         * objects are still being initialised.
         */
        val bottomNavItems: List<NoctaDestination>
            get() = bottomNavDestinations
    }
}

/**
 * The six primary destinations, in bar order: Home, Sleep, Coach, Optimize,
 * Insights, Profile — the coach sits next to sleep, and Optimize follows it.
 */
val bottomNavDestinations: List<NoctaDestination> = listOf(
    NoctaDestination.Home,
    NoctaDestination.Sleep,
    NoctaDestination.Coach,
    NoctaDestination.Optimize,
    NoctaDestination.Insights,
    NoctaDestination.Profile
)

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
    const val JOURNAL = "journal"
    const val SETTINGS = "settings"

    // Focused explanation pages. Long-form copy lives here instead of on the
    // main tabs (see docs/03-SCREENS.md — screen composition rules).
    const val RECOMMENDATION = "recommendation"
    const val SLEEP_DEBT = "sleep_debt"
    const val SCORE_HELP = "score_help"
    const val PLAN_WHY = "plan_why"
    const val PRIVACY = "privacy"

    /**
     * Coach opened from a tip bubble, with the question already attached. Kept
     * separate from the tab route so the tab's back-stack behaviour and
     * highlighting stay untouched.
     */
    const val COACH_ASK = "coach_ask/{prompt}"
}
