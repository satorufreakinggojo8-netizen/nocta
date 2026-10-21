package com.nocta.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nocta.app.domain.model.SleepDayQuality
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.usecase.CalendarPeriod
import com.nocta.app.domain.usecase.SleepCalendar
import com.nocta.app.domain.usecase.SleepQualityClassifier
import com.nocta.app.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Sleep-history calendar backed by the saved Room sleep records.
 *
 * Each day is colored by [SleepQualityClassifier]'s five-level quality of the
 * session saved for that date; days without records stay neutral. Week, month
 * and year views all navigate through real dates, forward navigation is
 * clamped to the present, and the legend always spells out every level so the
 * map never relies on color alone.
 */
@Composable
fun SleepCalendarCard(
    sessions: List<SleepSession>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var periodName by rememberSaveable {
        mutableStateOf(CalendarPeriod.MONTH.name)
    }
    var anchorEpochDay by rememberSaveable {
        mutableStateOf(LocalDate.now().toEpochDay())
    }
    // -1 is the "nothing selected" sentinel; real epochs for displayed dates
    // are far above it.
    var selectedEpochDay by rememberSaveable { mutableStateOf(-1L) }

    val period = CalendarPeriod.entries.firstOrNull { it.name == periodName }
        ?: CalendarPeriod.MONTH
    val anchor = LocalDate.ofEpochDay(anchorEpochDay)
    val today = LocalDate.now()
    val classifier = remember { SleepQualityClassifier() }
    val locale = remember { Locale.getDefault() }

    val sessionsByDate = remember(sessions) { sessions.associateBy { it.date } }
    val monthScores = remember(sessions) {
        sessions
            .groupBy { YearMonth.from(it.date) }
            .mapValues { (_, nights) -> nights.map { classifier.scoreNight(it) }.average() }
    }
    val monthNightCounts = remember(sessions) {
        sessions.groupingBy { YearMonth.from(it.date) }.eachCount()
    }

    val selectedDate = selectedEpochDay
        .takeIf { it >= 0 }
        ?.let { LocalDate.ofEpochDay(it) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(NoctaShapes.large)
            .background(
                Brush.verticalGradient(listOf(NoctaSurfaceElevated, NoctaSurface))
            )
            .padding(NoctaSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = SleepCalendar.title(period, anchor, locale),
                style = NoctaTypography.titleMedium,
                color = NoctaTextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    anchorEpochDay =
                        SleepCalendar.previousPeriod(period, anchor).toEpochDay()
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = "Previous ${period.name.lowercase()}",
                    tint = NoctaTextSecondary
                )
            }
            val canGoNext = SleepCalendar.canGoNext(period, anchor, today)
            IconButton(
                onClick = {
                    anchorEpochDay =
                        SleepCalendar.nextPeriod(period, anchor, today).toEpochDay()
                },
                enabled = canGoNext
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Next ${period.name.lowercase()}",
                    tint = if (canGoNext) NoctaTextSecondary else NoctaTextTertiary.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(Modifier.height(NoctaSpacing.sm))

        PeriodSelector(
            current = period,
            onSelected = { newPeriod ->
                periodName = newPeriod.name
                anchorEpochDay = today.toEpochDay()
            }
        )

        Spacer(Modifier.height(NoctaSpacing.md))

        when {
            isLoading -> CalendarLoadingPlaceholder()
            else -> when (period) {
                CalendarPeriod.WEEK -> WeekGrid(
                    anchor = anchor,
                    sessionsByDate = sessionsByDate,
                    classifier = classifier,
                    selectedDate = selectedDate,
                    onSelect = { selectedEpochDay = it.toEpochDay() }
                )

                CalendarPeriod.MONTH -> MonthGrid(
                    anchor = anchor,
                    sessionsByDate = sessionsByDate,
                    classifier = classifier,
                    selectedDate = selectedDate,
                    onSelect = { selectedEpochDay = it.toEpochDay() }
                )

                CalendarPeriod.YEAR -> YearGrid(
                    anchor = anchor,
                    monthScores = monthScores,
                    monthNightCounts = monthNightCounts,
                    classifier = classifier,
                    onSelectMonth = { month ->
                        periodName = CalendarPeriod.MONTH.name
                        anchorEpochDay = month.atDay(1).toEpochDay()
                    }
                )
            }
        }

        if (!isLoading && sessions.isEmpty()) {
            Spacer(Modifier.height(NoctaSpacing.sm))
            Text(
                text = "No sleep records yet — nights you track or log will appear here.",
                style = NoctaTypography.bodySmall,
                color = NoctaTextTertiary
            )
        }

        Spacer(Modifier.height(NoctaSpacing.md))

        QualityLegend()

        if (period != CalendarPeriod.YEAR && selectedDate != null) {
            Spacer(Modifier.height(NoctaSpacing.md))
            SelectedDayDetail(
                date = selectedDate,
                session = sessionsByDate[selectedDate],
                classifier = classifier
            )
        }
    }
}

