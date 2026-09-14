package com.nocta.app.data.local

import androidx.room.Entity

@Entity(tableName = "morning_check_ins")
data class MorningCheckInEntity(
    @androidx.room.PrimaryKey
    val trackingSessionId: String,
    val sleepQuality: Int,
    val nightAwakenings: Int,
    val awakeMinutes: Int,
    val morningEnergy: Int,
    val disturbance: String,
    val unusualEvent: String?
)
