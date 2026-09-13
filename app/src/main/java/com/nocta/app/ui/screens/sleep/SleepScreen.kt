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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

@Composable
fun SleepScreen(viewModel: SleepViewModel = hiltViewModel()) {
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
                StartTrackingCard(onStart = viewModel::startTracking)
            }
        }

        state.latestCompleted?.let { session ->
            item {
                LatestCompletedSection(session)
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
private fun StartTrackingCard(onStart: () -> Unit) {
    Column {
        Text(
            "Track your sleep",
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(NoctaSpacing.xs))
        Text(
            "Start tracking when you get into bed. We'll record the window — " +
                "no wearable required.",
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
    // Recomputed on every recomposition, which happens once per ticker emission
    // because the ViewModel StateFlow changes each second while active.
    val elapsed: Duration = Duration.between(session.startedAt, Instant.now())
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
        Row(horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)) {
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
                    shape = NoctaShapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NoctaTextPrimary
                    )
                ) {
                    Text("Discard")
                }
            }
        }
    }
}

@Composable
private fun LatestCompletedSection(session: SleepTrackingSession) {
    val ended = session.endedAt ?: return
    val zone = ZoneId.systemDefault()
    val startLocal = session.startedAt.atZone(zone).toLocalTime()
    val endLocal = ended.atZone(zone).toLocalTime()
    val minutes = Duration.between(session.startedAt, ended).toMinutes()

    Column {
        Text(
            "Last tracked night",
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = NoctaSpacing.sm)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)) {
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
