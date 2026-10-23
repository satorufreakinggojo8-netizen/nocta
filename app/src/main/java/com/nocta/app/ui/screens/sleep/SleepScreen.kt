package com.nocta.app.ui.screens.sleep

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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nocta.app.domain.model.MorningCheckIn
import com.nocta.app.domain.model.SleepDisturbance
import com.nocta.app.domain.model.SleepScoreBreakdown
import com.nocta.app.domain.model.SleepTrackingSession
import com.nocta.app.ui.components.MetricCard
import com.nocta.app.ui.components.SleepCalendarCard
import com.nocta.app.ui.components.SleepScoreEmptyCard
import com.nocta.app.ui.theme.NoctaAccent
import com.nocta.app.ui.theme.NoctaAlert
import com.nocta.app.ui.theme.NoctaBackground
import com.nocta.app.ui.theme.NoctaShapes
import com.nocta.app.ui.theme.NoctaSpacing
import com.nocta.app.ui.theme.NoctaSurface
import com.nocta.app.ui.theme.NoctaSurfaceBorder
import com.nocta.app.ui.theme.NoctaTextPrimary
import com.nocta.app.ui.theme.NoctaTextSecondary
import com.nocta.app.ui.theme.NoctaTextTertiary
import com.nocta.app.ui.theme.NoctaTypography
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIME_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a")

@Composable
private fun NightRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = NoctaTypography.bodyMedium,
            color = NoctaTextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatTrackedDuration(minutes: Long): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (remainder == 0L) "${hours}h" else "${hours}h ${remainder}m"
}

@Composable
fun SleepScreen(
    onOpenSleepDetails: (String) -> Unit = {},
    onLogSleep: () -> Unit = {},
    onOpenJournal: () -> Unit = {},
    onOpenScoreHelp: () -> Unit = {},
    viewModel: SleepViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NoctaBackground),
        contentPadding = PaddingValues(
            start = NoctaSpacing.md,
            end = NoctaSpacing.md,
            top = NoctaSpacing.md,
            // Leave the companion stage clear at the bottom.
            bottom = com.nocta.app.ui.guide.ShadeLayout.CompactBottomInset
        ),
        verticalArrangement = Arrangement.spacedBy(NoctaSpacing.lg)
    ) {
        item {
            Text(
                "Sleep",
                style = NoctaTypography.displayLarge,
                color = NoctaTextPrimary,
                modifier = Modifier.padding(top = NoctaSpacing.md)
            )
        }

        item {
            val active = state.active

            if (active != null) {
                ActiveTrackingCard(
                    session = active,
                    isStale = state.isStale,
                    onEnd = viewModel::endTracking,
                    onDiscard = viewModel::discardActive
                )
            } else {
                StartTrackingCard(
                    onStart = viewModel::startTracking
                )
            }
        }

        state.latestCompleted?.let { session ->
            item {
                LatestCompletedSection(
                    session = session,
                    onDiscardShortSession = viewModel::discardLatestSession
                )
            }
        }

        if (state.showMorningCheckIn) {
            item {
                MorningCheckInCard(
                    onSubmit = viewModel::submitMorningCheckIn
                )
            }
        }

        item {
            val score = state.sleepScore
            if (score != null && score.hasData) {
                SleepScoreSection(score, onOpenScoreHelp = onOpenScoreHelp)
            } else if (!state.isLoading) {
                SleepScoreEmptyCard(onLogSleep = onLogSleep)
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(NoctaShapes.medium)
                    .background(NoctaSurface)
                    .border(1.dp, NoctaSurfaceBorder, NoctaShapes.medium)
                    .clickable { onOpenJournal() }
                    .padding(NoctaSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Sleep journal",
                        style = NoctaTypography.titleMedium,
                        color = NoctaTextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Caffeine, stress, naps, screens — optional notes that make patterns possible.",
                        style = NoctaTypography.labelSmall,
                        color = NoctaTextTertiary
                    )
                }
                Text(
                    "Open",
                    style = NoctaTypography.labelSmall,
                    color = NoctaAccent,
                    maxLines = 1
                )
            }
        }

        item {
            Column {
                Text(
                    "Sleep history",
                    style = NoctaTypography.titleMedium,
                    color = NoctaTextPrimary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = NoctaSpacing.sm)
                )

                SleepCalendarCard(
                    sessions = state.historySessions,
                    isLoading = state.isLoading,
                    onOpenSessionDetail = { session -> onOpenSleepDetails(session.id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        state.message?.let { msg ->
            item {
                Text(
                    msg,
                    style = NoctaTypography.bodyMedium,
                    color = NoctaTextSecondary
                )
            }
        }
    }
}

@Composable
private fun StartTrackingCard(
    onStart: () -> Unit
) {
    Column {
        Text(
            "Track your sleep",
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(NoctaSpacing.xs))

        Text(
            "Start tracking when you get into bed. We'll record the window — no wearable required.",
            style = NoctaTypography.bodyMedium,
            color = NoctaTextSecondary
        )

        Spacer(Modifier.height(NoctaSpacing.md))

        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            shape = NoctaShapes.medium
        ) {
            Text("Start Sleep Tracking")
        }
    }
}

@Composable
private fun ActiveTrackingCard(
    session: SleepTrackingSession,
    isStale: Boolean,
    onEnd: () -> Unit,
    onDiscard: () -> Unit
) {
    val elapsed: Duration =
        Duration.between(session.startedAt, Instant.now())

    val hours = elapsed.toHours()
    val minutes = elapsed.toMinutes() % 60
    val seconds = elapsed.seconds % 60

    val startedLocal = session.startedAt
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(TIME_FMT)

    Column {
        Text(
            "Tracking in progress",
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(NoctaSpacing.xs))

        Text(
            "Started at $startedLocal",
            style = NoctaTypography.bodyMedium,
            color = NoctaTextSecondary
        )

        Spacer(Modifier.height(NoctaSpacing.md))

        Text(
            "%02d:%02d:%02d".format(hours, minutes, seconds),
            style = NoctaTypography.displayLarge,
            color = NoctaTextPrimary
        )

        if (isStale) {
            Spacer(Modifier.height(NoctaSpacing.sm))

            Text(
                "This session has been running a long time. Did you forget to end it?",
                style = NoctaTypography.bodyMedium,
                color = NoctaTextSecondary
            )
        }

        Spacer(Modifier.height(NoctaSpacing.md))

        Row(
            horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)
        ) {
            Button(
                onClick = onEnd,
                modifier = Modifier.weight(1f),
                shape = NoctaShapes.medium
            ) {
                Text("End Sleep")
            }

            if (isStale) {
                OutlinedButton(
                    onClick = onDiscard,
                    modifier = Modifier.weight(1f),
                    shape = NoctaShapes.medium
                ) {
                    Text("Discard")
                }
            }
        }
    }
}

