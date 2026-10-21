package com.nocta.app.data.repository

import android.content.Context
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
        const val RECORD_SEPARATOR = "\u001e"
        const val FIELD_SEPARATOR = "\u001f"
        val DEFAULT_WAKE_TIME: LocalTime = LocalTime.of(7, 0)
    }
}
