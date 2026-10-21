package com.nocta.app.data.repository

import android.content.Context
import com.nocta.app.domain.model.SleepScoreCardMode
import com.nocta.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    override fun observeSleepScoreCardMode(): Flow<SleepScoreCardMode> =
        sleepScoreCardModeFlow.asStateFlow()

    override suspend fun setSleepScoreCardMode(mode: SleepScoreCardMode) {
        prefs.edit().putString(KEY_SLEEP_SCORE_CARD_MODE, mode.name).apply()
        sleepScoreCardModeFlow.value = mode
    }

    private fun readSleepScoreCardMode(): SleepScoreCardMode {
        val stored = prefs.getString(KEY_SLEEP_SCORE_CARD_MODE, null)
        return SleepScoreCardMode.entries.firstOrNull { it.name == stored }
            ?: SleepScoreCardMode.STANDARD
    }

    private companion object {
        const val PREFS_NAME = "nocta_settings"
        const val KEY_SLEEP_SCORE_CARD_MODE = "sleep_score_card_mode"
    }
}
