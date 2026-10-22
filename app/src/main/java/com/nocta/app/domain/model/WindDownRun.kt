package com.nocta.app.domain.model

import java.time.LocalDate

enum class WindDownOutcome { RUNNING, COMPLETED, DISMISSED }

/**
 * One run of the evening wind-down routine. The transition logic lives here (not
 * in the ViewModel) so pause/resume/complete behaviour is deterministic and
 * unit-tested with an explicit clock.
 *
 * Time accounting:
 * - `startedAtMillis` is when the run began.
 * - `accumulatedPauseMillis` is the sum of every completed pause.
 * - `pausedAtMillis != null` means the run is frozen right now.
 * - `endedAtMillis` is set once the run is completed or dismissed, so history
 *   shows the real length even after the app restarts.
 */
data class WindDownRun(
    val id: String,
    val date: LocalDate,
    val plannedMinutes: Int,
    val startedAtMillis: Long,
    val accumulatedPauseMillis: Long = 0,
    val pausedAtMillis: Long? = null,
    val endedAtMillis: Long? = null,
    val outcome: WindDownOutcome = WindDownOutcome.RUNNING,
    val completedStepIds: Set<String> = emptySet()
) {
    val isPaused: Boolean get() = pausedAtMillis != null
    val isActive: Boolean get() = outcome == WindDownOutcome.RUNNING

    private fun endReference(nowMillis: Long): Long =
        endedAtMillis ?: pausedAtMillis ?: nowMillis

    /** Elapsed running time, excluding paused time, frozen after the run ends. */
    fun elapsedMillisAt(nowMillis: Long): Long =
        (endReference(nowMillis) - startedAtMillis - accumulatedPauseMillis).coerceAtLeast(0)

    fun elapsedMinutesAt(nowMillis: Long): Long = elapsedMillisAt(nowMillis) / 60_000

    fun remainingSecondsAt(nowMillis: Long): Long {
        val plannedSeconds = plannedMinutes.coerceAtLeast(0) * 60L
        return (plannedSeconds - elapsedMillisAt(nowMillis) / 1000).coerceIn(0, plannedSeconds)
    }

    fun progressAt(nowMillis: Long): Float {
        val plannedMillis = plannedMinutes.coerceAtLeast(1) * 60_000L
        return (elapsedMillisAt(nowMillis).toFloat() / plannedMillis).coerceIn(0f, 1f)
    }

    fun pause(nowMillis: Long): WindDownRun =
        if (!isActive || isPaused) this else copy(pausedAtMillis = nowMillis)

    fun resume(nowMillis: Long): WindDownRun {
        val pausedAt = pausedAtMillis ?: return this
        if (!isActive) return this
        return copy(
            accumulatedPauseMillis = accumulatedPauseMillis +
                (nowMillis - pausedAt).coerceAtLeast(0),
            pausedAtMillis = null
        )
    }

    fun toggleStep(stepId: String): WindDownRun {
        if (!isActive) return this
        val next = if (stepId in completedStepIds) {
            completedStepIds - stepId
        } else {
            completedStepIds + stepId
        }
        return copy(completedStepIds = next)
    }

    /** Ends the run. Completion is only valid for a still-running run. */
    fun finish(nowMillis: Long, outcome: WindDownOutcome): WindDownRun {
        require(outcome != WindDownOutcome.RUNNING) { "Finish needs a terminal outcome." }
        if (!isActive) return this
        val end = pausedAtMillis ?: nowMillis
        return copy(
            outcome = outcome,
            endedAtMillis = end.coerceAtLeast(startedAtMillis),
            pausedAtMillis = null
        )
    }
}
