package com.nocta.app.ui.screens.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nocta.app.domain.model.SleepDebtStatus
import com.nocta.app.domain.usecase.TrendSummary
import com.nocta.app.ui.components.MetricCard
import com.nocta.app.ui.guide.ShadeLayout
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

private val MONTH_DAY: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

/**
 * Trends computed exclusively from the user's stored sleep records. Charts
 * render real points only — nights without records appear as gaps, never as
 * fabricated values.
 */
@Composable
fun InsightsScreen(
    onOpenSleepDetails: (String) -> Unit = {},
    onOpenSleepDebt: () -> Unit = {},
    onLogSleep: () -> Unit = {},
    viewModel: InsightsViewModel = hiltViewModel()
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
            bottom = ShadeLayout.CompactBottomInset
        ),
        verticalArrangement = Arrangement.spacedBy(NoctaSpacing.lg)
    ) {
        item {
            Column(modifier = Modifier.padding(top = NoctaSpacing.sm)) {
                Text(
                    "Insights",
                    style = NoctaTypography.displayLarge,
                    color = NoctaTextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Your recorded nights — nothing filled in.",
                    style = NoctaTypography.labelSmall,
                    color = NoctaTextTertiary
                )
            }
        }

        item {
            WindowSelector(current = state.window, onSelect = viewModel::selectWindow)
        }

        when {
            state.isLoading -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(NoctaShapes.large)
                        .background(NoctaSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Crunching your nights…", color = NoctaTextTertiary)
                }
            }

            state.errorMessage != null -> item {
                Text(
                    state.errorMessage!!,
                    style = NoctaTypography.bodyMedium,
                    color = NoctaTextSecondary
                )
            }

            state.summary != null -> {
                val summary = state.summary!!
                if (summary.trackedNights == 0) {
                    item { EmptyState(onLogSleep = onLogSleep) }
                } else {
                    item { StatsGrid(summary) }
                    item { DurationChart(summary = summary, onOpenNight = onOpenSleepDetails) }
                    state.sleepDebt?.let { debt ->
                        item {
                            SleepDebtRow(
                                status = debt.status,
                                onOpen = onOpenSleepDebt
                            )
                        }
                    }
                    item { StreaksRow(summary) }
                    item { DistributionCard(summary) }
                }
            }
        }

        item {
            Text(
                text = "All figures come from your saved sleep records. Missing nights are " +
                    "counted, never invented.",
                style = NoctaTypography.labelSmall,
                color = NoctaTextTertiary
            )
        }
    }
}

@Composable
private fun WindowSelector(current: TrendWindow, onSelect: (TrendWindow) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TrendWindow.entries.forEach { window ->
            val selected = window == current
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) NoctaAccentMuted else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (selected) Color.Transparent else NoctaSurfaceBorder,
                        shape = RoundedCornerShape(50)
                    )
                    .clickable(onClickLabel = "Show ${window.label}") { onSelect(window) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = window.label,
                    style = NoctaTypography.labelSmall,
                    color = if (selected) NoctaAccent else NoctaTextSecondary
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onLogSleep: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoctaShapes.large)
            .background(NoctaSurface)
            .border(1.dp, NoctaSurfaceBorder, NoctaShapes.large)
            .padding(NoctaSpacing.lg)
    ) {
        Text(
            "No sleep records in this range",
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(NoctaSpacing.sm))
        Text(
            "Log a night and its duration, score and timing appear here. Try a longer range " +
                "if you tracked sleep before this week.",
            style = NoctaTypography.bodyMedium,
            color = NoctaTextSecondary
        )
        Spacer(Modifier.height(NoctaSpacing.md))
        Button(onClick = onLogSleep, shape = NoctaShapes.medium) {
            Text("Log a night")
        }
    }
}

