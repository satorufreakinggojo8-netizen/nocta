package com.nocta.app.domain.usecase

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Circular statistics for clock times (minutes since midnight).
 *
 * Clock times wrap: 11:50 p.m. and 12:10 a.m. are 20 minutes apart, not almost
 * 24 hours apart. Extracted from CalculateSleepScore so trend calculations use
 * exactly the same definition of "consistent" as the score itself.
 */
object CircularClockStats {

    const val MINUTES_PER_DAY = 24.0 * 60.0

    /** Mean clock time (minutes since midnight) of the given values. */
    fun meanClockMinutes(clockMinutes: List<Double>): Double {
        if (clockMinutes.isEmpty()) return 0.0
        val angle = clockMinutes.map { it / MINUTES_PER_DAY * 2.0 * Math.PI }
        val meanAngle = atan2(angle.sumOf(::sin), angle.sumOf(::cos))
        return ((meanAngle / (2.0 * Math.PI) * MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
    }

    /** Average absolute circular distance from the circular mean, in minutes. */
    fun meanAbsoluteDeviation(clockMinutes: List<Double>): Double {
        if (clockMinutes.isEmpty()) return 0.0
        val mean = meanClockMinutes(clockMinutes)
        return clockMinutes.map { absoluteDelta(it, mean) }.average()
    }

    /** Unsigned circular distance between two clock times, in minutes (0–720). */
    fun absoluteDelta(a: Double, b: Double): Double {
        val direct = kotlin.math.abs(a - b)
        return minOf(direct, MINUTES_PER_DAY - direct)
    }

    /** Signed circular difference a − b normalized to (−720, +720]; negative = a is earlier. */
    fun signedDelta(a: Double, b: Double): Double {
        var diff = ((a - b) % MINUTES_PER_DAY + MINUTES_PER_DAY) % MINUTES_PER_DAY
        if (diff > MINUTES_PER_DAY / 2) diff -= MINUTES_PER_DAY
        return diff
    }
}
