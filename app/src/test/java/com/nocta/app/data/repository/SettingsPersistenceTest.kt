package com.nocta.app.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The guidance mute is a user preference, not session state: muting must still
 * be in effect after the app restarts, and the same switch must turn it back on.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsPersistenceTest {

    @Test
    fun `tips default to on and muting survives a restart`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val firstRun = SettingsRepositoryImpl(context)
        assertTrue("tips start enabled", firstRun.getShadeGuideEnabled())
        assertTrue(firstRun.observeShadeGuideEnabled().first())

        firstRun.setShadeGuideEnabled(false)

        // A new instance re-reads SharedPreferences, exactly like a restart.
        val secondRun = SettingsRepositoryImpl(context)
        assertFalse("mute must survive a restart", secondRun.getShadeGuideEnabled())
        assertFalse(secondRun.observeShadeGuideEnabled().first())

        secondRun.setShadeGuideEnabled(true)
        assertTrue(SettingsRepositoryImpl(context).getShadeGuideEnabled())
    }
}
