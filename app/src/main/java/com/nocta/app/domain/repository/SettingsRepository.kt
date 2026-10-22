package com.nocta.app.domain.repository

import com.nocta.app.domain.model.SchedulePreset
import com.nocta.app.domain.model.SleepScoreCardMode
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

/** A single step in the user's editable wind-down routine. */
data class WindDownStep(
    val id: String,
    val label: String,
    val isEnabled: Boolean
)

/**
 * Lightweight user-interface preferences (display choices and schedule
 * settings rather than sleep data). Backed by SharedPreferences on the data
 * layer; see docs/01-ARCHITECTURE.md — sleep data itself stays in Room.
 */
interface SettingsRepository {
    fun observeSleepScoreCardMode(): Flow<SleepScoreCardMode>
    suspend fun setSleepScoreCardMode(mode: SleepScoreCardMode)

    /** The time the user needs to be awake; anchors the bedtime plan. */
    fun observeWakeTime(): Flow<LocalTime>
    suspend fun setWakeTime(time: LocalTime)
    suspend fun getWakeTime(): LocalTime

    /** Which named schedule is active (school night / weekend / custom). */
    fun observeActivePreset(): Flow<SchedulePreset>
    suspend fun setActivePreset(preset: SchedulePreset)
    suspend fun getActivePreset(): SchedulePreset

    /** Wake time saved for a preset, or null if the user never set one. */
    suspend fun getPresetWakeTime(preset: SchedulePreset): LocalTime?
    suspend fun setPresetWakeTime(preset: SchedulePreset, time: LocalTime)

    /** How long the evening wind-down lasts; feeds PlanBedtime and the routine. */
    fun observeWindDownMinutes(): Flow<Int>
    suspend fun setWindDownMinutes(minutes: Int)
    suspend fun getWindDownMinutes(): Int

    /** The user's editable evening wind-down routine, in order. */
    fun observeWindDownSteps(): Flow<List<WindDownStep>>
    suspend fun setWindDownSteps(steps: List<WindDownStep>)
    suspend fun getWindDownSteps(): List<WindDownStep>
}
