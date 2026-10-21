package com.nocta.app.ui.screens.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nocta.app.ui.theme.NoctaAccent
import com.nocta.app.ui.theme.NoctaAlert
import com.nocta.app.ui.theme.NoctaBackground
import com.nocta.app.ui.theme.NoctaShapes
import com.nocta.app.ui.theme.NoctaSpacing
import com.nocta.app.ui.theme.NoctaSurface
import com.nocta.app.ui.theme.NoctaSurfaceBorder
import com.nocta.app.ui.theme.NoctaSuccess
import com.nocta.app.ui.theme.NoctaTextPrimary
import com.nocta.app.ui.theme.NoctaTextSecondary
import com.nocta.app.ui.theme.NoctaTextTertiary
import com.nocta.app.ui.theme.NoctaTypography
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")

/**
 * Manual sleep entry. Sources: self-reported times and optional check-in
 * answers; everything is validated before it reaches the Room log.
 */
@Composable
fun LogSleepScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: LogSleepViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NoctaBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NoctaSpacing.sm, vertical = NoctaSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NoctaTextSecondary
                )
            }
            Text(
                "Log sleep",
                style = NoctaTypography.titleMedium,
                color = NoctaTextPrimary,
                fontWeight = FontWeight.Medium
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(NoctaSpacing.md),
            verticalArrangement = Arrangement.spacedBy(NoctaSpacing.md)
        ) {
            item {
                Text(
                    "Enter the times you remember — estimates are fine. " +
                        "The record will be labeled self-reported.",
                    style = NoctaTypography.bodyMedium,
                    color = NoctaTextSecondary
                )
            }

            item {
                Card {
                    Text("Woke on", style = NoctaTypography.labelSmall, color = NoctaTextSecondary)
                    Spacer(Modifier.height(NoctaSpacing.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)) {
                        OutlinedButton(onClick = {
                            viewModel.setWakeDate(state.wakeDate.minusDays(1))
                        }) { Text("◀ Day") }
                        Box2 {
                            Text(
                                state.wakeDate.format(DATE_FMT),
                                style = NoctaTypography.titleMedium,
                                color = NoctaTextPrimary
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.setWakeDate(state.wakeDate.plusDays(1)) },
                            enabled = !state.wakeDate.isAfter(java.time.LocalDate.now())
                        ) { Text("Day ▶") }
                    }
                }
            }

            item {
                Card {
                    Text("Bedtime", style = NoctaTypography.labelSmall, color = NoctaTextSecondary)
                    Spacer(Modifier.height(NoctaSpacing.xs))
                    TimeRow(
                        time = state.bedtime,
                        onShift = { minutes ->
                            viewModel.setBedtime(state.bedtime.plusMinutes(minutes.toLong()))
                        }
                    )
                }
            }

            item {
                Card {
                    Text("Wake time", style = NoctaTypography.labelSmall, color = NoctaTextSecondary)
                    Spacer(Modifier.height(NoctaSpacing.xs))
                    TimeRow(
                        time = state.wakeTime,
                        onShift = { minutes ->
                            viewModel.setWakeTime(state.wakeTime.plusMinutes(minutes.toLong()))
                        }
                    )
                }
            }

            item {
                Card {
                    Text(
                        "Awake during the night",
                        style = NoctaTypography.labelSmall,
                        color = NoctaTextSecondary
                    )
                    Spacer(Modifier.height(NoctaSpacing.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)) {
                        listOf(
                            "0 min" to 0, "15 min" to 15, "30 min" to 30,
                            "60 min" to 60, "90+ min" to 90
                        ).forEach { (label, minutes) ->
                            ChoiceChip(
                                label = label,
                                selected = state.awakeMinutes == minutes,
                                onClick = { viewModel.setAwakeMinutes(minutes) }
                            )
                        }
                    }
                    Spacer(Modifier.height(NoctaSpacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)) {
                        Text(
                            "${state.awakenings} night awakening" +
                                if (state.awakenings == 1) "" else "s",
                            style = NoctaTypography.bodyMedium,
                            color = NoctaTextPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = {
                            viewModel.setAwakenings(state.awakenings - 1)
                        }, enabled = state.awakenings > 0) { Text("−") }
                        OutlinedButton(onClick = {
                            viewModel.setAwakenings(state.awakenings + 1)
                        }, enabled = state.awakenings < 20) { Text("+") }
                    }
                }
            }

            item {
                Card {
                    Text(
                        "How did it feel? (optional)",
                        style = NoctaTypography.labelSmall,
                        color = NoctaTextSecondary
                    )
                    Spacer(Modifier.height(NoctaSpacing.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)) {
                        (1..5).forEach { value ->
                            ChoiceChip(
                                label = "$value",
                                selected = state.quality == value,
                                onClick = {
                                    viewModel.setQuality(if (state.quality == value) null else value)
                                }
                            )
                        }
                        Text(
                            if (state.quality == null) "skipped" else "of 5",
                            style = NoctaTypography.labelSmall,
                            color = NoctaTextTertiary,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }

            state.error?.let { message ->
                item {
                    Text(message, style = NoctaTypography.bodyMedium, color = NoctaAlert)
                }
            }

            item {
                Button(
                    onClick = viewModel::save,
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = NoctaShapes.medium
                ) {
                    Text(if (state.saving) "Saving…" else "Save night")
                }
            }

            item {
                Text(
                    "Saved nights appear in your calendar, score, trends, and reports.",
                    style = NoctaTypography.labelSmall,
                    color = NoctaTextTertiary
                )
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoctaShapes.medium)
            .background(NoctaSurface)
            .border(1.dp, NoctaSurfaceBorder, NoctaShapes.medium)
            .padding(NoctaSpacing.md)
    ) {
        content()
    }
}

@Composable
private fun Box2(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .clip(NoctaShapes.small)
            .background(NoctaBackground)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) { content() }
}

@Composable
private fun TimeRow(
    time: java.time.LocalTime,
    onShift: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = { onShift(-30) }) { Text("−30m") }
        Box2 {
            val hour24 = time.hour
            val hour12 = when (val h = hour24 % 12) { 0 -> 12; else -> h }
            val amPm = if (hour24 < 12) "AM" else "PM"
            Text(
                String.format(Locale.US, "%d:%02d %s", hour12, time.minute, amPm),
                style = NoctaTypography.titleMedium,
                color = NoctaAccent
            )
        }
        OutlinedButton(onClick = { onShift(30) }) { Text("+30m") }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Text(
            label,
            color = if (selected) NoctaAccent else NoctaTextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
