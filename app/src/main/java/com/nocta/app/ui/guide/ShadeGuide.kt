package com.nocta.app.ui.guide

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One short, screen-specific line from the companion.
 *
 * - [message] is the bubble itself: one sentence, no instructions the screen
 *   does not actually support.
 * - [detail] is the in-app guidance shown when the coach backend is missing,
 *   so a tap always produces a real answer instead of a dead end.
 * - [coachPrompt] is the question sent to the coach when AI is available.
 */
data class ShadeTip(
    val id: String,
    val message: String,
    val detail: String,
    val coachPrompt: String
)

/**
 * The tips shown per screen. Every statement matches a feature that exists in
 * this build — nothing here promises a screen the app does not have.
 *
 * Routes are plain strings (not the navigation sealed class) so this catalogue
 * stays a pure data table that unit tests can check without a NavController.
 */
object ShadeGuide {

    const val ROUTE_HOME = "home"
    const val ROUTE_SLEEP = "sleep"
    const val ROUTE_OPTIMIZE = "optimize"
    const val ROUTE_INSIGHTS = "insights"
    const val ROUTE_PROFILE = "profile"
    const val ROUTE_SLEEP_DETAILS = "sleep_details/{sessionId}"
    const val ROUTE_JOURNAL = "journal"
    const val ROUTE_RECOMMENDATION = "recommendation"
    const val ROUTE_SLEEP_DEBT = "sleep_debt"

    private val tipsByRoute: Map<String, List<ShadeTip>> = mapOf(
        ROUTE_HOME to listOf(
            ShadeTip(
                id = "home.score",
                message = "Your score comes from your recorded nights — tap it for the full night.",
                detail = "The score card opens last night's report. With no scored night yet it " +
                    "offers Log a night instead of showing a zero.",
                coachPrompt = "What lowered my sleep score most?"
            ),
            ShadeTip(
                id = "home.start",
                message = "Two ways in: log a night you remember, or plan tonight.",
                detail = "Log sleep records a night you already had. Wind down starts tonight's " +
                    "routine at the time your plan suggests.",
                coachPrompt = "Should I log last night or plan tonight?"
            )
        ),
        ROUTE_SLEEP to listOf(
            ShadeTip(
                id = "sleep.track",
                message = "Start tracking when you get into bed — stop it when you wake.",
                detail = "Tracking records the start and end only. Your morning check-in is what " +
                    "turns that window into a scored night.",
                coachPrompt = "How does sleep tracking work?"
            ),
            ShadeTip(
                id = "sleep.journal",
                message = "The journal covers what timestamps can't: caffeine, stress, screens.",
                detail = "Every journal question is optional. Patterns only appear after three " +
                    "nights on each side of a factor, and they are associations, not causes.",
                coachPrompt = "What do my journal entries suggest about my sleep?"
            )
        ),
        ROUTE_OPTIMIZE to listOf(
            ShadeTip(
                id = "optimize.plan",
                message = "Tonight's plan works backwards from your wake time.",
                detail = "Lights-out and wind-down start come from your saved wake time, your " +
                    "age range and your recent nights. Tap Why this plan for the reasoning.",
                coachPrompt = "Does my bedtime plan fit what you see in my records?"
            ),
            ShadeTip(
                id = "optimize.routine",
                message = "Run the wind-down rather than just reading it.",
                detail = "Start the routine, tick steps as you go, and pause if you need to. " +
                    "Only completed runs are saved to your history.",
                coachPrompt = "How have my wind-down routines been going?"
            )
        ),
        ROUTE_INSIGHTS to listOf(
            ShadeTip(
                id = "insights.range",
                message = "Switch the range to see whether a change is actually holding.",
                detail = "7 days shows this week; 30 and 90 days show the longer pattern. Every " +
                    "figure comes from saved nights only.",
                coachPrompt = "What stands out in my sleep trends?"
            ),
            ShadeTip(
                id = "insights.night",
                message = "Tap a bar to open that night's report.",
                detail = "Each bar is one saved night. Gaps are nights with no record — they are " +
                    "never drawn as zero hours.",
                coachPrompt = "Which recent night was my weakest, and why?"
            )
        ),
        ROUTE_PROFILE to listOf(
            ShadeTip(
                id = "profile.age",
                message = "Your age sets the sleep range behind scores and plans.",
                detail = "Nocta uses the recommended range for that age group. It is a wellness " +
                    "guideline, not a medical target.",
                coachPrompt = "Is my age-based sleep target realistic?"
            ),
            ShadeTip(
                id = "profile.data",
                message = "Your records stay on this device unless you add a backend.",
                detail = "Export a JSON copy or delete it here. The privacy page lists exactly " +
                    "what is stored and what can be sent to a backend you configure.",
                coachPrompt = "Where is my sleep data stored?"
            )
        ),
        ROUTE_SLEEP_DETAILS to listOf(
            ShadeTip(
                id = "details.numbers",
                message = "Every number here belongs to this one night.",
                detail = "Measured values and your check-in answers are shown separately, so you " +
                    "can tell what was recorded from what you reported.",
                coachPrompt = "Walk me through this night's report."
            )
        ),
        ROUTE_JOURNAL to listOf(
            ShadeTip(
                id = "journal.optional",
                message = "Answer only what you want — blank answers stay blank.",
                detail = "Unanswered questions are never treated as a zero, and a day with no " +
                    "answers is removed instead of saved empty.",
                coachPrompt = "How do my journal answers relate to my sleep?"
            )
        ),
        ROUTE_RECOMMENDATION to listOf(
            ShadeTip(
                id = "recommendation.one",
                message = "One small step, chosen from your recent records.",
                detail = "It is a suggestion based on your history, not a treatment plan. Skip " +
                    "it if it doesn't fit your week.",
                coachPrompt = "Is this recommendation right for me?"
            )
        ),
        ROUTE_SLEEP_DEBT to listOf(
            ShadeTip(
                id = "debt.method",
                message = "Sleep debt is an estimate, never a measurement.",
                detail = "It compares your recent nights with an eight-hour target and counts " +
                    "only nights you actually recorded.",
                coachPrompt = "How large is my sleep debt, really?"
            )
        )
    )

