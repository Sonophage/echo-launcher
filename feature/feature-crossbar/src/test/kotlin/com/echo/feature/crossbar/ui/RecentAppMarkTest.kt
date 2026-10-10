package com.echo.feature.crossbar.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.echo.core.ui.preview.EchoScreenPreview
import com.echo.feature.crossbar.viewmodel.CrossbarItem
import com.echo.feature.crossbar.viewmodel.RecentFilter
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

// owner, 2026-10-09: an app on Recent shows its icon again, large and faint in the bottom corner, as on its App
// Drawer case
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w821dp-h462dp")
class RecentAppMarkTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    // sampled pixels low in the right corner that differ from the page's own fill in the same row, taken from a column
    // left of the mark and right of the words, so the page's top-to-bottom shading cancels out
    private fun markedPixels(page: PixelMap): Int {
        val reference = page.width * 62 / 100
        var marked = 0
        for (y in page.height * 85 / 100 until page.height step 2) {
            val fill = page[reference, y]
            for (x in page.width * 80 / 100 until page.width step 2) {
                val p = page[x, y]
                if (abs(p.red - fill.red) + abs(p.green - fill.green) + abs(p.blue - fill.blue) > 0.02f) marked++
            }
        }
        return marked
    }

    private fun assertAppMarked(railVisible: Boolean) {
        val item = CrossbarItem(id = "app", title = "Calendar", packageName = composeRule.activity.packageName)
        composeRule.setContent {
            EchoScreenPreview {
                LastPlayedPage(
                    items = listOf(item), selectedIndex = 0, listState = rememberLazyListState(),
                    filter = RecentFilter.ALL, railVisible = railVisible, onCardTapped = {},
                )
            }
        }
        composeRule.waitForIdle()
        val marked = markedPixels(composeRule.onRoot().captureToImage().toPixelMap())
        assertTrue("sampled corner pixels where the app's faint icon shows: $marked", marked > 100)
    }

    @Test
    fun `an app on Recent has its faint icon in the corner`() = assertAppMarked(railVisible = false)

    @Test
    fun `an app on Recent's rail view has its faint icon in the corner`() = assertAppMarked(railVisible = true)
}
