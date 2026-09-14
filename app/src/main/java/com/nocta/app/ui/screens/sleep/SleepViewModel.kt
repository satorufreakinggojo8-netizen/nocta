package com.nocta.app.ui.screens.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocta.app.domain.model.MorningCheckIn
import com.nocta.app.domain.model.SleepScoreBreakdown
import com.nocta.app.domain.model.SleepTrackingError
import com.nocta.app.domain.model.SleepTrackingSession
import com.nocta.app.domain.repository.MorningCheckInRepository
import com.nocta.app.domain.repository.SleepRepository
import com.nocta.app.domain.repository.SleepTrackingRepository
import com.nocta.app.domain.usecase.CalculateSleepScore
import com.nocta.app.domain.usecase.CreateSleepSession
import com.nocta.app.domain.usecase.SleepTrackingRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class SleepUiState(
    val isLoading: Boolean = true,
    val active: SleepTrackingSession? = null,
    val latestCompleted: SleepTrackingSession? = null,
    val latestCheckIn: MorningCheckIn? = null,
    val isStale: Boolean = false,
    val sleepScore: SleepScoreBreakdown? = null,
    val showMorningCheckIn: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SleepViewModel @Inject constructor(
    private val trackingRepository: SleepTrackingRepository,
    private val morningCheckInRepository: MorningCheckInRepository,
    private val sleepRepository: SleepRepository,
    private val createSleepSession: CreateSleepSession,
    private val calculateSleepScore: CalculateSleepScore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SleepUiState())
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()

    private val now = MutableStateFlow(Instant.now())

    init {
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                now.value = Instant.now()
            }
        }

        viewModelScope.launch {
            combine(
                trackingRepository.observeActive(),
                trackingRepository.observeLatestCompleted(),
                now
            ) { active, completed, tick ->

                val checkIn = completed?.let {
                    morningCheckInRepository.getForSession(it.id)
                }

                SleepUiState(
                    isLoading = false,
                    active = active,
                    latestCompleted = completed,
                    latestCheckIn = checkIn,
                    isStale = active != null &&
                        SleepTrackingRules.isStale(active.startedAt, tick),
                    sleepScore = _uiState.value.sleepScore,
                    showMorningCheckIn = completed != null && checkIn == null,
                    message = _uiState.value.message
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun startTracking() {
        viewModelScope.launch {
            try {
                trackingRepository.start(Instant.now())
            } catch (t: Throwable) {
                emitMessage(t)
            }
        }
    }

    fun endTracking() {
        viewModelScope.launch {
            try {
                trackingRepository.end(Instant.now())
            } catch (t: Throwable) {
                emitMessage(t)
            }
        }
    }

    fun discardActive() {
        viewModelScope.launch {
            try {
                trackingRepository.discardActive()
            } catch (t: Throwable) {
                emitMessage(t)
            }
        }
    }

    fun submitMorningCheckIn(checkIn: MorningCheckIn) {
        viewModelScope.launch {
            try {
                val trackingSession: SleepTrackingSession =
                    trackingRepository.observeLatestCompleted().first()
                        ?: throw IllegalStateException(
                            "No completed sleep session found."
                        )

                morningCheckInRepository.save(
                    trackingSession.id,
                    checkIn
                )

                val start = trackingSession.startedAt

                val end = trackingSession.endedAt
                    ?: throw IllegalStateException(
                        "Sleep session has no wake time."
                    )

                val sleepSession = createSleepSession(
                    trackingSessionId = trackingSession.id,
                    trackingStart = start,
                    trackingEnd = end,
                    checkIn = checkIn
                )

                sleepRepository.logSession(sleepSession)

                val score = calculateSleepScore(
                    listOf(sleepSession)
                )

                _uiState.value = _uiState.value.copy(
                    latestCheckIn = checkIn,
                    sleepScore = score,
                    showMorningCheckIn = false,
                    message = null
                )
            } catch (t: Throwable) {
                emitMessage(t)
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(
            message = null
        )
    }

    private fun emitMessage(t: Throwable) {
        val text = when (t) {
            is SleepTrackingError -> t.message
            else -> "Something went wrong. Please try again."
        }

        _uiState.value = _uiState.value.copy(
            message = text
        )
    }
}