@Composable
private fun PeriodSelector(
    current: CalendarPeriod,
    onSelected: (CalendarPeriod) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CalendarPeriod.entries.forEach { candidate ->
            val selected = candidate == current
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) NoctaAccentMuted else Color.Transparent)
                    .clickable { onSelected(candidate) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = candidate.name.lowercase().replaceFirstChar { it.uppercaseChar() },
                    style = NoctaTypography.labelSmall,
                    color = if (selected) NoctaAccent else NoctaTextTertiary
                )
            }
        }
    }
}

@Composable
private fun WeekGrid(
    anchor: LocalDate,
    sessionsByDate: Map<LocalDate, SleepSession>,
    classifier: SleepQualityClassifier,
    selectedDate: LocalDate?,
    onSelect: (LocalDate) -> Unit
) {
    val days = SleepCalendar.weekDays(anchor)
    WeekdayLabelRow()
    Spacer(Modifier.height(NoctaSpacing.xs))
    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEach { date ->
            DayCell(
                date = date,
                quality = classifier.classify(sessionsByDate[date]),
                isSelected = date == selectedDate,
                isToday = date == LocalDate.now(),
                onSelect = onSelect,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthGrid(
    anchor: LocalDate,
    sessionsByDate: Map<LocalDate, SleepSession>,
    classifier: SleepQualityClassifier,
    selectedDate: LocalDate?,
    onSelect: (LocalDate) -> Unit
) {
    val grid = SleepCalendar.monthGrid(anchor)
    WeekdayLabelRow()
    Spacer(Modifier.height(NoctaSpacing.xs))
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        grid.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f).padding(2.dp))
                    } else {
                        DayCell(
                            date = date,
                            quality = classifier.classify(sessionsByDate[date]),
                            isSelected = date == selectedDate,
                            isToday = date == LocalDate.now(),
                            onSelect = onSelect,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YearGrid(
    anchor: LocalDate,
    monthScores: Map<YearMonth, Double>,
    monthNightCounts: Map<YearMonth, Int>,
    classifier: SleepQualityClassifier,
    onSelectMonth: (YearMonth) -> Unit
) {
    val labels = remember { SleepCalendar.monthLabels() }
    Column(verticalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)) {
        SleepCalendar.yearMonths(anchor).chunked(3).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { month ->
                    MonthCell(
                        month = month,
                        label = labels[month.monthValue - 1],
                        quality = monthScores[month]?.let { classifier.levelForScore(it) }
                            ?: SleepDayQuality.NO_RECORD,
                        nightCount = monthNightCounts[month] ?: 0,
                        onClick = { onSelectMonth(month) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekdayLabelRow() {
    val labels = remember { SleepCalendar.weekdayLabels() }
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEach { label ->
            Text(
                text = label,
                style = NoctaTypography.labelSmall,
                color = NoctaTextTertiary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    quality: SleepDayQuality,
    isSelected: Boolean,
    isToday: Boolean,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val color = qualityColor(quality)
    val cellBackground = if (quality == SleepDayQuality.NO_RECORD) {
        Color.Transparent
    } else {
        color.copy(alpha = 0.28f)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(cellBackground)
            .then(
                when {
                    isSelected -> Modifier.border(2.dp, NoctaAccent, CircleShape)
                    isToday -> Modifier.border(1.dp, NoctaTextSecondary, CircleShape)
                    else -> Modifier
                }
            )
            .clickable { onSelect(date) }
            .semantics {
                contentDescription = "${date.dayOfMonth} " +
                    date.month.getDisplayName(
                        java.time.format.TextStyle.SHORT,
                        Locale.getDefault()
                    ) + ", ${quality.displayName} sleep"
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${date.dayOfMonth}",
            style = NoctaTypography.labelSmall,
            color = if (quality == SleepDayQuality.NO_RECORD) {
                NoctaTextTertiary
            } else {
                color
            }
        )
    }
}

@Composable
private fun MonthCell(
    month: YearMonth,
    label: String,
    quality: SleepDayQuality,
    nightCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = qualityColor(quality)
    Column(
        modifier = modifier
            .clip(NoctaShapes.small)
            .clickable { onClick() }
            .padding(vertical = NoctaSpacing.xs, horizontal = NoctaSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = NoctaTypography.labelSmall,
            color = NoctaTextSecondary
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    if (quality == SleepDayQuality.NO_RECORD) {
                        NoctaSurface
                    } else {
                        color.copy(alpha = 0.28f)
                    }
                )
                .then(
                    if (quality == SleepDayQuality.NO_RECORD) {
                        Modifier.border(1.dp, NoctaSurfaceBorder, CircleShape)
                    } else {
                        Modifier
                    }
                )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (nightCount > 0) "$nightCount" else "—",
            style = NoctaTypography.labelSmall,
            color = if (nightCount > 0) NoctaTextSecondary else NoctaTextTertiary
        )
    }
}

@Composable
private fun QualityLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SleepDayQuality.entries.forEach { level ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (level == SleepDayQuality.NO_RECORD) {
                                NoctaTextTertiary.copy(alpha = 0.4f)
                            } else {
                                qualityColor(level)
                            }
                        )
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = level.displayName,
                    style = NoctaTypography.labelSmall,
                    color = if (level == SleepDayQuality.NO_RECORD) {
                        NoctaTextTertiary
                    } else {
                        NoctaTextSecondary
                    }
                )
            }
        }
    }
}

@Composable
private fun SelectedDayDetail(
    date: LocalDate,
    session: SleepSession?,
    classifier: SleepQualityClassifier
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NoctaSurfaceBorder)
        )
        Spacer(Modifier.height(NoctaSpacing.sm))
        Text(
            text = date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")),
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(NoctaSpacing.xs))
        if (session == null) {
            Text(
                text = "No sleep record for this day.",
                style = NoctaTypography.bodyMedium,
                color = NoctaTextTertiary
            )
        } else {
            val hours = session.durationMinutes / 60
            val minutes = session.durationMinutes % 60
            Text(
                text = "${hours}h ${minutes}m asleep • " +
                    classifier.classify(session).displayName,
                style = NoctaTypography.bodyMedium,
                color = NoctaTextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Bedtime ${session.bedtime.format(TIME_FMT)} • " +
                    "Wake ${session.wakeTime.format(TIME_FMT)} • " +
                    session.source.name.lowercase().replace('_', ' '),
                style = NoctaTypography.labelSmall,
                color = NoctaTextTertiary
            )
        }
    }
}

@Composable
private fun CalendarLoadingPlaceholder() {
    Column(verticalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)) {
        repeat(4) {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(NoctaSurface)
                    )
                }
            }
        }
    }
}

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

private fun qualityColor(level: SleepDayQuality) = when (level) {
    SleepDayQuality.EXCELLENT -> NoctaScoreExcellent
    SleepDayQuality.GOOD -> NoctaScoreGood
    SleepDayQuality.FAIR -> NoctaScoreFair
    SleepDayQuality.POOR -> NoctaScorePoor
    SleepDayQuality.VERY_POOR -> NoctaScoreVeryPoor
    SleepDayQuality.NO_RECORD -> NoctaTextTertiary
}
