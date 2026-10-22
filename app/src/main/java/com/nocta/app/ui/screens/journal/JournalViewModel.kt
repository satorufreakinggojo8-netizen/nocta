package com.nocta.app.ui.screens.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocta.app.domain.model.JournalEntry
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.repository.SleepJournalRepository
import com.nocta.app.domain.repository.SleepRepository
import com.nocta.app.domain.usecase.JournalInsights
import com.nocta.app.domain.usecase.JournalPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class JournalUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectableDates: List<LocalDate> = emptyList(),
    val draft: JournalEntry = JournalEntry(LocalDate.now()),
    val storedEntryExists: Boolean = false,
    val message: String? = null,
    val patterns: List<JournalPattern> = emptyList(),
    val journalDays: Int = 0,
    val nightsAvailable: Int = 0
) {
    /** Patterns need several nights on both sides; below this the UI says so. */
    val patternsUnavailableBecauseOfData: Boolean
        get() = patterns.isEmpty()
}

/**
 * The optional daily journal and its pattern hints. Everything writes through
 * [SleepJournalRepository] (Room), so a process restart keeps every answer, and
 * a blank entry is removed rather than stored as an empty day.
 */
@HiltViewModel
class JournalViewModel @Inject constructor(
    private val journalRepository: SleepJournalRepository,
    sleepRepository: SleepRepository,
    private val journalInsights: JournalInsights
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        JournalUiState(selectableDates = (0L until DAYS_SELECTABLE).map { LocalDate.now().minusDays(it) })
    )
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    private var entryJob: Job? = null
    private var sessions: List<SleepSession> = emptyList()
    private var entries: List<JournalEntry> = emptyList()

    init {
        selectDate(LocalDate.now())
        viewModelScope.launch {
            sleepRepository.observeRecentSessions(days = WINDOW_DAYS).collect { recent ->
                sessions = recent
                recomputePatterns()
            }
        }
        viewModelScope.launch {
            journalRepository.observeRecent(days = WINDOW_DAYS).collect { recent ->
                entries = recent
                recomputePatterns()
            }
        }
    }

    fun selectDate(date: LocalDate) {
        entryJob?.cancel()
        _uiState.update {
            it.copy(selectedDate = date, draft = JournalEntry(date), storedEntryExists = false)
        }
        entryJob = viewModelScope.launch {
            journalRepository.observeEntry(date).collect { stored ->
                _uiState.update { state ->
                    // Don't clobber the user's unsaved edits for this same date.
                    if (state.selectedDate != date) return@update state
                    val keepDraft = state.storedEntryExists && stored != null
                    state.copy(
                        isLoading = false,
                        draft = if (keepDraft) state.draft else stored ?: JournalEntry(date),
                        storedEntryExists = stored != null
                    )
                }
            }
        }
    }

    // Tapping the selected value again clears it: every answer stays optional.
    fun setRestfulness(value: Int) = updateDraft { current ->
        current.copy(restfulness = if (current.restfulness == value) null else value)
    }

    fun setStress(value: Int) = updateDraft { current ->
        current.copy(stressLevel = if (current.stressLevel == value) null else value)
    }

    fun adjustNapMinutes(delta: Int) = updateDraft { current ->
        val next = (current.napMinutes ?: 0) + delta
        current.copy(napMinutes = next.coerceIn(0, JournalEntry.MAX_NAP_MINUTES).takeIf { it > 0 })
    }

    fun adjustCaffeineDrinks(delta: Int) = updateDraft { current ->
        val next = (current.caffeineDrinks ?: 0) + delta
        current.copy(
            caffeineDrinks = next.coerceIn(0, JournalEntry.MAX_CAFFEINE_DRINKS).takeIf { it > 0 },
            // Clearing the count also clears its time — a time with no drinks is meaningless.
            lastCaffeineTime = if (next <= 0) null else current.lastCaffeineTime
        )
    }

    fun shiftCaffeineTime(minutes: Int) = updateDraft { current ->
        val base = current.lastCaffeineTime ?: DEFAULT_CAFFEINE_TIME
        current.copy(lastCaffeineTime = shift(base, minutes))
    }

    fun clearCaffeineTime() = updateDraft { it.copy(lastCaffeineTime = null) }

    fun adjustExerciseMinutes(delta: Int) = updateDraft { current ->
        val next = (current.exercisedMinutes ?: 0) + delta
        current.copy(
            exercisedMinutes = next.coerceIn(0, JournalEntry.MAX_EXERCISE_MINUTES).takeIf { it > 0 },
            lastExerciseTime = if (next <= 0) null else current.lastExerciseTime
        )
    }

    fun shiftExerciseTime(minutes: Int) = updateDraft { current ->
        val base = current.lastExerciseTime ?: DEFAULT_EXERCISE_TIME
        current.copy(lastExerciseTime = shift(base, minutes))
    }

    fun clearExerciseTime() = updateDraft { it.copy(lastExerciseTime = null) }

    fun toggleScreens() = updateDraft { current ->
        current.copy(screenUseBeforeBed = when (current.screenUseBeforeBed) {
            null -> true
            true -> false
            false -> null
        })
    }

    fun toggleRoutine() = updateDraft { current ->
        current.copy(routineCompleted = when (current.routineCompleted) {
            null -> true
            true -> false
            false -> null
        })
    }

    fun setNotes(text: String) = updateDraft { current ->
        current.copy(notes = text.take(JournalEntry.MAX_NOTES_LENGTH))
    }

    fun save() {
        val draft = _uiState.value.draft.normalized()
        viewModelScope.launch {
            runCatching { journalRepository.save(draft) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            storedEntryExists = draft.hasAnyAnswer,
                            message = if (draft.hasAnyAnswer) {
                                "Journal saved for ${draft.date}."
                            } else {
                                "Nothing answered — the entry was removed."
                            }
                        )
                    }
                }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(message = "Couldn't save the journal entry. Try again.")
                    }
                }
        }
    }

    fun deleteEntry() {
        val date = _uiState.value.selectedDate
        viewModelScope.launch {
            journalRepository.delete(date)
            _uiState.update {
                it.copy(
                    draft = JournalEntry(date),
                    storedEntryExists = false,
                    message = "Journal entry deleted."
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun updateDraft(transform: (JournalEntry) -> JournalEntry) {
        _uiState.update { state ->
            state.copy(draft = transform(state.draft).copy(date = state.selectedDate))
        }
    }

    private fun recomputePatterns() {
        val patterns = journalInsights(entries, sessions)
        _uiState.update {
            it.copy(
                patterns = patterns,
                journalDays = entries.count { entry -> entry.hasAnyAnswer },
                nightsAvailable = sessions.count { session -> session.hasUsableWindow }
            )
        }
    }

    private fun shift(time: LocalTime, minutes: Int): LocalTime =
        LocalTime.ofSecondOfDay(
            (((time.toSecondOfDay() + minutes * 60L) % 86_400 + 86_400) % 86_400)
        )

    private companion object {
        const val WINDOW_DAYS = 90
        const val DAYS_SELECTABLE = 7L
        val DEFAULT_CAFFEINE_TIME: LocalTime = LocalTime.of(15, 0)
        val DEFAULT_EXERCISE_TIME: LocalTime = LocalTime.of(18, 0)
    }
}
