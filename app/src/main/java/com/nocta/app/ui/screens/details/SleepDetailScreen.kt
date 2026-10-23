package com.nocta.app.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nocta.app.domain.model.SleepDisturbance
import com.nocta.app.domain.usecase.NightlyFactor
import com.nocta.app.ui.components.MetricCard
import com.nocta.app.ui.theme.NoctaAccent
import com.nocta.app.ui.theme.NoctaBackground
import com.nocta.app.ui.theme.NoctaScorePoor
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

private val DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
private val TIME_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a")

/**
 * The detailed report for one recorded night — every value comes from the
 * stored session, its morning check-in and the score window ending at it.
 */
@Composable
fun SleepDetailScreen(
    onBack: () -> Unit,
    viewModel: SleepDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

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
                text = "Night report",
                style = NoctaTypography.titleMedium,
                color = NoctaTextPrimary,
                fontWeight = FontWeight.Medium
            )
        }

        when {
            state.isLoading -> LoadingState()
            state.notFound -> CenteredMessage(
                "No sleep record found for this night. It may have been deleted."
            )
            state.errorMessage != null -> CenteredMessage(state.errorMessage!!)
            state.report != null -> ReportContent(state.report!!)
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(NoctaSpacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Text("Loading night report…", color = NoctaTextTertiary)
    }
}

@Composable
private fun CenteredMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(NoctaSpacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = NoctaTextSecondary)
    }
}

@Composable
private fun ReportContent(report: com.nocta.app.domain.usecase.NightlyReport) {
    val session = report.session

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
            Column {
                Text(
                    text = session.date.format(DATE_FMT),
                    style = NoctaTypography.headlineMedium,
                    color = NoctaTextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = session.source.name.lowercase().replace('_', ' '),
                    style = NoctaTypography.labelSmall,
                    color = NoctaTextTertiary
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)) {
                MetricCard(
                    "Bedtime",
                    session.bedtime.format(TIME_FMT),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    "Wake time",
                    session.wakeTime.format(TIME_FMT),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(NoctaSpacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)) {
                MetricCard(
                    "Time asleep",
                    formatDuration(report.asleepMinutes),
                    caption = "of ${formatDuration(report.timeInBedMinutes)} in bed",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    "Efficiency",
                    report.efficiencyPercent?.let { "$it%" } ?: "—",
                    caption = report.efficiencyPercent?.let { null }
                        ?: "needs awake-time data",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sleep score",
                        style = NoctaTypography.titleMedium,
                        color = NoctaTextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${report.breakdown.overall} · ${report.breakdown.label}",
                        style = NoctaTypography.titleMedium,
                        color = NoctaAccent
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Over the ${report.nightsInScoreWindow}-night window ending this night",
                    style = NoctaTypography.labelSmall,
                    color = NoctaTextTertiary
                )
                Spacer(Modifier.height(NoctaSpacing.md))
                FactorRow("Duration", report.breakdown.durationScore, 40)
                FactorRow("Bedtime consistency", report.breakdown.consistencyScore, 25)
                FactorRow("Sleep quality", report.breakdown.qualityScore, 15)
                FactorRow("Schedule timing", report.breakdown.timingScore, 10)
                FactorRow("Night awakenings", report.breakdown.routineScore, 10)
            }
        }

        item {
            Column {
                Text(
                    text = "Vs your recent baseline",
                    style = NoctaTypography.titleMedium,
                    color = NoctaTextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(NoctaSpacing.sm))
                if (report.baselineNightsCompared == 0) {
                    Text(
                        text = "This is your first recorded night — no baseline yet.",
                        style = NoctaTypography.bodyMedium,
                        color = NoctaTextTertiary
                    )
                } else {
                    Text(
                        text = "Compared with your last ${report.baselineNightsCompared} nights",
                        style = NoctaTypography.labelSmall,
                        color = NoctaTextTertiary
                    )
                    Spacer(Modifier.height(NoctaSpacing.sm))
                    val durationDelta = report.durationDeltaMinutes
                    MetricRow(
                        "Sleep duration",
                        if (durationDelta == null) "—"
                        else signedDuration(durationDelta) + " vs average " +
                            formatDuration(report.baselineAvgDurationMinutes ?: 0)
                    )
                    val bedtimeDelta = report.bedtimeDeltaMinutes
                    MetricRow(
                        "Bedtime",
                        if (bedtimeDelta == null) "—"
                        else signedMinutes(bedtimeDelta) + " vs usual " +
                            report.baselineMeanBedtimeMinutes?.let { formatClock(it) }.orEmpty()
                    )
                }
            }
        }

        item {
            Column {
                Text(
                    text = "What went well",
                    style = NoctaTypography.titleMedium,
                    color = NoctaTextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(NoctaSpacing.xs))
                Text(
                    text = "${report.strongestFactor.name} — ${report.strongestFactor.bandLabel} " +
                        "(${report.strongestFactor.score}/100, ${report.strongestFactor.weightPercent}% of score)",
                    style = NoctaTypography.bodyMedium,
                    color = NoctaTextSecondary
                )
            }
        }

        item {
            Column {
                Text(
                    text = "Best opportunity",
                    style = NoctaTypography.titleMedium,
                    color = NoctaTextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(NoctaSpacing.xs))
                Text(
                    text = "${report.improvementFactor.name} — ${report.improvementFactor.bandLabel} " +
                        "(${report.improvementFactor.score}/100)",
                    style = NoctaTypography.bodyMedium,
                    color = NoctaTextSecondary
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(NoctaShapes.medium)
                    .background(NoctaSurface)
                    .border(1.dp, NoctaSurfaceBorder, NoctaShapes.medium)
                    .padding(NoctaSpacing.md)
            ) {
                Text(
                    text = "For your next night",
                    style = NoctaTypography.titleMedium,
                    color = NoctaTextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(NoctaSpacing.sm))
                Text(
                    text = report.recommendation,
                    style = NoctaTypography.bodyMedium,
                    color = NoctaTextSecondary
                )
            }
        }

        report.checkIn?.let { checkIn ->
            item {
                Column {
                    Text(
                        text = "Your morning check-in",
                        style = NoctaTypography.titleMedium,
                        color = NoctaTextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(NoctaSpacing.sm))
                    MetricRow("Sleep quality", "${checkIn.sleepQuality}/5")
                    MetricRow("Morning energy", "${checkIn.morningEnergy}/5")
                    MetricRow(
                        "Night awakenings",
                        "${checkIn.nightAwakenings}" +
                            if (checkIn.awakeMinutes > 0) " · ~${checkIn.awakeMinutes}m awake" else ""
                    )
                    MetricRow(
                        "Disturbance",
                        checkIn.disturbance.displayName()
                    )
                    checkIn.unusualEvent?.takeIf { it.isNotBlank() }?.let { event ->
                        MetricRow("Unusual event", event)
                    }
                }
            }
        }

        if (report.missingInputs.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "About this data",
                        style = NoctaTypography.titleMedium,
                        color = NoctaTextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(NoctaSpacing.sm))
                    report.missingInputs.forEach { note ->
                        Text(
                            text = "• $note",
                            style = NoctaTypography.bodyMedium,
                            color = NoctaTextTertiary
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                }
            }
        }

        item {
            Text(
                text = "This is a wellness report from your own records, not a medical assessment.",
                style = NoctaTypography.labelSmall,
                color = NoctaTextTertiary
            )
        }
    }
}

