package com.nocta.app.ui.screens.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.profile.SleepProfile
import com.nocta.app.domain.repository.MorningCheckInRepository
import com.nocta.app.domain.repository.SleepProfileRepository
import com.nocta.app.domain.repository.SleepRepository
import com.nocta.app.domain.usecase.BuildNightlyReport
import com.nocta.app.domain.usecase.NightlyReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SleepDetailUiState(
    val isLoading: Boolean = true,
    val report: NightlyReport? = null,
    /** True when the requested id matches no stored night. */
    val notFound: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SleepDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sleepRepository: SleepRepository,
    private val morningCheckInRepository: MorningCheckInRepository,
    private val sleepProfileRepository: SleepProfileRepository,
    private val buildNightlyReport: BuildNightlyReport
) : ViewModel() {

    private val sessionId: String =
        savedStateHandle.get<String>(SESSION_ID_ARG) ?: ""

    private val _uiState = MutableStateFlow(SleepDetailUiState())
    val uiState: StateFlow<SleepDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                sleepRepository.observeSession(sessionId),
                sleepRepository.observeRecentSessions(days = CONTEXT_NIGHTS),
                sleepProfileRepository.observeProfile()
            ) { session, recent, profile ->
                Triple(session, recent, profile)
            }.catch { throwable ->
                _uiState.value = SleepDetailUiState(
                    isLoading = false,
                    errorMessage = "Couldn't load this night: ${throwable.message ?: "database error"}"
                )
            }.collect { (session, recent, profile) ->
                if (session == null) {
                    // Room emits the stored row (or null) immediately, so a null
                    // here means no night with this id exists.
                    _uiState.value = SleepDetailUiState(isLoading = false, notFound = true)
                    return@collect
                }
                val checkIn = morningCheckInRepository.getForSession(sessionId)
                val report = buildNightlyReport(
                    session = session,
                    recentSessions = recent,
                    checkIn = checkIn,
                    profile = profile ?: SleepProfile(age = DEFAULT_PROFILE_AGE)
                )
                _uiState.value = SleepDetailUiState(isLoading = false, report = report)
            }
        }
    }

    private companion object {
        const val SESSION_ID_ARG = "sessionId"
        const val CONTEXT_NIGHTS = 60
        const val DEFAULT_PROFILE_AGE = 18
    }
}
