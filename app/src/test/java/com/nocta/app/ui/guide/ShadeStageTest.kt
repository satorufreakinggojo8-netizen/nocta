package com.nocta.app.ui.guide

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The stage is the only place the tip and the character are combined, so this
 * guards two things the screenshots cannot: the bubble sits above the
 * character's head rather than over its face, and muting hides the bubble
 * without removing the character.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp-xhdpi")
class ShadeStageTest {

    @get:Rule
    val compose = createComposeRule()

    private val tip = ShadeGuide.tipsFor(ShadeGuide.ROUTE_HOME).first()

    private val velaDescription =
        "Vela, your AI sleep coach. Double tap to open the coach."

    @Test
    fun `the bubble rests above the character, never over its face`() {
        compose.setContent {
            ShadeStage(
                baseRoute = ShadeGuide.ROUTE_HOME,
                tip = tip,
                guideEnabled = true,
                detailExpanded = false,
                aiAvailable = false,
                onActivate = {},
                onMute = {},
                onOpenCoach = {}
            )
        }

        compose.onNodeWithText(tip.message).assertIsDisplayed()
        compose.onNodeWithContentDescription(velaDescription).assertIsDisplayed()

        val bubble = compose.onNodeWithText(tip.message).getUnclippedBoundsInRoot()
        val character = compose.onNodeWithContentDescription(velaDescription)
            .getUnclippedBoundsInRoot()

        assertTrue(
            "bubble bottom ${bubble.bottom} must be above the character top ${character.top}",
            bubble.bottom.value <= character.top.value
        )
    }

    @Test
    fun `muting hides the bubble but keeps the character`() {
        compose.setContent {
            ShadeStage(
                baseRoute = ShadeGuide.ROUTE_SLEEP,
                tip = ShadeGuide.tipsFor(ShadeGuide.ROUTE_SLEEP).first(),
                guideEnabled = false,
                detailExpanded = false,
                aiAvailable = false,
                onActivate = {},
                onMute = {},
                onOpenCoach = {}
            )
        }

        compose.onNodeWithText(
            ShadeGuide.tipsFor(ShadeGuide.ROUTE_SLEEP).first().message
        ).assertDoesNotExist()
        compose.onNodeWithContentDescription(velaDescription).assertIsDisplayed()
    }

    @Test
    fun `tapping the bubble and the character have separate actions`() {
        var activated = false
        var openedCoach = false

        compose.setContent {
            ShadeStage(
                baseRoute = ShadeGuide.ROUTE_HOME,
                tip = tip,
                guideEnabled = true,
                detailExpanded = false,
                aiAvailable = true,
                onActivate = { activated = true },
                onMute = {},
                onOpenCoach = { openedCoach = true }
            )
        }

        compose.onNodeWithText(tip.message).performClick()
        assertTrue(activated)
        assertFalse("the tip must not open the chat by itself", openedCoach)

        compose.onNodeWithContentDescription(velaDescription).performClick()
        assertTrue(openedCoach)
    }
}