@Composable
private fun LatestCompletedSection(
    session: SleepTrackingSession,
    onDiscardShortSession: () -> Unit
) {
    val ended = session.endedAt ?: return
    val zone = ZoneId.systemDefault()

    val startLocal =
        session.startedAt.atZone(zone).toLocalTime()

    val endLocal =
        ended.atZone(zone).toLocalTime()

    val minutes =
        Duration.between(session.startedAt, ended).toMinutes()

    // A start/end in the same minute is not a night of sleep; saying "0h 0m"
    // would look like a bug in the app rather than an empty session.
    if (minutes < 1) {
        Column {
            Text(
                "Last tracked night",
                style = NoctaTypography.titleMedium,
                color = NoctaTextPrimary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = NoctaSpacing.sm)
            )
            Text(
                "That session was too short to record. Start tracking when you get into bed.",
                style = NoctaTypography.bodyMedium,
                color = NoctaTextSecondary
            )
            Spacer(Modifier.height(NoctaSpacing.xs))
            TextButton(onClick = onDiscardShortSession) {
                Text("Dismiss", color = NoctaAlert)
            }
        }
        return
    }

    Column {
        Text(
            "Last tracked night",
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = NoctaSpacing.sm)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(NoctaShapes.medium)
                .background(NoctaSurface)
                .border(1.dp, NoctaSurfaceBorder, NoctaShapes.medium)
                .padding(horizontal = NoctaSpacing.md, vertical = NoctaSpacing.xs)
        ) {
            NightRow("Bedtime", startLocal.format(TIME_FMT))
            NightRow("Wake time", endLocal.format(TIME_FMT))
            NightRow("Duration", formatTrackedDuration(minutes))
        }

        Spacer(Modifier.height(NoctaSpacing.sm))

        Text(
            session.confidenceLabel,
            style = NoctaTypography.labelSmall,
            color = NoctaTextTertiary
        )
    }
}

