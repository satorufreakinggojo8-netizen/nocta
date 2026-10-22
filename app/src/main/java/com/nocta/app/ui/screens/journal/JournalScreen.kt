package com.nocta.app.ui.screens.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nocta.app.domain.model.JournalEntry
import com.nocta.app.domain.usecase.JournalPattern
import com.nocta.app.ui.theme.NoctaAccent
import com.nocta.app.ui.theme.NoctaAccentMuted
import com.nocta.app.ui.theme.NoctaBackground
import com.nocta.app.ui.theme.NoctaShapes
import com.nocta.app.ui.theme.NoctaSpacing
import com.nocta.app.ui.theme.NoctaSurface
import com.nocta.app.ui.theme.NoctaSurfaceBorder
import com.nocta.app.ui.theme.NoctaTextPrimary
import com.nocta.app.ui.theme.NoctaTextSecondary
import com.nocta.app.ui.theme.NoctaTextTertiary
import com.nocta.app.ui.theme.NoctaTypography
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_CHIP_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d", Locale.getDefault())
private val FULL_DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())
private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

/**
 * Sleep journal — every field optional, saved to Room per sleep date. Pattern
 * hints only appear once there are enough real nights on both sides of a
 * factor, and are always labelled as possible associations.
 */
@Composable
fun JournalScreen(
    onBack: () -> Unit = {},
    viewModel: JournalViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val draft = state.draft

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NoctaBackground),
        contentPadding = PaddingValues(NoctaSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NoctaSpacing.md)
    ) {
        item {
            Column(Modifier.padding(top = NoctaSpacing.sm)) {
                Text(
                    "Journal",
                    style = NoctaTypography.displayLarge,
                    color = NoctaTextPrimary
                )
                Text(
                    "Optional notes on the day that shaped your night. " +
                        "Answer only what you want — everything here is your own record.",
                    style = NoctaTypography.bodyMedium,
                    color = NoctaTextSecondary
                )
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.xs)) {
                items(state.selectableDates) { date ->
                    DateChip(
                        date = date,
                        selected = date == state.selectedDate,
                        hasEntry = date == state.selectedDate && state.storedEntryExists,
                        onClick = { viewModel.selectDate(date) }
                    )
                }
            }
        }

        item {
            Card(title = state.selectedDate.format(FULL_DATE_FMT)) {
                Text(
                    "How the night felt",
                    style = NoctaTypography.labelSmall,
                    color = NoctaTextSecondary
                )
                Spacer(Modifier.height(NoctaSpacing.xs))
                RatingRow(
                    title = "Restfulness",
                    hint = "1 wrecked · 5 rested",
                    value = draft.restfulness,
                    onSelect = viewModel::setRestfulness
                )
                Spacer(Modifier.height(NoctaSpacing.sm))
                RatingRow(
                    title = "Stress",
                    hint = "1 calm · 5 very stressed",
                    value = draft.stressLevel,
                    onSelect = viewModel::setStress
                )
            }
        }

        item {
            Card(title = "Daytime") {
                StepperRow(
                    label = "Nap",
                    value = draft.napMinutes?.let { formatMinutes(it.toLong()) } ?: "None",
                    onMinus = { viewModel.adjustNapMinutes(-15) },
                    onPlus = { viewModel.adjustNapMinutes(15) }
                )
                Spacer(Modifier.height(NoctaSpacing.sm))
                StepperRow(
                    label = "Caffeine",
                    value = draft.caffeineDrinks?.let { "$it drink${if (it == 1) "" else "s"}" }
                        ?: "None",
                    onMinus = { viewModel.adjustCaffeineDrinks(-1) },
                    onPlus = { viewModel.adjustCaffeineDrinks(1) }
                )
                if ((draft.caffeineDrinks ?: 0) > 0) {
                    Spacer(Modifier.height(NoctaSpacing.xs))
                    TimeAdjustRow(
                        prefix = "Last caffeine",
                        time = draft.lastCaffeineTime,
                        onShift = viewModel::shiftCaffeineTime,
                        onClear = viewModel::clearCaffeineTime
                    )
                }
                Spacer(Modifier.height(NoctaSpacing.sm))
                StepperRow(
                    label = "Exercise",
                    value = draft.exercisedMinutes?.let { formatMinutes(it.toLong()) } ?: "None",
                    onMinus = { viewModel.adjustExerciseMinutes(-15) },
                    onPlus = { viewModel.adjustExerciseMinutes(15) }
                )
                if ((draft.exercisedMinutes ?: 0) > 0) {
                    Spacer(Modifier.height(NoctaSpacing.xs))
                    TimeAdjustRow(
                        prefix = "Finished around",
                        time = draft.lastExerciseTime,
                        onShift = viewModel::shiftExerciseTime,
                        onClear = viewModel::clearExerciseTime
                    )
                }
            }
        }

        item {
            Card(title = "Evening") {
                TriStateRow(
                    label = "Screens before bed",
                    value = draft.screenUseBeforeBed,
                    onClick = viewModel::toggleScreens
                )
                Spacer(Modifier.height(NoctaSpacing.xs))
                TriStateRow(
                    label = "Finished my wind-down",
                    value = draft.routineCompleted,
                    onClick = viewModel::toggleRoutine
                )
                Spacer(Modifier.height(NoctaSpacing.xs))
                Text(
                    "Unanswered stays unanswered — tap once for yes, twice for no, three times to clear.",
                    style = NoctaTypography.labelSmall,
                    color = NoctaTextTertiary
                )
            }
        }

        item {
            Card(title = "Notes") {
                OutlinedTextField(
                    value = draft.notes.orEmpty(),
                    onValueChange = viewModel::setNotes,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    placeholder = {
                        Text(
                            "Anything worth remembering about last night? (Private to this device.)",
                            style = NoctaTypography.bodyMedium,
                            color = NoctaTextTertiary
                        )
                    },
                    textStyle = NoctaTypography.bodyMedium,
                    shape = NoctaShapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NoctaAccent,
                        unfocusedBorderColor = NoctaSurfaceBorder,
                        focusedTextColor = NoctaTextPrimary,
                        unfocusedTextColor = NoctaTextPrimary,
                        cursorColor = NoctaAccent
                    )
                )
                Spacer(Modifier.height(NoctaSpacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)) {
                    Button(
                        onClick = viewModel::save,
                        shape = NoctaShapes.small,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text("Save entry", maxLines = 1)
                    }
                    if (state.storedEntryExists) {
                        OutlinedButton(
                            onClick = viewModel::deleteEntry,
                            shape = NoctaShapes.small,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text("Delete", color = NoctaTextSecondary, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        onClick = onBack,
                        shape = NoctaShapes.small,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text("Back", maxLines = 1)
                    }
                }
                state.message?.let { message ->
                    Spacer(Modifier.height(NoctaSpacing.xs))
                    Text(message, style = NoctaTypography.labelSmall, color = NoctaAccent)
                }
            }
        }

        item {
            PatternCard(
                patterns = state.patterns,
                journalDays = state.journalDays,
                nightsAvailable = state.nightsAvailable
            )
        }

        item {
            Text(
                "Patterns compare your own recorded nights and are possible associations, " +
                    "not proven causes. Journal notes never leave this device.",
                style = NoctaTypography.labelSmall,
                color = NoctaTextTertiary
            )
        }
    }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoctaShapes.medium)
            .background(NoctaSurface)
            .border(1.dp, NoctaSurfaceBorder, NoctaShapes.medium)
            .padding(NoctaSpacing.md)
    ) {
        Text(
            title,
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(NoctaSpacing.sm))
        content()
    }
}

