package com.nocta.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocta.app.domain.model.*
import com.nocta.app.domain.repository.SleepRepository
import com.nocta.app.domain.usecase.CalculateSleepScore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val lastNight: SleepSession? = null,
    val scoreBreakdown: SleepScoreBreakdown? = null,
    val sleepDebt: SleepDebt? = null,
    val todaysRecommendation: Recommendation? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sleepRepository: SleepRepository,
    private val calculateSleepScore: CalculateSleepScore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                sleepRepository.observeLastNight(),
                sleepRepository.observeRecentSessions(days = 14)
            ) { lastNight, recent ->
                val breakdown = calculateSleepScore(recent)
                HomeUiState(
                    isLoading = false,
                    lastNight = lastNight,
                    scoreBreakdown = breakdown,
                    sleepDebt = estimateSleepDebt(recent),
                    todaysRecommendation = pickRecommendation(lastNight, breakdown)
                )
            }.collect { _uiState.value = it }
        }
        viewModelScope.launch { sleepRepository.syncPending() }
    }

    private fun estimateSleepDebt(recent: List<SleepSession>): SleepDebt {
        if (recent.isEmpty()) return SleepDebt(SleepDebtStatus.WELL_RESTED, 0)
        val targetMinutes = 8 * 60
        val avgMinutes = recent.map { it.durationMinutes }.average()
        val deficitPerNight = (targetMinutes - avgMinutes).coerceAtLeast(0.0)
        val approxDeficit = (deficitPerNight * recent.size).toInt()
        val status = when {
            approxDeficit < 60 -> SleepDebtStatus.WELL_RESTED
            approxDeficit < 300 -> SleepDebtStatus.SLIGHTLY_BEHIND
            else -> SleepDebtStatus.CATCHING_UP
        }
        return SleepDebt(status, approxDeficit)
    }

    /**
     * Minimal, transparent rule-based picker for the scaffold. In production this
     * call goes to the backend's insight engine (same data, same "no invented
     * claims" rule) so it can incorporate longer history and AI-generated phrasing —
     * see docs/01-ARCHITECTURE.md, AI architecture section.
     */
    private fun pickRecommendation(
        lastNight: SleepSession?,
        breakdown: SleepScoreBreakdown?
    ): Recommendation? {
        if (breakdown == null) return null
        return when {
            breakdown.consistencyScore < 70 -> Recommendation(
                id = "rec_consistency",
                title = "Steady your bedtime",
                why = "Your bedtime has varied more than usual over the last two weeks.",
                whatToDo = "Pick one bedtime and hold it for the next 5 nights, weekends included.",
                difficulty = Difficulty.MODERATE,
                expectedBenefit = "More consistent sleep timing tends to improve how rested you feel.",
                category = RecommendationCategory.SCHEDULE
            )
            (lastNight?.nightAwakenings ?: 0) >= 2 -> Recommendation(
                id = "rec_environment",
                title = "Check your sleep environment",
                why = "You logged multiple awakenings last night.",
                whatToDo = "Try a cooler, darker room tonight and see if awakenings drop.",
                difficulty = Difficulty.EASY,
                expectedBenefit = "A quieter, darker, cooler room commonly reduces night waking.",
                category = RecommendationCategory.ENVIRONMENT
            )
            else -> Recommendation(
                id = "rec_screens",
                title = "Ease off screens before bed",
                why = "Reducing late-night screen exposure may help you fall asleep more consistently.",
                whatToDo = "Try keeping your phone away from your bed tonight.",
                difficulty = Difficulty.EASY,
                expectedBenefit = "Many people fall asleep faster with less late-night screen light.",
                category = RecommendationCategory.WIND_DOWN
            )
        }
    }
}