@Composable
private fun FactorRow(name: String, score: Int, weight: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = NoctaTypography.bodyMedium,
            color = NoctaTextSecondary,
            modifier = Modifier.width(150.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(NoctaShapes.small)
                .background(NoctaSurface)
        ) {
            val color = if (score < 50) NoctaScorePoor else NoctaAccent
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = score.coerceIn(0, 100) / 100f)
                    .height(6.dp)
                    .background(color)
            )
        }
        Spacer(Modifier.width(NoctaSpacing.sm))
        Text(
            text = "$score",
            style = NoctaTypography.labelSmall,
            color = NoctaTextPrimary,
            modifier = Modifier.semantics {
                contentDescription = "$name score $score out of 100, weight $weight percent"
            }
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = NoctaTypography.bodyMedium, color = NoctaTextSecondary)
        Text(
            value,
            style = NoctaTypography.bodyMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun SleepDisturbance.displayName(): String = name
    .lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase(Locale.getDefault()) }

private fun formatDuration(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0L) "${h}h" else "${h}h ${m}m"
}

private fun signedDuration(delta: Long): String = when {
    delta == 0L -> "no change"
    delta > 0 -> "+${formatDuration(delta)}"
    else -> "−${formatDuration(-delta)}"
}

private fun signedMinutes(delta: Int): String = when {
    delta == 0 -> "same time"
    delta > 0 -> "+${delta}m later"
    else -> "${-delta}m earlier"
}

private fun formatClock(minutesSinceMidnight: Int): String {
    val hour24 = (minutesSinceMidnight / 60) % 24
    val minute = minutesSinceMidnight % 60
    val hour12 = when (val h = hour24 % 12) { 0 -> 12; else -> h }
    val amPm = if (hour24 < 12) "AM" else "PM"
    return String.format(Locale.US, "%d:%02d %s", hour12, minute, amPm)
}
