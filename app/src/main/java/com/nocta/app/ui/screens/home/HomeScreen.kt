package com.nocta.app.ui.screens.home

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.ui.components.SleepScoreCard
import com.nocta.app.ui.components.SleepScoreEmptyCard
import com.nocta.app.ui.guide.ShadeLayout
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
private val DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())

/**
 * The dashboard, deliberately small: one score, one line about last night, one
 * focus for today, and two actions. Longer explanations live on their own pages
 * (recommendation, sleep debt on Insights, night report), reached by tapping.
 */
@Composable
fun HomeScreen(
    onOpenSleepDetails: (String) -> Unit,
    onOpenWindDown: () -> Unit,
    onLogSleep: () -> Unit,
    onOpenRecommendation: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val greeting = greetingFor(LocalTime.now())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NoctaBackground),
        contentPadding = PaddingValues(
            start = NoctaSpacing.md,
            end = NoctaSpacing.md,
            top = NoctaSpacing.md,
            // The companion and its bubble rest at the bottom; content ends
            // above them rather than underneath.
            bottom = ShadeLayout.HomeBottomInset
        ),
        verticalArrangement = Arrangement.spacedBy(NoctaSpacing.lg)
    ) {
        item {
            Column(modifier = Modifier.padding(top = NoctaSpacing.sm)) {
                Text(
                    greeting,
                    style = NoctaTypography.displayLarge,
                    color = NoctaTextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    LocalDate.now().format(DATE_FMT),
                    style = NoctaTypography.labelSmall,
                    color = NoctaTextTertiary
                )
            }
        }

        item {
            val breakdown = uiState.scoreBreakdown
            when {
                uiState.isLoading -> ScoreCardSkeleton()
                breakdown != null && breakdown.hasData -> SleepScoreCard(
                    breakdown = breakdown,
                    mode = uiState.scoreCardMode,
                    onModeChange = viewModel::setScoreCardMode,
                    onTap = {
                        uiState.lastNight?.id?.let(onOpenSleepDetails)
                    }
                )
                else -> SleepScoreEmptyCard(onLogSleep = onLogSleep)
            }
        }

        uiState.lastNight?.let { session ->
            item {
                LastNightLine(
                    session = session,
                    onOpen = { onOpenSleepDetails(session.id) }
                )
            }
        }

        uiState.todaysRecommendation?.let { recommendation ->
            item {
                FocusLine(
                    title = recommendation.title,
                    onOpen = onOpenRecommendation
                )
            }
        }

        item {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)
                ) {
                    QuickActionButton("Log sleep", Modifier.weight(1f), onLogSleep)
                    QuickActionButton("Wind down", Modifier.weight(1f), onOpenWindDown)
                }
            }
        }
    }
}

@Composable
private fun LastNightLine(session: SleepSession, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoctaShapes.medium)
            .background(NoctaSurface)
            .border(1.dp, NoctaSurfaceBorder, NoctaShapes.medium)
            .clickable(onClickLabel = "Open night report", onClick = onOpen)
            .padding(horizontal = NoctaSpacing.md, vertical = NoctaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Last night",
                style = NoctaTypography.labelSmall,
                color = NoctaTextTertiary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                formatDuration(session.durationMinutes),
                style = NoctaTypography.titleMedium,
                color = NoctaTextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Bed ${session.bedtime.format(TIME_FMT)} · Wake ${session.wakeTime.format(TIME_FMT)}",
                style = NoctaTypography.labelSmall,
                color = NoctaTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
private fun FocusLine(title: String, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoctaShapes.medium)
            .background(NoctaSurface)
            .border(1.dp, NoctaSurfaceBorder, NoctaShapes.medium)
            .clickable(onClickLabel = "Open today's focus", onClick = onOpen)
            .padding(horizontal = NoctaSpacing.md, vertical = NoctaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Today's focus",
                style = NoctaTypography.labelSmall,
                color = NoctaTextTertiary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                title,
                style = NoctaTypography.bodyLarge,
                color = NoctaTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
private fun QuickActionButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
            containerColor = NoctaSurface,
            contentColor = NoctaTextPrimary
        ),
        shape = NoctaShapes.medium
    ) {
        Text(
            label,
            style = NoctaTypography.bodyMedium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ScoreCardSkeleton() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(NoctaShapes.large)
            .background(NoctaSurface)
    )
}

private fun greetingFor(time: LocalTime): String = when (time.hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Late night"
}

internal fun formatDuration(minutes: Long): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (remainder == 0L) "${hours}h" else "${hours}h ${remainder}m"
}
