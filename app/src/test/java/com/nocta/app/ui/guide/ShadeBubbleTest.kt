package com.nocta.app.ui.guide

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The bubble has to stay readable and operable: the tip is visible, the mute
 * control has its own label and touch target, and tapping the bubble does the
 * thing the label promises.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-xhdpi")
class ShadeBubbleTest {

    @get:Rule
    val compose = createComposeRule()

    private val tip = ShadeTip(
        id = "test.tip",
        message = "Start tracking when you get into bed.",
        detail = "Tracking records the start and end only; the morning check-in scores it.",
        coachPrompt = "How does sleep tracking work?"
    )

    @Test
    fun `shows the tip, an ask label and a labelled mute control`() {
        var muted = false
        compose.setContent {
            ShadeBubble(
                tip = tip,
                aiAvailable = true,
                expanded = false,
                onActivate = {},
                onMute = { muted = true }
            )
        }

        compose.onNodeWithText(tip.message).assertIsDisplayed()
        compose.onNodeWithText("Ask Vela").assertIsDisplayed()

        compose.onNodeWithContentDescription("Mute Vela's tips").performClick()
        assertTrue("mute must be its own labelled control", muted)
    }

    @Test
    fun `tapping the bubble runs the promised action`() {
        var activated = false
        compose.setContent {
            ShadeBubble(
                tip = tip,
                aiAvailable = true,
                expanded = false,
                onActivate = { activated = true },
                onMute = {}
            )
        }

        compose.onNodeWithText(tip.message).performClick()
        assertTrue("tapping the bubble must ask Vela", activated)
    }

    @Test
    fun `without a backend the detail is shown instead of a dead end`() {
        compose.setContent {
            ShadeBubble(
                tip = tip,
                aiAvailable = false,
                expanded = true,
                onActivate = {},
                onMute = {}
            )
        }

        compose.onNodeWithText("More").assertDoesNotExist()
        compose.onNodeWithText("Less").assertIsDisplayed()
        compose.onNodeWithText(tip.detail).assertIsDisplayed()
        compose.onNodeWithText(
            "Coach replies aren't available in this build — this guidance comes from the app itself."
        ).assertIsDisplayed()
    }
}
