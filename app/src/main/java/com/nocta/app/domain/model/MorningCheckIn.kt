package com.nocta.app.domain.model

data class MorningCheckIn(
    val sleepQuality: Int,
    val nightAwakenings: Int,
    val awakeMinutes: Int,
    val morningEnergy: Int,
    val disturbance: SleepDisturbance,
    val unusualEvent: String? = null
)

enum class SleepDisturbance {
    NONE,
    NOISE,
    TEMPERATURE,
    LIGHT,
    STRESS,
    PHONE,
    PHYSICAL_DISCOMFORT,
    OTHER
}
