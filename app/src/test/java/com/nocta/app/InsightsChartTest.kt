package com.nocta.app

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nocta.app.data.local.NoctaDatabase
import com.nocta.app.data.local.SleepSessionEntity
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Seeds one real night, opens Insights in the running app and taps the bar for
 * that night. Proves the chart is drawn from the database, shows the age-range
 * target, and that a bar opens the matching night report.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-xhdpi")
class InsightsChartTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun epochMinute(dateTime: LocalDateTime): Long =
        dateTime.toEpochSecond(ZoneOffset.UTC) / 60

    private fun seedNight() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.databaseBuilder(context, NoctaDatabase::class.java, "nocta.db")
            .allowMainThreadQueries()
            .build()
        val night = LocalDate.now().minusDays(1)
        val bedtime = night.atTime(23, 0)
        val sleep = night.atTime(23, 10)
        val wake = night.plusDays(1).atTime(6, 40)

        runBlocking {
            database.sleepDao().upsert(
                SleepSessionEntity(
                    id = SEEDED_ID,
                    dateEpochDay = night.toEpochDay(),
                    bedtimeEpochMinute = epochMinute(bedtime),
                    sleepAttemptEpochMinute = epochMinute(bedtime),
                    estimatedSleepEpochMinute = epochMinute(sleep),
                    wakeEpochMinute = epochMinute(wake),
                    nightAwakenings = 1,
                    sleepQuality = 4,
                    morningEnergy = 3,
                    source = "SELF_REPORTED",
                    synced = true
                )
            )
        }
        database.close()
    }

    @Test
    fun `a real night is charted with its target and opens its report`() {
        seedNight()

        compose.onNodeWithText("Insights").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Time asleep per night")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Target line legend comes from the age profile (18–60 → 7–10 h → 8h30m).
        compose.onNodeWithText("Target 8h 30m").assertExists()

        val chart = compose.onNode(
            hasContentDescription("Nightly sleep durations", substring = true)
        )
        chart.assertExists()

        // 7-day window: yesterday is the sixth of seven slots (index 5).
        chart.performTouchInput {
            click(Offset(width * 5.5f / 7f, height * 0.5f))
        }

        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Night report").fetchSemanticsNodes().isNotEmpty()
        }
        // The report body loads from Room after the header appears.
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Time asleep").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Time asleep").assertExists()
    }

    private companion object {
        const val SEEDED_ID = "seeded-chart-night"
    }
}
