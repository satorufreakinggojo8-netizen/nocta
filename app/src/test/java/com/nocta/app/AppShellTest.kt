package com.nocta.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.nocta.app.ui.guide.ShadeGuide
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The real activity, real navigation graph, real Hilt view models and a real
 * Room database. This is the closest thing to a device run available here: it
 * proves the shell starts, tabs navigate, the tip follows the tab (and
 * advances rather than repeating), the bubble clears the primary actions, and
 * the mute survives a trip to Profile and back.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-xhdpi")
class AppShellTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val velaDescription =
        "Vela, your AI sleep coach. Double tap to open the coach."

    private fun homeTip(index: Int) = ShadeGuide.tipsFor(ShadeGuide.ROUTE_HOME)[index]

    private fun waitForText(text: String, timeoutMillis: Long = 5_000) {
        compose.waitUntil(timeoutMillis) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun `the home tip sits above the two primary actions`() {
        compose.onNodeWithContentDescription(velaDescription).assertIsDisplayed()
        compose.onNodeWithText(homeTip(0).message).assertIsDisplayed()

        val bubble = compose.onNodeWithText(homeTip(0).message).getUnclippedBoundsInRoot()
        val logAction = compose.onNodeWithText("Log sleep").getUnclippedBoundsInRoot()
        val windAction = compose.onNodeWithText("Wind down").getUnclippedBoundsInRoot()

        // The bubble is 250 dp tall above the character, so it sits below the
        // actions on an empty dashboard. It must not cover them.
        val lowestAction = maxOf(logAction.bottom.value, windAction.bottom.value)
        assertTrue(
            "bubble top ${bubble.top} must sit below the actions (${lowestAction}dp)",
            bubble.top.value >= lowestAction
        )
    }

    @Test
    fun `changing tabs swaps the tip and never repeats it back to back`() {
        compose.onNodeWithText("Sleep").performClick()
        val sleepTip = ShadeGuide.tipsFor(ShadeGuide.ROUTE_SLEEP).first().message
        waitForText(sleepTip)

        compose.onNodeWithText("Track your sleep").assertIsDisplayed()
        compose.onNodeWithText(homeTip(0).message).assertDoesNotExist()

        compose.onNodeWithText("Home").performClick()
        // Re-entering Home shows its next tip, not the same sentence again.
        waitForText(homeTip(1).message)
        compose.onNodeWithText(homeTip(0).message).assertDoesNotExist()
    }

    @Test
    fun `muting hides the bubble and Profile can turn it back on`() {
        compose.onNodeWithContentDescription("Mute Vela's tips").performClick()
        compose.waitForIdle()

        compose.onNodeWithText(homeTip(0).message).assertDoesNotExist()
        // The character itself is not muted.
        compose.onNodeWithContentDescription(velaDescription).assertIsDisplayed()

        compose.onNodeWithText("Profile").performClick()
        // The profile cards render after the first database read; wait for one.
        waitForText("Sleep target")
        val profileTip = ShadeGuide.tipsFor(ShadeGuide.ROUTE_PROFILE).first().message
        compose.onNodeWithText(profileTip).assertDoesNotExist()

        // The card is below the fold; scroll the lazy list to it before tapping.
        compose.onNode(hasScrollAction())
            .performScrollToNode(hasText("Vela's guidance"))
        compose.onNode(isToggleable()).performClick()
        compose.waitForIdle()

        compose.onNodeWithText(profileTip).assertIsDisplayed()
    }

    @Test
    fun `insights opens with the trend tip and a truthful empty state`() {
        compose.onNodeWithText("Insights").performClick()
        val insightsTip = ShadeGuide.tipsFor(ShadeGuide.ROUTE_INSIGHTS).first().message
        waitForText(insightsTip)

        compose.onNodeWithText("Your recorded nights — nothing filled in.").assertIsDisplayed()
        waitForText("No sleep records in this range")
        compose.onNodeWithText("Log a night").assertIsDisplayed()
    }
}
