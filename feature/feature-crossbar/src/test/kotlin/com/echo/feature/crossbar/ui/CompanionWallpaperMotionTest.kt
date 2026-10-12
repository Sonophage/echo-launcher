package com.echo.feature.crossbar.ui

import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.echo.core.ui.motion.MOTION_WALLPAPER_TAG
import com.echo.feature.crossbar.viewmodel.CrossbarUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// owner, 2026-10-12: the Companion Screen is a display of its own with nothing else moving, so an app's Details
// there play the motion wallpaper as the Main Screen does. Recent and the App Drawer sit over a Crossbar that
// already plays it, so they keep the still copy
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CompanionWallpaperMotionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val ui = CrossbarUiState(
        customWallpaperPath = "/data/wallpaper_1.jpg",
        motionWallpaperPath = "/data/wallpaper_1.gif",
    )

    private fun motionSurfaces() =
        composeRule.onAllNodesWithTag(MOTION_WALLPAPER_TAG, useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun `the Companion Screen's wallpaper plays the motion wallpaper`() {
        composeRule.setContent { PlayingWallpaper(ui) }
        assertEquals("the Companion plays the motion", 1, motionSurfaces())
    }

    @Test
    fun `Recent and the App Drawer keep the still copy`() {
        composeRule.setContent { StillWallpaper(ui.customWallpaperPath, ui.wallpaperAccent) }
        assertEquals("the still copy never plays", 0, motionSurfaces())
    }

    @Test
    fun `the Companion Screen's wallpaper holds still under Battery Saver, as the Main Screen's does`() {
        val pm = composeRule.activity.getSystemService(PowerManager::class.java)
        shadowOf(pm).setIsPowerSaveMode(true)
        composeRule.setContent { PlayingWallpaper(ui.copy(respectBatterySaver = true)) }
        assertEquals("Battery Saver shows the poster", 0, motionSurfaces())
    }
}
