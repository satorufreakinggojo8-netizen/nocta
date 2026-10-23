package com.nocta.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocta.app.domain.model.*
import com.nocta.app.domain.profile.SleepProfile
import com.nocta.app.domain.repository.SettingsRepository
import com.nocta.app.domain.repository.SleepProfileRepository
import com.nocta.app.domain.repository.SleepRepository
import com.nocta.app.domain.usecase.CalculateSleepScore
import com.nocta.app.domain.usecase.EstimateSleepDebt
import com.nocta.app.domain.usecase.PickRecommendation
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
    val todaysRecommendation: Recommendation? = null,
    val scoreCardMode: SleepScoreCardMode = SleepScoreCardMode.STANDARD
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sleepRepository: SleepRepository,
    private val settingsRepository: SettingsRepository,
    private val sleepProfileRepository: SleepProfileRepository,
    private val calculateSleepScore: CalculateSleepScore,
    private val estimateSleepDebt: EstimateSleepDebt,
    private val pickRecommendation: PickRecommendation
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                sleepRepository.observeLastNight(),
                sleepRepository.observeRecentSessions(days = 14),
                sleepProfileRepository.observeProfile()
            ) { lastNight, recent, profile ->
                // Score against the user's saved profile age when one exists;
                // fall back to the documented default until they set it
                // (see NOCTA_CLAUDE_CODE_INSTRUCTIONS §9 — no hard-coded
                // profile when a saved one exists).
                val effectiveProfile = profile ?: SleepProfile(age = DEFAULT_PROFILE_AGE)
                val breakdown = calculateSleepScore(recent, effectiveProfile)
                HomeUiState(
                    isLoading = false,
                    lastNight = lastNight,
                    scoreBreakdown = breakdown,
                    todaysRecommendation = pickRecommendation(lastNight, breakdown),
                    scoreCardMode = _uiState.value.scoreCardMode
                )
            }.collect { _uiState.value = it }
        }
        viewModelScope.launch {
            settingsRepository.observeSleepScoreCardMode().collect { mode ->
                _uiState.value = _uiState.value.copy(scoreCardMode = mode)
            }
        }
        viewModelScope.launch { sleepRepository.syncPending() }
    }

    fun setScoreCardMode(mode: SleepScoreCardMode) {
        viewModelScope.launch {
            settingsRepository.setSleepScoreCardMode(mode)
        }
    }

    private companion object {
        const val DEFAULT_PROFILE_AGE = 18
    }
}