@Composable
private fun StatsGrid(summary: TrendSummary) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)) {
            MetricCard(
                "Avg sleep",
                summary.averageDurationMinutes?.let { formatDuration(it) } ?: "—",
                caption = "${summary.trackedNights} nights tracked",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                "Avg score",
                summary.averageScore?.toString() ?: "—",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(NoctaSpacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)) {
            MetricCard(
                "Avg bedtime",
                summary.averageBedtimeMinutes?.let { formatClock(it) } ?: "—",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                "Consistency",
                summary.consistencyPercent?.let { "$it/100" } ?: "—",
                caption = if (summary.consistencyPercent == null) "needs 3+ nights" else null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Nightly duration bars across the selected range.
 *
 * - Days without a record are left empty (no bar at all), so a gap always
 *   means "no record" rather than zero sleep.
 * - The dashed line is the midpoint of the user's age-based target range.
 * - Tapping a bar opens that night's report; the same action is exposed to
 *   screen readers as a custom accessibility action per night.
 */
@Composable
private fun DurationChart(
    summary: TrendSummary,
    onOpenNight: (String) -> Unit
) {
    val today = LocalDate.now()
    val windowDays = summary.windowDays
    val start = today.minusDays(windowDays - 1L)
    val byDate = summary.durationSeries.associateBy { it.date }

    val maxMinutes = summary.durationSeries
        .maxOfOrNull { it.minutes }
        ?.coerceAtLeast(summary.targetSleepMinutes.toLong())
        ?.coerceAtLeast(60L)
        ?: 60L
    val scaleMax = (maxMinutes * 1.15f).toFloat()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoctaShapes.medium)
            .background(NoctaSurface)
            .border(1.dp, NoctaSurfaceBorder, NoctaShapes.medium)
            .padding(NoctaSpacing.md)
    ) {
        Text(
            "Time asleep per night",
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "Tap a bar for that night's report.",
            style = NoctaTypography.labelSmall,
            color = NoctaTextTertiary
        )
        Spacer(Modifier.height(NoctaSpacing.md))

        val spokenSummary = summary.durationSeries.joinToString("; ") {
            "${it.date.format(MONTH_DAY)}: ${formatDuration(it.minutes)}"
        }
        val nightActions = summary.durationSeries.map { point ->
            CustomAccessibilityAction("Open report for ${point.date.format(MONTH_DAY)}") {
                onOpenNight(point.sessionId)
                true
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .semantics {
                    contentDescription = if (summary.durationSeries.isEmpty()) {
                        "No recorded nights in this range."
                    } else {
                        "Nightly sleep durations. $spokenSummary. Target " +
                            "${formatDuration(summary.targetSleepMinutes.toLong())}."
                    }
                    customActions = nightActions
                }
                .pointerInput(summary) {
                    detectTapGestures { offset ->
                        if (windowDays <= 0) return@detectTapGestures
                        val slot = size.width / windowDays
                        if (slot <= 0f) return@detectTapGestures
                        val index = (offset.x / slot).toInt().coerceIn(0, windowDays - 1)
                        val tappedDate = start.plusDays(index.toLong())
                        byDate[tappedDate]?.let { point -> onOpenNight(point.sessionId) }
                    }
                }
        ) {
            // Baseline so empty days read as empty slots, not a broken chart.
            drawLine(
                color = NoctaSurfaceBorder,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1.dp.toPx()
            )

            val slot = size.width / windowDays
            val barWidth = (slot * 0.62f).coerceAtLeast(1.5f)

            summary.durationSeries.forEach { point ->
                val dayOffset = point.date.toEpochDay() - start.toEpochDay()
                if (dayOffset !in 0 until windowDays) return@forEach
                val fraction = (point.minutes.toFloat() / scaleMax).coerceIn(0f, 1f)
                val barHeight = size.height * fraction
                drawRoundRect(
                    color = NoctaAccent,
                    topLeft = Offset(
                        x = dayOffset * slot + (slot - barWidth) / 2f,
                        y = size.height - barHeight
                    ),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }

            // Target line, last so it stays visible over the bars.
            val targetFraction =
                (summary.targetSleepMinutes.toFloat() / scaleMax).coerceIn(0f, 1f)
            val targetY = size.height - size.height * targetFraction
            drawLine(
                color = NoctaTextSecondary.copy(alpha = 0.55f),
                start = Offset(0f, targetY),
                end = Offset(size.width, targetY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )
        }

        Spacer(Modifier.height(NoctaSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(1.dp)
                    .background(NoctaTextSecondary.copy(alpha = 0.55f))
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Target ${formatDuration(summary.targetSleepMinutes.toLong())}",
                style = NoctaTypography.labelSmall,
                color = NoctaTextSecondary
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${start.format(MONTH_DAY)} – ${today.format(MONTH_DAY)}",
                style = NoctaTypography.labelSmall,
                color = NoctaTextTertiary
            )
        }
    }
}

@Composable
private fun SleepDebtRow(status: SleepDebtStatus, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoctaShapes.medium)
            .background(NoctaSurface)
            .border(1.dp, NoctaSurfaceBorder, NoctaShapes.medium)
            .clickable(onClickLabel = "How sleep debt is estimated", onClick = onOpen)
            .padding(horizontal = NoctaSpacing.md, vertical = NoctaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Sleep debt",
                style = NoctaTypography.labelSmall,
                color = NoctaTextTertiary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = when (status) {
                    SleepDebtStatus.WELL_RESTED -> "You're well rested"
                    SleepDebtStatus.SLIGHTLY_BEHIND -> "Slightly behind on sleep"
                    SleepDebtStatus.CATCHING_UP -> "Catching up on sleep"
                },
                style = NoctaTypography.bodyLarge,
                color = NoctaTextPrimary
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = NoctaTextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun StreaksRow(summary: TrendSummary) {
    Row(horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)) {
        MetricCard(
            "Current streak",
            "${summary.currentStreakNights} ${if (summary.currentStreakNights == 1) "night" else "nights"}",
            caption = "a missed night resets it",
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            "Best streak",
            "${summary.longestStreakNights} ${if (summary.longestStreakNights == 1) "night" else "nights"}",
            caption = "in these ${summary.windowDays} days",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DistributionCard(summary: TrendSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoctaShapes.medium)
            .background(NoctaSurface)
            .border(1.dp, NoctaSurfaceBorder, NoctaShapes.medium)
            .padding(NoctaSpacing.md)
    ) {
        Text(
            "Score distribution",
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "${summary.missedNights} of ${summary.windowDays} nights untracked",
            style = NoctaTypography.labelSmall,
            color = NoctaTextTertiary
        )
        Spacer(Modifier.height(NoctaSpacing.md))
        summary.scoreDistribution.forEach { band ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    band.band,
                    style = NoctaTypography.bodyMedium,
                    color = NoctaTextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${band.count} ${if (band.count == 1) "night" else "nights"}",
                    style = NoctaTypography.bodyMedium,
                    color = NoctaTextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun formatDuration(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0L) "${h}h" else "${h}h ${m}m"
}

private fun formatClock(minutesSinceMidnight: Int): String {
    val hour24 = (minutesSinceMidnight / 60) % 24
    val minute = minutesSinceMidnight % 60
    val hour12 = when (val h = hour24 % 12) { 0 -> 12; else -> h }
    val amPm = if (hour24 < 12) "AM" else "PM"
    return String.format(Locale.US, "%d:%02d %s", hour12, minute, amPm)
}
