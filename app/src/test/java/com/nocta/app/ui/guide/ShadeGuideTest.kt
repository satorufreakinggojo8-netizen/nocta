package com.nocta.app.ui.guide

import com.nocta.app.navigation.bottomNavDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Shade's tips are contextual: one short, real line per screen, rotated so the
 * same sentence is not repeated on every visit.
 */
class ShadeGuideTest {

    @Test
    fun `every tab except the chat itself has at least one tip`() {
        bottomNavDestinations
            .filter { it.route != "coach" }
            .forEach { destination ->
                assertTrue(
                    "no tip for ${destination.route}",
                    ShadeGuide.hasTips(destination.route)
                )
            }

        // The chat screen is the conversation — a tip about it would be noise.
        assertTrue(ShadeGuide.tipsFor("coach").isEmpty())
    }

    @Test
    fun `tips are short enough for a bubble`() {
        val routes = bottomNavDestinations.map { it.route } + ShadeGuide.ROUTE_JOURNAL
        routes.flatMap { ShadeGuide.tipsFor(it) }.forEach { tip ->
            assertTrue("${tip.id} message is too long", tip.message.length <= 90)
            assertTrue("${tip.id} has no detail fallback", tip.detail.isNotBlank())
            assertTrue("${tip.id} has no coach question", tip.coachPrompt.isNotBlank())
        }
    }

    @Test
    fun `re-entering a screen does not repeat the same tip`() {
        val rotator = ShadeTipRotator()
        val tips = ShadeGuide.tipsFor(ShadeGuide.ROUTE_HOME)

        assertEquals(tips[0].id, rotator.next(ShadeGuide.ROUTE_HOME)?.id)
        assertEquals(tips[1].id, rotator.next(ShadeGuide.ROUTE_HOME)?.id)
        // With two tips it cycles back, but never twice in a row.
        assertEquals(tips[0].id, rotator.next(ShadeGuide.ROUTE_HOME)?.id)
    }

    @Test
    fun `rotation is tracked per screen`() {
        val rotator = ShadeTipRotator()
        val homeTips = ShadeGuide.tipsFor(ShadeGuide.ROUTE_HOME)
        val sleepTips = ShadeGuide.tipsFor(ShadeGuide.ROUTE_SLEEP)

        assertEquals(homeTips[0].id, rotator.next(ShadeGuide.ROUTE_HOME)?.id)
        assertEquals(sleepTips[0].id, rotator.next(ShadeGuide.ROUTE_SLEEP)?.id)
        assertEquals(homeTips[1].id, rotator.next(ShadeGuide.ROUTE_HOME)?.id)
    }

    @Test
    fun `a screen with one tip repeats it rather than inventing another`() {
        val rotator = ShadeTipRotator()
        val tip = ShadeGuide.tipsFor(ShadeGuide.ROUTE_JOURNAL).single()
        assertEquals(tip.id, rotator.next(ShadeGuide.ROUTE_JOURNAL)?.id)
        assertEquals(tip.id, rotator.next(ShadeGuide.ROUTE_JOURNAL)?.id)
    }

    @Test
    fun `unknown routes and the log form have no tip`() {
        assertNull(ShadeTipRotator().next("does_not_exist"))
        assertTrue(ShadeGuide.tipsFor(null).isEmpty())
        assertTrue(ShadeGuide.tipsFor("log_sleep").isEmpty())
    }

    @Test
    fun `query arguments do not change the route used for tips`() {
        assertEquals("coach", ShadeGuide.baseRouteOf("coach?prompt=hello"))
        assertEquals("home", ShadeGuide.baseRouteOf("home"))
        assertNull(ShadeGuide.baseRouteOf(null))
    }

    @Test
    fun `the dashboard reserves more room than the compact screens`() {
        assertTrue(ShadeLayout.HomeBottomInset > ShadeLayout.CompactBottomInset)
        assertEquals(
            ShadeLayout.FullCompanionHeight,
            ShadeLayout.companionHeight(ShadeGuide.ROUTE_HOME)
        )
        assertEquals(
            ShadeLayout.CompactCompanionHeight,
            ShadeLayout.companionHeight(ShadeGuide.ROUTE_INSIGHTS)
        )
        // Bubble sits above the character, so the inset must be bigger than it.
        assertTrue(ShadeLayout.CompactBottomInset > ShadeLayout.CompactCompanionHeight)
        assertTrue(ShadeLayout.HomeBottomInset > ShadeLayout.FullCompanionHeight)
    }

    @Test
    fun `tip ids are unique`() {
        val routes = bottomNavDestinations.map { it.route } +
            listOf(
                ShadeGuide.ROUTE_JOURNAL,
                ShadeGuide.ROUTE_SLEEP_DETAILS,
                ShadeGuide.ROUTE_RECOMMENDATION,
                ShadeGuide.ROUTE_SLEEP_DEBT
            )
        val ids = routes.flatMap { ShadeGuide.tipsFor(it) }.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
        assertNotEquals(ids.first(), ids.last())
    }
}