@Composable
private fun MorningCheckInCard(
    onSubmit: (MorningCheckIn) -> Unit
) {
    var sleepQuality by remember { mutableIntStateOf(3) }
    var awakenings by remember { mutableIntStateOf(0) }
    var awakeMinutes by remember { mutableIntStateOf(0) }
    var morningEnergy by remember { mutableIntStateOf(3) }
    var disturbance by remember {
        mutableStateOf(SleepDisturbance.NONE)
    }

    Column {
        Text(
            "Good morning",
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(NoctaSpacing.xs))

        Text(
            "A quick check-in helps Nocta understand how your night actually felt.",
            style = NoctaTypography.bodyMedium,
            color = NoctaTextSecondary
        )

        Spacer(Modifier.height(NoctaSpacing.md))

        CheckInQuestion(
            title = "How was your sleep overall?",
            options = listOf(
                "Very poor",
                "Poor",
                "Okay",
                "Good",
                "Excellent"
            ),
            selected = sleepQuality - 1,
            onSelected = { sleepQuality = it + 1 }
        )

        Spacer(Modifier.height(NoctaSpacing.md))

        CheckInQuestion(
            title = "How often did you wake during the night?",
            options = listOf(
                "No times",
                "Once",
                "2–3 times",
                "4+ times"
            ),
            selected = when (awakenings) {
                0 -> 0
                1 -> 1
                2 -> 2
                else -> 3
            },
            onSelected = {
                awakenings = when (it) {
                    0 -> 0
                    1 -> 1
                    2 -> 2
                    else -> 4
                }
            }
        )

        Spacer(Modifier.height(NoctaSpacing.md))

        CheckInQuestion(
            title = "Roughly how long were you awake?",
            options = listOf(
                "Not at all",
                "A few minutes",
                "10–30 min",
                "30–60 min",
                "More than an hour"
            ),
            selected = when (awakeMinutes) {
                0 -> 0
                5 -> 1
                20 -> 2
                45 -> 3
                else -> 4
            },
            onSelected = {
                awakeMinutes = when (it) {
                    0 -> 0
                    1 -> 5
                    2 -> 20
                    3 -> 45
                    else -> 60
                }
            }
        )

        Spacer(Modifier.height(NoctaSpacing.md))

        CheckInQuestion(
            title = "How rested do you feel now?",
            options = listOf(
                "Exhausted",
                "Tired",
                "Okay",
                "Rested",
                "Very refreshed"
            ),
            selected = morningEnergy - 1,
            onSelected = { morningEnergy = it + 1 }
        )

        Spacer(Modifier.height(NoctaSpacing.md))

        CheckInQuestion(
            title = "Did anything disturb your sleep?",
            options = listOf(
                "Nothing",
                "Noise",
                "Too hot or cold",
                "Light",
                "Stress or thoughts",
                "Phone or notifications",
                "Physical discomfort",
                "Other"
            ),
            selected = disturbance.ordinal,
            onSelected = {
                disturbance = SleepDisturbance.entries[it]
            }
        )

        Spacer(Modifier.height(NoctaSpacing.lg))

        Button(
            onClick = {
                onSubmit(
                    MorningCheckIn(
                        sleepQuality = sleepQuality,
                        nightAwakenings = awakenings,
                        awakeMinutes = awakeMinutes,
                        morningEnergy = morningEnergy,
                        disturbance = disturbance
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = NoctaShapes.medium
        ) {
            Text("Calculate My Sleep Score")
        }
    }
}

@Composable
private fun CheckInQuestion(
    title: String,
    options: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit
) {
    Column {
        Text(
            title,
            style = NoctaTypography.bodyLarge,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(NoctaSpacing.sm))

        Column(
            verticalArrangement = Arrangement.spacedBy(NoctaSpacing.xs)
        ) {
            options.forEachIndexed { index, option ->
                if (index == selected) {
                    Button(
                        onClick = { onSelected(index) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = NoctaShapes.medium
                    ) {
                        Text(option)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onSelected(index) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = NoctaShapes.medium
                    ) {
                        Text(option)
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepScoreSection(
    score: SleepScoreBreakdown,
    onOpenScoreHelp: () -> Unit
) {
    Column {
        Text(
            "Your sleep score",
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(NoctaSpacing.sm))

        Text(
            "${score.overall}",
            style = NoctaTypography.displayLarge,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Bold
        )

        Text(
            score.label,
            style = NoctaTypography.titleMedium,
            color = NoctaTextSecondary
        )

        Spacer(Modifier.height(NoctaSpacing.md))

        Row(
            horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)
        ) {
            MetricCard(
                "Duration",
                "${score.durationScore}",
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                "Consistency",
                "${score.consistencyScore}",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(NoctaSpacing.sm))

        Row(
            horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)
        ) {
            MetricCard(
                "Timing",
                "${score.timingScore}",
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                "Quality",
                "${score.qualityScore}",
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                "Routine",
                "${score.routineScore}",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(NoctaSpacing.xs))

        // The long explanation (weights, caveats) lives on its own page.
        TextButton(onClick = onOpenScoreHelp) {
            Text(
                "How this is calculated",
                style = NoctaTypography.labelSmall,
                color = NoctaAccent,
                maxLines = 1
            )
        }
    }
}
