package com.echo.feature.appbar.appdrawer

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.echo.core.ui.design.DesignUnits
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

// owner, 2026-10-09: the drawer's background is the app's colour as a tint over the wallpaper, which still shows;
// it was a solid room that hid the wallpaper
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w821dp-h462dp")
class WallBackdropTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the wallpaper shows through the drawer's tint`() {
        var paper by mutableStateOf(Color.Red)
        composeRule.setContent {
            WallBackdrop(null, null, DesignUnits(1f, LocalDensity.current), wallpaper = { Box(Modifier.fillMaxSize().background(paper)) })
        }
        composeRule.waitForIdle()
        val overRed = composeRule.onRoot().captureToImage().toPixelMap()
        paper = Color.Green
        composeRule.waitForIdle()
        val overGreen = composeRule.onRoot().captureToImage().toPixelMap()
        val a = overRed[overRed.width / 2, overRed.height / 2]
        val b = overGreen[overGreen.width / 2, overGreen.height / 2]
        val change = abs(a.red - b.red) + abs(a.green - b.green)
        assertTrue("the middle of the screen changes with the wallpaper by $change", change > 0.2f)
    }
}
