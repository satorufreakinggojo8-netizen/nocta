package com.nocta.app.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocta.app.domain.profile.SleepProfile
import com.nocta.app.domain.repository.SleepProfileRepository
import com.nocta.app.domain.repository.SleepRepository
import com.nocta.app.domain.usecase.CalculateSleepTrends
import com.nocta.app.domain.usecase.TrendSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class TrendWindow(val days: Int, val label: String) {
    WEEK(7, "7 days"),
    MONTH(30, "30 days"),
    QUARTER(90, "90 days")
}

data class InsightsUiState(
    val isLoading: Boolean = true,
    val window: TrendWindow = TrendWindow.WEEK,
    val summary: TrendSummary? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val sleepRepository: SleepRepository,
    sleepProfileRepository: SleepProfileRepository,
    private val calculateSleepTrends: CalculateSleepTrends
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // 90 days covers the longest selectable window plus its score history.
            combine(
                sleepRepository.observeRecentSessions(days = MAX_WINDOW_DAYS),
                sleepProfileRepository.observeProfile(),
                _uiState
            ) { sessions, profile, ui ->
                ui to calculateSleepTrends(
                    sessions = sessions,
                    windowDays = ui.window.days,
                    today = LocalDate.now(),
                    profile = profile ?: SleepProfile(age = DEFAULT_PROFILE_AGE)
                )
            }.catch { throwable ->
                _uiState.value = InsightsUiState(
                    isLoading = false,
                    window = _uiState.value.window,
                    errorMessage = "Couldn't load your sleep history: ${throwable.message ?: "database error"}"
                )
            }.collect { (ui, summary) ->
                _uiState.value = ui.copy(isLoading = false, summary = summary, errorMessage = null)
            }
        }
    }

    fun selectWindow(window: TrendWindow) {
        _uiState.value = _uiState.value.copy(window = window)
    }

    private companion object {
        const val MAX_WINDOW_DAYS = 90
        const val DEFAULT_PROFILE_AGE = 18
    }
}
