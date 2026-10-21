package com.nocta.app.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocta.app.domain.model.DataSource
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.repository.SleepRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

data class LogSleepUiState(
    /** The morning the sleeper woke on. */
    val wakeDate: LocalDate = LocalDate.now(),
    val bedtime: LocalTime = LocalTime.of(23, 0),
    val wakeTime: LocalTime = LocalTime.of(7, 0),
    val awakeMinutes: Int = 0,
    val quality: Int? = null,     // null = not answered; 1–5 when given
    val energy: Int? = null,
    val awakenings: Int = 0,
    val error: String? = null,
    val saved: Boolean = false,
    val saving: Boolean = false
)

/**
 * Manual sleep entry — the "manually entered sleep and wake times" source
 * from the tracking architecture. Validation rejects impossible durations and
 * the record is stored with source SELF_REPORTED so reports and the calendar
 * label it honestly.
 */
@HiltViewModel
class LogSleepViewModel @Inject constructor(
    private val sleepRepository: SleepRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogSleepUiState())
    val uiState: StateFlow<LogSleepUiState> = _uiState.asStateFlow()

    fun setWakeDate(date: LocalDate) = _uiState.update {
        it.copy(wakeDate = date, error = null)
    }

    fun setBedtime(time: LocalTime) = _uiState.update {
        it.copy(bedtime = time, error = null)
    }

    fun setWakeTime(time: LocalTime) = _uiState.update {
        it.copy(wakeTime = time, error = null)
    }

    fun setAwakeMinutes(minutes: Int) = _uiState.update {
        it.copy(awakeMinutes = minutes.coerceIn(0, 240), error = null)
    }

    fun setQuality(quality: Int?) = _uiState.update { it.copy(quality = quality) }

    fun setEnergy(energy: Int?) = _uiState.update { it.copy(energy = energy) }

    fun setAwakenings(count: Int) = _uiState.update {
        it.copy(awakenings = count.coerceIn(0, 20))
    }

    fun save() {
        val state = _uiState.value
        if (state.saving) return

        // The wake happens on wakeDate. Bedtime is normally the evening before
        // it (23:00 → 07:00); a bedtime earlier on the clock than the wake time
        // (01:00 → 07:00) happened after midnight on wakeDate itself.
        val wakeDateTime = LocalDateTime.of(state.wakeDate, state.wakeTime)
        val bedtimeDateTime = if (state.wakeTime.isAfter(state.bedtime)) {
            LocalDateTime.of(state.wakeDate, state.bedtime)
        } else {
            LocalDateTime.of(state.wakeDate.minusDays(1), state.bedtime)
        }

        val inBedMinutes = java.time.Duration.between(bedtimeDateTime, wakeDateTime).toMinutes()
        val asleepMinutes = inBedMinutes - state.awakeMinutes

        if (inBedMinutes <= 0) {
            _uiState.update { it.copy(error = "Wake time must be after bedtime.") }
            return
        }
        if (inBedMinutes > MAX_IN_BED_MINUTES) {
            _uiState.update {
                it.copy(error = "That's more than ${MAX_IN_BED_MINUTES / 60} hours in bed — check the times.")
            }
            return
        }
        if (asleepMinutes < MIN_ASLEEP_MINUTES) {
            _uiState.update {
                it.copy(error = "Estimated sleep must be at least ${MIN_ASLEEP_MINUTES} minutes.")
            }
            return
        }

        val session = SleepSession(
            id = UUID.randomUUID().toString(),
            date = bedtimeDateTime.toLocalDate(),
            bedtime = bedtimeDateTime,
            sleepAttemptTime = bedtimeDateTime,
            estimatedSleepTime = bedtimeDateTime,
            wakeTime = wakeDateTime,
            nightAwakenings = state.awakenings,
            sleepQuality = state.quality ?: 0,
            morningEnergy = state.energy ?: 0,
            source = DataSource.SELF_REPORTED,
            awakeMinutes = state.awakeMinutes
        )

        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            runCatching { sleepRepository.logSession(session) }
                .onSuccess {
                    _uiState.update { it.copy(saving = false, saved = true, error = null) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            saving = false,
                            error = "Couldn't save: ${error.message ?: "storage error"}"
                        )
                    }
                }
        }
    }

    private companion object {
        const val MAX_IN_BED_MINUTES = 20 * 60
        const val MIN_ASLEEP_MINUTES = 30
    }
}
