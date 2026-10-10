package com.echo.feature.crossbar.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import com.echo.core.ui.design.APP_WATERMARK_TAG
import com.echo.core.ui.preview.EchoScreenPreview
import com.echo.feature.crossbar.viewmodel.CrossbarItem
import com.echo.feature.crossbar.viewmodel.RecentFilter
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

// owner, 2026-10-09: on Recent an app with no art shows the wallpaper tinted the app's colour, with the app's icon
// large and faint in the bottom left corner, as the crossbar and the App Drawer do
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w821dp-h462dp")
class RecentAppMarkTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var paper by mutableStateOf(Color.Red)

    private fun showApp(railVisible: Boolean) {
        val pm = shadowOf(composeRule.activity.packageManager)
        pm.installPackage(PackageInfo().apply {
            packageName = "com.test.calendar"
            applicationInfo = ApplicationInfo().apply { packageName = "com.test.calendar" }
        })
        pm.setApplicationIcon("com.test.calendar", ColorDrawable(android.graphics.Color.BLUE))
        val item = CrossbarItem(id = "app", title = "Calendar", packageName = "com.test.calendar")
        composeRule.setContent {
            CompositionLocalProvider(LocalRecentWallpaper provides { Box(Modifier.fillMaxSize().background(paper)) }) {
                EchoScreenPreview {
                    LastPlayedPage(
                        items = listOf(item), selectedIndex = 0, listState = rememberLazyListState(),
                        filter = RecentFilter.ALL, railVisible = railVisible, onCardTapped = {},
                    )
                }
            }
        }
        // the icon loads off the main thread
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(APP_WATERMARK_TAG, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun assertMarkBottomLeft() {
        val mark = composeRule.onAllNodesWithTag(APP_WATERMARK_TAG, useUnmergedTree = true).fetchSemanticsNodes().single().boundsInRoot
        val screen = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("the icon's middle is left of the screen's: ${mark.center.x}", mark.center.x < screen.width / 2)
        assertTrue("and low: ${mark.center.y}", mark.center.y > screen.height / 2)
    }

    @Test
    fun `an app on Recent has its faint icon bottom left, and the wallpaper shows`() {
        showApp(railVisible = false)
        assertMarkBottomLeft()
        val overRed = composeRule.onRoot().captureToImage().toPixelMap()
        paper = Color.Green
        composeRule.waitForIdle()
        val overGreen = composeRule.onRoot().captureToImage().toPixelMap()
        val x = overRed.width * 3 / 4
        val y = overRed.height / 4
        val change = abs(overRed[x, y].red - overGreen[x, y].red) + abs(overRed[x, y].green - overGreen[x, y].green)
        assertTrue("the wallpaper shows through behind the app: $change", change > 0.1f)
    }

    @Test
    fun `on Recent's rail view too`() {
        showApp(railVisible = true)
        assertMarkBottomLeft()
    }
}
