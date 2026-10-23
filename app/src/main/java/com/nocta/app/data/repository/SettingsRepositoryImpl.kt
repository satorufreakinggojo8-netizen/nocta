package com.nocta.app.data.repository

import android.content.Context
import com.nocta.app.domain.model.PlannerLimits
import com.nocta.app.domain.model.SchedulePreset
import com.nocta.app.domain.model.SleepScoreCardMode
import com.nocta.app.domain.repository.SettingsRepository
import com.nocta.app.domain.repository.WindDownStep
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SharedPreferences-backed settings store. UI preferences are deliberately
 * kept out of the Room schema so adding one never risks a data migration on
 * the sleep database.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : SettingsRepository {

    private val prefs =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val sleepScoreCardModeFlow =
        MutableStateFlow(readSleepScoreCardMode())

    private val wakeTimeFlow = MutableStateFlow(readWakeTime())

    private val windDownStepsFlow = MutableStateFlow(readWindDownSteps())

    private val activePresetFlow = MutableStateFlow(readActivePreset())

    private val windDownMinutesFlow = MutableStateFlow(readWindDownMinutes())

    private val shadeGuideEnabledFlow = MutableStateFlow(readShadeGuideEnabled())

    override fun observeSleepScoreCardMode(): Flow<SleepScoreCardMode> =
        sleepScoreCardModeFlow.asStateFlow()

    override suspend fun setSleepScoreCardMode(mode: SleepScoreCardMode) {
        prefs.edit().putString(KEY_SLEEP_SCORE_CARD_MODE, mode.name).apply()
        sleepScoreCardModeFlow.value = mode
    }

    override fun observeWakeTime(): Flow<LocalTime> = wakeTimeFlow.asStateFlow()

    override suspend fun setWakeTime(time: LocalTime) {
        prefs.edit().putInt(KEY_WAKE_TIME, time.toSecondOfDay()).apply()
        wakeTimeFlow.value = time
    }

    override suspend fun getWakeTime(): LocalTime = wakeTimeFlow.value

    override fun observeActivePreset(): Flow<SchedulePreset> =
        activePresetFlow.asStateFlow()

    override suspend fun setActivePreset(preset: SchedulePreset) {
        prefs.edit().putString(KEY_ACTIVE_PRESET, preset.name).apply()
        activePresetFlow.value = preset
    }

    override suspend fun getActivePreset(): SchedulePreset = activePresetFlow.value

    override suspend fun getPresetWakeTime(preset: SchedulePreset): LocalTime? {
        val stored = prefs.getInt(presetKey(preset), -1)
        if (stored < 0) return null
        return runCatching { LocalTime.ofSecondOfDay(stored.toLong()) }.getOrNull()
    }

    override suspend fun setPresetWakeTime(preset: SchedulePreset, time: LocalTime) {
        prefs.edit().putInt(presetKey(preset), time.toSecondOfDay()).apply()
    }

    override fun observeWindDownMinutes(): Flow<Int> =
        windDownMinutesFlow.asStateFlow()

    override suspend fun setWindDownMinutes(minutes: Int) {
        val clamped = PlannerLimits.clampWindDownMinutes(minutes)
        prefs.edit().putInt(KEY_WIND_DOWN_MINUTES, clamped).apply()
        windDownMinutesFlow.value = clamped
    }

    override suspend fun getWindDownMinutes(): Int = windDownMinutesFlow.value

    override fun observeWindDownSteps(): Flow<List<WindDownStep>> =
        windDownStepsFlow.asStateFlow()

    override suspend fun setWindDownSteps(steps: List<WindDownStep>) {
        // Custom steps and toggles are stored as "id\u001flabel\u001fenabled"
        // records; default steps are recreated when the stored list is absent
        // or malformed, so a corrupt pref can never crash the screen.
        val encoded = steps.joinToString(RECORD_SEPARATOR) { step ->
            listOf(step.id, step.label, step.isEnabled.toString())
                .joinToString(FIELD_SEPARATOR)
        }
        prefs.edit().putString(KEY_WIND_DOWN_STEPS, encoded).apply()
        windDownStepsFlow.value = steps
    }

    override suspend fun getWindDownSteps(): List<WindDownStep> = readWindDownSteps()

    override fun observeShadeGuideEnabled(): Flow<Boolean> =
        shadeGuideEnabledFlow.asStateFlow()

    override suspend fun setShadeGuideEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHADE_GUIDE_ENABLED, enabled).apply()
        shadeGuideEnabledFlow.value = enabled
    }

    override suspend fun getShadeGuideEnabled(): Boolean = shadeGuideEnabledFlow.value

    private fun readSleepScoreCardMode(): SleepScoreCardMode {
        val stored = prefs.getString(KEY_SLEEP_SCORE_CARD_MODE, null)
        return SleepScoreCardMode.entries.firstOrNull { it.name == stored }
            ?: SleepScoreCardMode.STANDARD
    }

    private fun readWakeTime(): LocalTime {
        val stored = prefs.getInt(KEY_WAKE_TIME, -1)
        return runCatching { LocalTime.ofSecondOfDay(stored.toLong()) }
            .getOrDefault(DEFAULT_WAKE_TIME)
    }

    private fun readActivePreset(): SchedulePreset {
        val stored = prefs.getString(KEY_ACTIVE_PRESET, null)
        return SchedulePreset.entries.firstOrNull { it.name == stored }
            ?: SchedulePreset.CUSTOM
    }

    private fun readShadeGuideEnabled(): Boolean =
        prefs.getBoolean(KEY_SHADE_GUIDE_ENABLED, true)

    private fun readWindDownMinutes(): Int {
        val stored = prefs.getInt(KEY_WIND_DOWN_MINUTES, PlannerLimits.DEFAULT_WIND_DOWN_MINUTES)
        return PlannerLimits.clampWindDownMinutes(stored)
    }

    private fun readWindDownSteps(): List<WindDownStep> {
        val encoded = prefs.getString(KEY_WIND_DOWN_STEPS, null)
            ?: return defaultWindDownSteps()
        val parsed = encoded.split(RECORD_SEPARATOR).mapNotNull { record ->
            val fields = record.split(FIELD_SEPARATOR)
            if (fields.size != 3) return@mapNotNull null
            val enabled = fields[2].toBooleanStrictOrNull() ?: return@mapNotNull null
            WindDownStep(id = fields[0], label = fields[1], isEnabled = enabled)
        }
        return parsed.ifEmpty { defaultWindDownSteps() }
    }

    private fun presetKey(preset: SchedulePreset) = "$KEY_PRESET_WAKE_PREFIX${preset.name}"

    private fun defaultWindDownSteps() = listOf(
        WindDownStep("dim_lights", "Dim the lights", true),
        WindDownStep("phone_away", "Put your phone away", true),
        WindDownStep("warm_shower", "Take a warm shower or bath", true),
        WindDownStep("breathe", "Five minutes of slow breathing", true),
        WindDownStep("read", "Read or journal", false),
        WindDownStep("prep", "Prepare tomorrow — clothes, bag, first task", false)
    )

    private companion object {
        const val PREFS_NAME = "nocta_settings"
        const val KEY_SLEEP_SCORE_CARD_MODE = "sleep_score_card_mode"
        const val KEY_WAKE_TIME = "wake_time_seconds"
        const val KEY_WIND_DOWN_STEPS = "wind_down_steps"
        const val KEY_ACTIVE_PRESET = "active_schedule_preset"
        const val KEY_PRESET_WAKE_PREFIX = "preset_wake_"
        const val KEY_WIND_DOWN_MINUTES = "wind_down_minutes"
        // Tips start on; muting is remembered across restarts.
        const val KEY_SHADE_GUIDE_ENABLED = "shade_guide_enabled"
        const val RECORD_SEPARATOR = "\u001e"
        const val FIELD_SEPARATOR = "\u001f"
        val DEFAULT_WAKE_TIME: LocalTime = LocalTime.of(7, 0)
    }
}
