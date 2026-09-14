package com.nocta.app.ui.screens.sleep

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nocta.app.domain.model.MorningCheckIn
import com.nocta.app.domain.model.SleepDisturbance
import com.nocta.app.domain.model.SleepScoreBreakdown
import com.nocta.app.domain.model.SleepTrackingSession
import com.nocta.app.ui.components.MetricCard
import com.nocta.app.ui.theme.NoctaBackground
import com.nocta.app.ui.theme.NoctaShapes
import com.nocta.app.ui.theme.NoctaSpacing
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
fun SleepScreen(
    viewModel: SleepViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NoctaBackground),
        contentPadding = PaddingValues(NoctaSpacing.md),
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
                LatestCompletedSection(session)
            }
        }

        if (state.showMorningCheckIn) {
            item {
                MorningCheckInCard(
                    onSubmit = viewModel::submitMorningCheckIn
                )
            }
        }

        state.sleepScore?.let { score ->
            item {
                SleepScoreSection(score)
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
    session: SleepTrackingSession
) {
    val ended = session.endedAt ?: return
    val zone = ZoneId.systemDefault()

    val startLocal =
        session.startedAt.atZone(zone).toLocalTime()

    val endLocal =
        ended.atZone(zone).toLocalTime()

    val minutes =
        Duration.between(session.startedAt, ended).toMinutes()

    Column {
        Text(
            "Last tracked night",
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = NoctaSpacing.sm)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)
        ) {
            MetricCard(
                "Bedtime",
                startLocal.format(TIME_FMT),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                "Wake time",
                endLocal.format(TIME_FMT),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                "Duration",
                "${minutes / 60}h ${minutes % 60}m",
                modifier = Modifier.weight(1f)
            )
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
    score: SleepScoreBreakdown
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

        Spacer(Modifier.height(NoctaSpacing.sm))

        Text(
            "This is a wellness score based on your tracked sleep and check-in responses. It isn't a medical assessment.",
            style = NoctaTypography.bodySmall,
            color = NoctaTextTertiary
        )
    }
}
