package com.nocta.app.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The dashboard screenshot showed "Sleep Score 0 — Needs attention" with no
 * usable data. This guards the replacement: an explicit empty state, no zero.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-xhdpi")
class SleepScoreEmptyCardTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsEmptyStateInsteadOfAZeroScore() {
        var logRequested = false
        compose.setContent {
            SleepScoreEmptyCard(onLogSleep = { logRequested = true })
        }

        compose.onNodeWithText("Sleep Score").assertIsDisplayed()
        compose.onNodeWithText("No score yet").assertIsDisplayed()
        compose.onNodeWithText("0").assertDoesNotExist()

        compose.onNodeWithText("Log a night").performClick()
        assertTrue("the empty state must offer a way to log a night", logRequested)
    }
}
