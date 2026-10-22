package com.nocta.app.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.nocta.app.navigation.NoctaDestination
import com.nocta.app.navigation.bottomNavDestinations
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Layout regression test for the bottom navigation: "Optimize" used to wrap
 * onto two lines (with the last letter underneath) on a 360 dp phone. These
 * tests lay the real composable out at the narrowest width the app supports and
 * fail if any label becomes two lines or is not displayed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BottomNavigationLayoutTest {

    @get:Rule
    val compose = createComposeRule()

    /** 11 sp with a 14 sp line height: one line can never exceed ~16 dp. */
    private val oneLineMaxHeight = 18.dp

    private fun assertSingleLineLabels() {
        bottomNavDestinations.forEach { destination ->
            compose.onNodeWithText(destination.label, useUnmergedTree = true).assertIsDisplayed()
            val bounds = compose
                .onNodeWithText(destination.label, useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
            val height = bounds.bottom - bounds.top
            val width = bounds.right - bounds.left
            assertTrue(
                "\"${destination.label}\" wrapped onto $height (expected a single " +
                    "line under $oneLineMaxHeight)",
                height <= oneLineMaxHeight
            )
            assertTrue(
                "\"${destination.label}\" is $width wide and overflows its slot",
                width <= 64.dp
            )
        }
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun labelsStayOnOneLineOnNarrowPhone() {
        compose.setContent {
            NoctaBottomNavigation(
                currentRoute = NoctaDestination.Optimize.route,
                onNavigate = {}
            )
        }
        assertSingleLineLabels()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xhdpi")
    fun labelsStayOnOneLineOnNormalPhone() {
        compose.setContent {
            NoctaBottomNavigation(
                currentRoute = NoctaDestination.Home.route,
                onNavigate = {}
            )
        }
        assertSingleLineLabels()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun everyDestinationIsTappable() {
        var navigatedTo: String? = null
        compose.setContent {
            NoctaBottomNavigation(
                currentRoute = NoctaDestination.Home.route,
                onNavigate = { navigatedTo = it.route }
            )
        }
        compose.onNodeWithText("Insights").performClick()
        assertTrue("tapping Insights should navigate", navigatedTo == NoctaDestination.Insights.route)
    }
}