@Composable
private fun DateChip(date: LocalDate, selected: Boolean, hasEntry: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(NoctaShapes.small)
            .background(if (selected) NoctaAccentMuted else NoctaSurface)
            .border(
                1.dp,
                if (selected) NoctaAccent.copy(alpha = 0.5f) else NoctaSurfaceBorder,
                NoctaShapes.small
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            date.format(DATE_CHIP_FMT),
            style = NoctaTypography.labelSmall,
            color = if (selected) NoctaAccent else NoctaTextSecondary,
            maxLines = 1
        )
        Text(
            if (hasEntry) "saved" else "—",
            style = NoctaTypography.labelSmall,
            color = if (hasEntry) NoctaAccent else NoctaTextTertiary,
            maxLines = 1
        )
    }
}

@Composable
private fun RatingRow(title: String, hint: String, value: Int?, onSelect: (Int) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = NoctaTypography.bodyMedium, color = NoctaTextPrimary)
            Text(hint, style = NoctaTypography.labelSmall, color = NoctaTextTertiary, maxLines = 1)
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.xs)) {
            (1..5).forEach { rating ->
                val selected = value == rating
                Text(
                    "$rating",
                    style = NoctaTypography.labelSmall,
                    color = if (selected) NoctaAccent else NoctaTextSecondary,
                    modifier = Modifier
                        .weight(1f)
                        .clip(NoctaShapes.small)
                        .background(if (selected) NoctaAccentMuted else NoctaBackground)
                        .border(
                            1.dp,
                            if (selected) NoctaAccent.copy(alpha = 0.5f) else NoctaSurfaceBorder,
                            NoctaShapes.small
                        )
                        .clickable { onSelect(rating) }
                        .padding(vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun StepperRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = NoctaTypography.bodyMedium, color = NoctaTextPrimary)
        Spacer(Modifier.weight(1f))
        StepButton("−", onMinus)
        Text(
            value,
            style = NoctaTypography.bodyMedium,
            color = NoctaTextPrimary,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        StepButton("+", onPlus)
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = NoctaTypography.titleMedium,
        color = NoctaTextPrimary,
        maxLines = 1,
        modifier = Modifier
            .clip(NoctaShapes.small)
            .background(NoctaBackground)
            .border(1.dp, NoctaSurfaceBorder, NoctaShapes.small)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}

@Composable
private fun TimeAdjustRow(
    prefix: String,
    time: java.time.LocalTime?,
    onShift: (Int) -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            prefix,
            style = NoctaTypography.labelSmall,
            color = NoctaTextSecondary
        )
        Spacer(Modifier.weight(1f))
        StepButton("−30", { onShift(-30) })
        Text(
            time?.format(TIME_FMT) ?: "not set",
            style = NoctaTypography.labelSmall,
            color = NoctaTextPrimary,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        StepButton("+30", { onShift(30) })
        Spacer(Modifier.padding(horizontal = 2.dp))
        Text(
            "Clear",
            style = NoctaTypography.labelSmall,
            color = NoctaTextTertiary,
            modifier = Modifier
                .clip(NoctaShapes.small)
                .clickable { onClear() }
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun TriStateRow(label: String, value: Boolean?, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = NoctaTypography.bodyMedium, color = NoctaTextPrimary)
        Spacer(Modifier.weight(1f))
        Text(
            when (value) {
                true -> "Yes"
                false -> "No"
                null -> "—"
            },
            style = NoctaTypography.labelSmall,
            color = if (value == null) NoctaTextTertiary else NoctaAccent,
            maxLines = 1,
            modifier = Modifier.padding(end = 8.dp)
        )
        Switch(
            checked = value == true,
            onCheckedChange = { onClick() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = NoctaBackground,
                checkedTrackColor = NoctaAccent,
                uncheckedThumbColor = NoctaTextSecondary,
                uncheckedTrackColor = NoctaSurfaceBorder
            )
        )
    }
}

@Composable
private fun PatternCard(patterns: List<JournalPattern>, journalDays: Int, nightsAvailable: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoctaShapes.medium)
            .background(NoctaSurface)
            .border(1.dp, NoctaSurfaceBorder, NoctaShapes.medium)
            .padding(NoctaSpacing.md)
    ) {
        Text(
            "Possible patterns",
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(NoctaSpacing.xs))
        if (patterns.isEmpty()) {
            Text(
                if (journalDays < 6 || nightsAvailable < 6) {
                    "Not enough overlapping data yet — you have $journalDays journal " +
                        "day${if (journalDays == 1) "" else "s"} and " +
                        "$nightsAvailable logged night${if (nightsAvailable == 1) "" else "s"}. " +
                        "Patterns appear once at least three nights exist on each side of a factor."
                } else {
                    "No clear association in your current records. " +
                        "That's a normal result — keep logging and the picture may change."
                },
                style = NoctaTypography.bodyMedium,
                color = NoctaTextTertiary
            )
        } else {
            patterns.forEach { pattern ->
                Text(
                    pattern.description,
                    style = NoctaTypography.bodyMedium,
                    color = NoctaTextPrimary
                )
                Text(
                    "Possible association, not cause.",
                    style = NoctaTypography.labelSmall,
                    color = NoctaTextTertiary
                )
                Spacer(Modifier.height(NoctaSpacing.sm))
            }
        }
    }
}

private fun formatMinutes(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0L -> "${m} min"
        m == 0L -> "${h}h"
        else -> "${h}h ${m}m"
    }
}
