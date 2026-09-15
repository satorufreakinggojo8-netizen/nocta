package com.nocta.app.domain.profile

data class SleepProfile(
    val age: Int
) {
    val recommendedMinMinutes: Int
        get() = when {
            age in 6..12 -> 9 * 60
            age in 13..17 -> 8 * 60
            age in 18..60 -> 7 * 60
            age in 61..64 -> 7 * 60
            age >= 65 -> 7 * 60
            else -> 8 * 60
        }

    val recommendedMaxMinutes: Int
        get() = when {
            age in 6..12 -> 12 * 60
            age in 13..17 -> 10 * 60
            age in 18..60 -> 10 * 60
            age in 61..64 -> 9 * 60
            age >= 65 -> 8 * 60
            else -> 10 * 60
        }

    val recommendedSleepLabel: String
        get() = when {
            age in 6..12 -> "9–12 hours"
            age in 13..17 -> "8–10 hours"
            age in 18..60 -> "7–10 hours"
            age in 61..64 -> "7–9 hours"
            age >= 65 -> "7–8 hours"
            else -> "8–10 hours"
        }
}
