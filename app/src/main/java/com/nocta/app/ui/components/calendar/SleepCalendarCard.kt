package com.nocta.app.ui.components.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nocta.app.domain.model.DailySleepRecord
import com.nocta.app.domain.model.SleepQualityCategory
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

enum class CalendarViewMode { WEEKLY, MONTHLY, YEARLY }

@Composable
fun SleepCalendarCard(
    sessions: List<SleepSession>,
    modifier: Modifier = Modifier,
    computeScore: (List<SleepSession>) -> Int = { it.firstOrNull()?.sleepQuality?.times(20) ?: 70 }
) {
    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTHLY) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var referenceDate by remember { mutableStateOf(LocalDate.now()) }

    val sessionMap = remember(sessions) {
        sessions.associateBy { it.date }
    }

    fun getRecordForDate(date: LocalDate): DailySleepRecord {
        val s = sessionMap[date]
        return if (s != null) {
            val sc = computeScore(listOf(s))
            DailySleepRecord(date, s, sc, SleepQualityCategory.fromScore(sc))
        } else {
            DailySleepRecord(date, null, null, SleepQualityCategory.UNRECORDED)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(NoctaShapes.large)
            .background(NoctaSurface)
            .padding(NoctaSpacing.md)
    ) {
        // Mode Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sleep Calendar",
                style = NoctaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = NoctaTextPrimary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CalendarViewMode.entries.forEach { mode ->
                    val isSelected = mode == viewMode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NoctaAccent else NoctaSurfaceElevated)
                            .clickable { viewMode = mode }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = NoctaTypography.bodySmall.copy(fontSize = 11.sp),
                            color = if (isSelected) Color.White else NoctaTextSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(NoctaSpacing.md))

        when (viewMode) {
            CalendarViewMode.WEEKLY -> WeeklyCalendarView(
                referenceDate = referenceDate,
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it },
                onNavigateWeek = { referenceDate = referenceDate.plusWeeks(it.toLong()) },
                getRecord = ::getRecordForDate
            )
            CalendarViewMode.MONTHLY -> MonthlyCalendarView(
                referenceMonth = YearMonth.from(referenceDate),
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it },
                onNavigateMonth = { referenceDate = referenceDate.plusMonths(it.toLong()) },
                getRecord = ::getRecordForDate
            )
            CalendarViewMode.YEARLY -> YearlyCalendarView(
                referenceYear = referenceDate.year,
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it },
                onNavigateYear = { referenceDate = referenceDate.plusYears(it.toLong()) },
                getRecord = ::getRecordForDate
            )
        }

        Spacer(Modifier.height(NoctaSpacing.md))

        // Selected Date Detail Card
        val selectedRecord = getRecordForDate(selectedDate)
        SelectedDateDetail(selectedRecord)

        Spacer(Modifier.height(NoctaSpacing.md))

        // 5-Color Quality Legend
        CalendarLegend()
    }
}

@Composable
private fun WeeklyCalendarView(
    referenceDate: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onNavigateWeek: (Int) -> Unit,
    getRecord: (LocalDate) -> DailySleepRecord
) {
    val startOfWeek = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekDays = (0..6).map { startOfWeek.plusDays(it.toLong()) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onNavigateWeek(-1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Week", tint = NoctaTextPrimary)
            }
            Text(
                text = "${startOfWeek.format(DateTimeFormatter.ofPattern("MMM d"))} - ${weekDays.last().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                style = NoctaTypography.bodyMedium,
                color = NoctaTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = { onNavigateWeek(1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Week", tint = NoctaTextPrimary)
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            weekDays.forEach { date ->
                val record = getRecord(date)
                val isSelected = date == selectedDate
                val isToday = date == LocalDate.now()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) NoctaSurfaceElevated else Color.Transparent)
                        .border(
                            width = if (isToday) 1.5.dp else 0.dp,
                            color = if (isToday) NoctaAccent else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onDateSelected(date) }
                        .padding(6.dp)
                ) {
                    Text(
                        text = date.dayOfWeek.name.take(3),
                        style = NoctaTypography.labelSmall,
                        color = NoctaTextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(record.category.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${date.dayOfMonth}",
                            style = NoctaTypography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyCalendarView(
    referenceMonth: YearMonth,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onNavigateMonth: (Int) -> Unit,
    getRecord: (LocalDate) -> DailySleepRecord
) {
    val firstOfMonth = referenceMonth.atDay(1)
    val dayOfWeekOffset = firstOfMonth.dayOfWeek.value - 1
    val daysInMonth = referenceMonth.lengthOfMonth()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onNavigateMonth(-1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month", tint = NoctaTextPrimary)
            }
            Text(
                text = referenceMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = NoctaTypography.bodyMedium,
                color = NoctaTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = { onNavigateMonth(1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = NoctaTextPrimary)
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { dayLabel ->
                Text(text = dayLabel, style = NoctaTypography.labelSmall, color = NoctaTextSecondary)
            }
        }

        Spacer(Modifier.height(6.dp))

        val totalGridSlots = dayOfWeekOffset + daysInMonth
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(200.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(totalGridSlots) { index ->
                if (index >= dayOfWeekOffset) {
                    val dayNum = index - dayOfWeekOffset + 1
                    val date = referenceMonth.atDay(dayNum)
                    val record = getRecord(date)
                    val isSelected = date == selectedDate

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(record.category.color)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onDateSelected(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$dayNum",
                            style = NoctaTypography.bodySmall.copy(fontSize = 11.sp),
                            color = Color.White
                        )
                    }
                } else {
                    Spacer(Modifier.aspectRatio(1f))
                }
            }
        }
    }
}

@Composable
private fun YearlyCalendarView(
    referenceYear: Int,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onNavigateYear: (Int) -> Unit,
    getRecord: (LocalDate) -> DailySleepRecord
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onNavigateYear(-1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Year", tint = NoctaTextPrimary)
            }
            Text(
                text = "Year $referenceYear",
                style = NoctaTypography.bodyMedium,
                color = NoctaTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = { onNavigateYear(1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Year", tint = NoctaTextPrimary)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Heatmap grid of 12 month tiles
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.height(220.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(12) { monthIdx ->
                val month = YearMonth.of(referenceYear, monthIdx + 1)
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NoctaSurfaceElevated)
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = month.month.name.take(3),
                        style = NoctaTypography.labelSmall.copy(fontSize = 10.sp),
                        color = NoctaTextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        (1..minOf(5, month.lengthOfMonth())).forEach { d ->
                            val date = month.atDay(d)
                            val rec = getRecord(date)
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(rec.category.color)
                                    .clickable { onDateSelected(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDateDetail(record: DailySleepRecord) {
    Surface(
        color = NoctaSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = record.date.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")),
                    style = NoctaTypography.bodyMedium,
                    color = NoctaTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (record.session != null) {
                        "Duration: ${record.session.durationMinutes / 60}h ${record.session.durationMinutes % 60}m"
                    } else "No sleep record logged",
                    style = NoctaTypography.bodySmall,
                    color = NoctaTextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(record.category.color)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (record.score != null) "${record.category.label} (${record.score})" else record.category.label,
                    style = NoctaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun CalendarLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        listOf(
            SleepQualityCategory.EXCELLENT,
            SleepQualityCategory.GOOD,
            SleepQualityCategory.FAIRLY_GOOD,
            SleepQualityCategory.POOR,
            SleepQualityCategory.VERY_POOR
        ).forEach { cat ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(cat.color)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = cat.label.take(4),
                    style = NoctaTypography.labelSmall.copy(fontSize = 9.sp),
                    color = NoctaTextTertiary
                )
            }
        }
    }
}
