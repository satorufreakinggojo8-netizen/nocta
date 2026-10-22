package com.nocta.app.domain.model

import java.time.LocalTime

/**
 * Named schedules the planner can switch between. Each preset stores its own
 * wake time, so moving from a school night to the weekend (and back) does not
 * lose the other schedule.
 */
enum class SchedulePreset(val label: String, val shortLabel: String) {
    SCHOOL_NIGHT("School night", "School"),
    WEEKEND("Weekend", "Weekend"),
    CUSTOM("Custom", "Custom");

    /** Default wake time used the first time a preset is selected. */
    val defaultWakeTime: LocalTime
        get() = when (this) {
            SCHOOL_NIGHT -> LocalTime.of(6, 30)
            WEEKEND -> LocalTime.of(8, 30)
            CUSTOM -> LocalTime.of(7, 0)
        }

    /** Whether the preset is one of the two with a built-in wake time. */
    val hasBuiltInWakeTime: Boolean get() = this != CUSTOM
}

/**
 * Planner display rules. Wind-down length is user-configurable, but the range is
 * bounded so the plan can never recommend a two-hour routine or a zero one.
 */
object PlannerLimits {
    const val MIN_WIND_DOWN_MINUTES = 15
    const val MAX_WIND_DOWN_MINUTES = 90
    const val STEP_WIND_DOWN_MINUTES = 5
    const val DEFAULT_WIND_DOWN_MINUTES = 45

    fun clampWindDownMinutes(minutes: Int): Int {
        val clamped = minutes.coerceIn(MIN_WIND_DOWN_MINUTES, MAX_WIND_DOWN_MINUTES)
        // Round to the nearest step so +/- never lands on an odd number.
        return ((clamped + STEP_WIND_DOWN_MINUTES / 2) / STEP_WIND_DOWN_MINUTES) *
            STEP_WIND_DOWN_MINUTES
    }
}
