package com.echo.feature.crossbar.ui.apppicker

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
import com.echo.core.ui.preview.EchoScreenPreview
import com.echo.feature.crossbar.viewmodel.AppPickerEntry
import com.echo.feature.crossbar.viewmodel.AppPickerState
import com.echo.feature.crossbar.viewmodel.AppPickerTarget
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// the picker opens over the crossbar; on the Konker the crossbar's icons and rows showed through between its tiles
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w640dp-h360dp")
class AppPickerBackdropTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    // an opaque screen is the same over any background: draw it over red and over green, and compare
    @Test
    fun `nothing behind the picker shows through it`() {
        var behind by mutableStateOf(Color.Red)
        composeRule.setContent {
            EchoScreenPreview {
                Box(Modifier.fillMaxSize().background(behind)) {
                    AppPickerScreen(
                        state = state,
                        onTileTapped = {}, onTouchBrowse = {}, onHeaderBack = {},
                        onSearchToggle = {}, onSearchChange = {}, onSearchDone = {},
                        onApply = {}, onConfirmRemoval = {}, onCancelRemoval = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val overRed = composeRule.onRoot().captureToImage().toPixelMap()
        behind = Color.Green
        composeRule.waitForIdle()
        val overGreen = composeRule.onRoot().captureToImage().toPixelMap()

        var differ = 0
        for (x in 0 until overRed.width step 4) for (y in 0 until overRed.height step 4) {
            val a = overRed[x, y]
            val b = overGreen[x, y]
            if (abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue) > 0.03f) differ++
        }
        assertEquals("sampled pixels that change with what is behind the picker", 0, differ)
    }

    private val state = AppPickerState(
        title = "Add Apps",
        target = AppPickerTarget.AndroidGames(platformId = "android"),
        apps = (1..6).map { AppPickerEntry(packageName = "com.test.app$it", label = "App $it") },
    )
}
