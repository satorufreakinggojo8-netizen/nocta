package com.nocta.app.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocta.app.domain.model.SleepDebt
import com.nocta.app.domain.profile.SleepProfile
import com.nocta.app.domain.repository.SettingsRepository
import com.nocta.app.domain.repository.SleepProfileRepository
import com.nocta.app.domain.repository.SleepRepository
import com.nocta.app.domain.usecase.BedtimePlan
import com.nocta.app.domain.usecase.EstimateSleepDebt
import com.nocta.app.domain.usecase.PlanBedtime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class SleepDebtUiState(
    val isLoading: Boolean = true,
    val debt: SleepDebt? = null,
    val nightsConsidered: Int = 0,
    val averageMinutes: Long? = null
)

/**
 * Explains the sleep-debt estimate using the same fourteen nights and the same
 * calculation as the Insights summary — never a second, different number.
 */
@HiltViewModel
class SleepDebtViewModel @Inject constructor(
    private val sleepRepository: SleepRepository,
    private val estimateSleepDebt: EstimateSleepDebt
) : ViewModel() {

    private val _uiState = MutableStateFlow(SleepDebtUiState())
    val uiState: StateFlow<SleepDebtUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sleepRepository.observeRecentSessions(days = WINDOW_DAYS).collect { recent ->
                _uiState.value = SleepDebtUiState(
                    isLoading = false,
                    debt = estimateSleepDebt(recent),
                    nightsConsidered = recent.size,
                    averageMinutes = recent
                        .takeIf { it.isNotEmpty() }
                        ?.map { it.durationMinutes }
                        ?.average()
                        ?.toLong()
                )
            }
        }
    }

    private companion object {
        const val WINDOW_DAYS = 14
    }
}

data class PlanWhyUiState(
    val isLoading: Boolean = true,
    val plan: BedtimePlan? = null
)

/**
 * The full reasoning behind tonight's plan, on its own page. It reads the
 * same stored settings and sleep history the Optimize tab does, so the two
 * screens cannot drift apart.
 */
@HiltViewModel
class PlanWhyViewModel @Inject constructor(
    sleepRepository: SleepRepository,
    sleepProfileRepository: SleepProfileRepository,
    settingsRepository: SettingsRepository,
    private val planBedtime: PlanBedtime
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanWhyUiState())
    val uiState: StateFlow<PlanWhyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                sleepRepository.observeRecentSessions(days = HISTORY_DAYS),
                sleepProfileRepository.observeProfile(),
                settingsRepository.observeWakeTime(),
                settingsRepository.observeWindDownMinutes()
            ) { sessions, profile, wakeTime, windDownMinutes ->
                planBedtime(
                    today = LocalDate.now(),
                    wakeTime = wakeTime,
                    recentSessions = sessions,
                    profile = profile ?: SleepProfile(age = DEFAULT_PROFILE_AGE),
                    windDownMinutes = windDownMinutes
                )
            }.collect { plan ->
                _uiState.value = PlanWhyUiState(isLoading = false, plan = plan)
            }
        }
    }

    private companion object {
        const val HISTORY_DAYS = 30
        const val DEFAULT_PROFILE_AGE = 18
    }
}
