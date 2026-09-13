package com.nocta.app.domain.model

import java.time.Duration
import java.time.Instant

/**
 * A start/stop tracking session — deliberately separate from [SleepSession].
 *
 * Rationale: [SleepSession] carries subjective inputs (sleepQuality,
 * morningEnergy, nightAwakenings) that cannot be known while a session is
 * still running, and must never be inferred from raw timestamps. A tracking
 * session is therefore only ever a [startedAt] + optional [endedAt] pair,
 * plus the [DataSource] that produced it. It is NOT fed into
 * CalculateSleepScore — the score pipeline is unchanged.
 */
data class SleepTrackingSession(
    val id: String,
    val startedAt: Instant,
    val endedAt: Instant?,
    val source: DataSource = DataSource.ESTIMATED,
    val synced: Boolean = false
) {
    val isActive: Boolean get() = endedAt == null

    /** Null while still tracking. */
    val duration: Duration? get() = endedAt?.let { Duration.between(startedAt, it) }

    /**
     * Confidence label shown in the UI. A tracked window is an estimate of
     * time-in-bed, never a clinical measurement — this is surfaced honestly
     * rather than implying wearable-grade accuracy.
     */
    val confidenceLabel: String get() = when (source) {
        DataSource.ESTIMATED -> "Estimated · start/stop tracking"
        DataSource.SELF_REPORTED -> "Self-reported"
        DataSource.HEALTH_CONNECT -> "Health Connect"
        DataSource.WEARABLE -> "Wearable"
    }
}

/** Typed errors for tracking operations. */
sealed class SleepTrackingError : Exception() {

    /** start() was called while a session was already active. */
    object AlreadyTracking : SleepTrackingError() {
        private fun readResolve(): Any = AlreadyTracking
        override val message get() = "A sleep session is already being tracked."
    }

    /** end() was called with no active session. */
    object NoActiveSession : SleepTrackingError() {
        private fun readResolve(): Any = NoActiveSession
        override val message get() = "There is no active sleep session to end."
    }

    /** A timestamp was in the future, or end < start. */
    data class InvalidTimestamps(val detail: String) : SleepTrackingError() {
        override val message get() = "Invalid sleep timestamps: $detail"
    }

    /** Duration exceeded the sanity ceiling (see SleepTrackingRules). */
    data class UnreasonableDuration(val actualMinutes: Long) : SleepTrackingError() {
        override val message get() =
            "Tracked duration of $actualMinutes minutes exceeds the 20-hour limit."
    }

    /** Room write failed. */
    data class StorageFailure(override val cause: Throwable) : SleepTrackingError() {
        override val message get() = "Could not save the sleep session."
    }
}
