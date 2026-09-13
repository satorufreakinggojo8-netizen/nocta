package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.SleepTrackingError
import java.time.Duration
import java.time.Instant

/**
 * Pure validation logic for sleep tracking. No Android/Room dependencies so it
 * is directly unit-testable (see SleepTrackingRulesTest).
 *
 * All time is [Instant]-based (UTC); the UI is responsible for localising
 * display. This keeps the rules deterministic and timezone-agnostic.
 */
object SleepTrackingRules {

    /** 20 hours — anything longer is treated as a forgotten session, not sleep. */
    const val MAX_SESSION_MINUTES: Long = 20 * 60

    /** Clock-skew tolerance when validating "is this in the future?". */
    private val FUTURE_TOLERANCE: Duration = Duration.ofMinutes(2)

    /**
     * Validates a start request.
     * @throws SleepTrackingError.InvalidTimestamps if [startedAt] is future beyond tolerance.
     */
    fun validateStart(startedAt: Instant, now: Instant = Instant.now()) {
        if (startedAt.isAfter(now.plus(FUTURE_TOLERANCE))) {
            throw SleepTrackingError.InvalidTimestamps("start time is in the future")
        }
    }

    /**
     * Validates an end request against the session's start.
     * @throws SleepTrackingError.InvalidTimestamps if end < start or end is future beyond tolerance.
     * @throws SleepTrackingError.UnreasonableDuration if the session exceeds [MAX_SESSION_MINUTES].
     */
    fun validateEnd(startedAt: Instant, endedAt: Instant, now: Instant = Instant.now()) {
        if (endedAt.isBefore(startedAt)) {
            throw SleepTrackingError.InvalidTimestamps("end time is before start time")
        }
        if (endedAt.isAfter(now.plus(FUTURE_TOLERANCE))) {
            throw SleepTrackingError.InvalidTimestamps("end time is in the future")
        }
        val minutes = Duration.between(startedAt, endedAt).toMinutes()
        if (minutes > MAX_SESSION_MINUTES) {
            throw SleepTrackingError.UnreasonableDuration(minutes)
        }
    }

    /**
     * True when a persisted active session should be considered stale
     * (app was killed, user forgot to end). The screen surfaces this as an
     * explicit "still tracking? / discard" choice rather than silently
     * auto-ending, so we never invent an end time.
     */
    fun isStale(startedAt: Instant, now: Instant = Instant.now()): Boolean =
        Duration.between(startedAt, now).toMinutes() > MAX_SESSION_MINUTES
}
