package com.nocta.app.ui.screens.optimize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocta.app.domain.profile.SleepProfile
import com.nocta.app.domain.repository.SettingsRepository
import com.nocta.app.domain.repository.SleepProfileRepository
import com.nocta.app.domain.repository.SleepRepository
import com.nocta.app.domain.repository.WindDownStep
import com.nocta.app.domain.usecase.BedtimePlan
import com.nocta.app.domain.usecase.PlanBedtime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class OptimizeUiState(
    val isLoading: Boolean = true,
    val plan: BedtimePlan? = null,
    val wakeTime: LocalTime = LocalTime.of(7, 0),
    val windDownSteps: List<WindDownStep> = emptyList(),
    val message: String? = null
)

/**
 * The Optimize area: an adaptive bedtime plan grounded in the user's real
 * sleep history, plus their editable evening wind-down routine.
 */
@HiltViewModel
class OptimizeViewModel @Inject constructor(
    private val sleepRepository: SleepRepository,
    sleepProfileRepository: SleepProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val planBedtime: PlanBedtime
) : ViewModel() {

    private val _uiState = MutableStateFlow(OptimizeUiState())
    val uiState: StateFlow<OptimizeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                sleepRepository.observeRecentSessions(days = HISTORY_DAYS),
                sleepProfileRepository.observeProfile(),
                settingsRepository.observeWakeTime(),
                settingsRepository.observeWindDownSteps()
            ) { recent, profile, wakeTime, steps ->
                val plan = planBedtime(
                    today = LocalDate.now(),
                    wakeTime = wakeTime,
                    recentSessions = recent,
                    profile = profile ?: SleepProfile(age = DEFAULT_PROFILE_AGE)
                )
                OptimizeUiState(
                    isLoading = false,
                    plan = plan,
                    wakeTime = wakeTime,
                    windDownSteps = steps
                )
            }.collect { _uiState.value = it }
        }
    }

    fun setStepEnabled(stepId: String, enabled: Boolean) {
        val updated = _uiState.value.windDownSteps.map {
            if (it.id == stepId) it.copy(isEnabled = enabled) else it
        }
        if (updated == _uiState.value.windDownSteps) return
        _uiState.update { it.copy(windDownSteps = updated) }
        viewModelScope.launch { settingsRepository.setWindDownSteps(updated) }
    }

    fun shiftWakeTime(minutes: Int) {
        val current = _uiState.value.wakeTime
        val next = LocalTime.ofSecondOfDay(
            (((current.toSecondOfDay() + minutes) % 86_400 + 86_400) % 86_400).toLong()
        )
        viewModelScope.launch { settingsRepository.setWakeTime(next) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private companion object {
        const val HISTORY_DAYS = 30
        const val DEFAULT_PROFILE_AGE = 18
    }
}
