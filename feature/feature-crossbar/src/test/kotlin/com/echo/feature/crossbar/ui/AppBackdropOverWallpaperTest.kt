package com.echo.feature.crossbar.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

// owner, 2026-10-09: on the crossbar a focused app's backdrop lets the wallpaper show, tinted by the crossbar's
// scrim, with the icon faint in the corner; it was a solid gradient over the wallpaper
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w821dp-h462dp")
class AppBackdropOverWallpaperTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the wallpaper shows through an app's backdrop on the crossbar`() {
        var paper by mutableStateOf(Color.Red)
        composeRule.setContent {
            Box(Modifier.fillMaxSize().background(paper)) {
                CrossbarAppIconBackdrop("com.test.none", fallbackAccent = Color.Blue, overWallpaper = true)
            }
        }
        composeRule.waitForIdle()
        val overRed = composeRule.onRoot().captureToImage().toPixelMap()
        paper = Color.Green
        composeRule.waitForIdle()
        val overGreen = composeRule.onRoot().captureToImage().toPixelMap()
        val a = overRed[overRed.width / 2, overRed.height / 2]
        val b = overGreen[overGreen.width / 2, overGreen.height / 2]
        assertTrue("the middle changes with the wallpaper", abs(a.red - b.red) + abs(a.green - b.green) > 0.5f)
    }
}