    fun tipsFor(baseRoute: String?): List<ShadeTip> =
        baseRoute?.let { tipsByRoute[it] } ?: emptyList()

    fun hasTips(baseRoute: String?): Boolean = tipsFor(baseRoute).isNotEmpty()

    /** Strips query arguments so `coach?prompt=…` maps to the same tip set. */
    fun baseRouteOf(route: String?): String? = route?.substringBefore("?")
}

/**
 * Picks the next tip for a screen, cycling so re-entering a screen does not
 * show the same line twice in a row. With a single tip per screen the same
 * line is the only honest choice.
 */
class ShadeTipRotator {

    private val lastShownIdByRoute = mutableMapOf<String, String>()

    fun next(baseRoute: String?): ShadeTip? {
        val tips = ShadeGuide.tipsFor(baseRoute)
        if (tips.isEmpty()) return null
        val key = baseRoute ?: ""
        val previous = lastShownIdByRoute[key]
        val tip = tips.firstOrNull { it.id != previous } ?: tips.first()
        lastShownIdByRoute[key] = tip.id
        return tip
    }
}

/**
 * Room the floating companion and its bubble need at the bottom of a screen.
 * Lists reserve it so their last row rests above the character instead of
 * underneath it.
 */
object ShadeLayout {

    /**
     * Height on the dashboard. Sized so the bubble above the head clears the
     * two action buttons at rest on an empty install (checked geometrically in
     * `AppShellTest`), while keeping the character prominent.
     */
    val FullCompanionHeight: Dp = 250.dp

    /** Smaller body on the other tabs, so the bubble does not dominate them. */
    val CompactCompanionHeight: Dp = 150.dp

    /** Character + a bubble of up to three lines on the dashboard. */
    val HomeBottomInset: Dp = 360.dp

    /** Character + a bubble of up to three lines on every other screen. */
    val CompactBottomInset: Dp = 266.dp

    val BubbleMaxWidth: Dp = 292.dp

    fun companionHeight(baseRoute: String?): Dp =
        if (baseRoute == ShadeGuide.ROUTE_HOME) FullCompanionHeight else CompactCompanionHeight

    fun bottomInset(baseRoute: String?): Dp =
        if (baseRoute == ShadeGuide.ROUTE_HOME) HomeBottomInset else CompactBottomInset
}
