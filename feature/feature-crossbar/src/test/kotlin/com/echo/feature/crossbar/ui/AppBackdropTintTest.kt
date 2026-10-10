package com.echo.feature.crossbar.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.echo.core.ui.theme.contrastRatio
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// owner, 2026-10-10: an app's tint over the wallpaper, on Recent and the crossbar, was too faint to read as the
// app's colour
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w821dp-h462dp")
class AppBackdropTintTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun middleOver(paper: Color, accent: Color): Color {
        composeRule.setContent {
            Box(Modifier.fillMaxSize().background(paper)) {
                CrossbarAppIconBackdrop("com.test.none", fallbackAccent = accent, overWallpaper = true, scrim = true)
            }
        }
        composeRule.waitForIdle()
        val pixels = composeRule.onRoot().captureToImage().toPixelMap()
        return pixels[pixels.width / 2, pixels.height / 2]
    }

    @Test
    fun `an app's colour reads over a dark wallpaper`() {
        val middle = middleOver(Color.Black, Color.Blue)
        assertTrue("the middle's blue is ${middle.blue}", middle.blue > 0.2f)
    }

    @Test
    fun `white text stays readable over a mid grey wallpaper with the brightest accent the deriver emits`() {
        val middle = middleOver(Color(0.5f, 0.5f, 0.5f), Color.hsv(60f, 0.55f, 1f))
        val ratio = contrastRatio(Color.White, middle)
        assertTrue("white on the middle is $ratio:1", ratio >= 3.0)
    }
}
