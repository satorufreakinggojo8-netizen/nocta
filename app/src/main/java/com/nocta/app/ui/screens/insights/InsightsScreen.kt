package com.nocta.app.ui.screens.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nocta.app.ui.components.MetricCard
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
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Trends computed exclusively from the user's stored sleep records. Charts
 * render real points only — nights without records appear as gaps, never as
 * fabricated values.
 */
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = hiltViewModel()
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
                "Insights",
                style = NoctaTypography.displayLarge,
                color = NoctaTextPrimary,
                modifier = Modifier.padding(top = NoctaSpacing.md)
            )
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
                    item { EmptyState() }
                } else {
                    item { StatsGrid(summary) }
                    item { DurationChart(summary) }
                    item { StreaksRow(summary) }
                    item { DistributionCard(summary) }
                }
            }
        }

        item {
            Text(
                text = "All figures come from your saved sleep records. " +
                    "Missing nights are counted, never invented.",
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
                    .clickable { onSelect(window) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
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
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoctaShapes.large)
            .background(NoctaSurface)
            .border(1.dp, NoctaSurfaceBorder, NoctaShapes.large)
            .padding(NoctaSpacing.lg)
    ) {
        Text(
            "No sleep records in this window",
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(NoctaSpacing.sm))
        Text(
            "Track a night or log sleep manually, and your trends will appear here.",
            style = NoctaTypography.bodyMedium,
            color = NoctaTextSecondary
        )
    }
}

@Composable
private fun StatsGrid(summary: com.nocta.app.domain.usecase.TrendSummary) {
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

@Composable
private fun DurationChart(summary: com.nocta.app.domain.usecase.TrendSummary) {
    val points = summary.durationSeries
    val maxMinutes = (points.maxOfOrNull { it.minutes } ?: 0L).coerceAtLeast(60L)

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
            "Bar height is minutes asleep; gaps are untracked nights.",
            style = NoctaTypography.labelSmall,
            color = NoctaTextTertiary
        )
        Spacer(Modifier.height(NoctaSpacing.md))

        // Textual equivalent for screen readers — charts must not be color-only.
        val summaryText = points.joinToString("; ") {
            "${it.date.format(MONTH_DAY)}: ${formatDuration(it.minutes)}"
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .semantics {
                    contentDescription = "Nightly sleep durations. $summaryText"
                }
        ) {
            if (points.isEmpty()) return@Canvas
            val slot = size.width / points.size
            val barWidth = slot * 0.6f
            points.forEachIndexed { index, point ->
                val fraction = point.minutes.toFloat() / maxMinutes
                val barHeight = size.height * fraction.coerceIn(0f, 1f)
                drawRoundRect(
                    color = NoctaAccent,
                    topLeft = Offset(
                        x = index * slot + (slot - barWidth) / 2f,
                        y = size.height - barHeight
                    ),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }
    }
}

@Composable
private fun StreaksRow(summary: com.nocta.app.domain.usecase.TrendSummary) {
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
private fun DistributionCard(summary: com.nocta.app.domain.usecase.TrendSummary) {
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

private val MONTH_DAY: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

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
