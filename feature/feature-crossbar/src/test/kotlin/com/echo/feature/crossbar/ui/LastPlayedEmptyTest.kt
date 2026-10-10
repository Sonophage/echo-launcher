package com.echo.feature.crossbar.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.echo.core.ui.preview.EchoScreenPreview
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// owner, 2026-10-09: Last Played with nothing played showed a bare crossbar; it now says so and offers the way to
// something to play
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w821dp-h462dp")
class LastPlayedEmptyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the prompt's buttons go to the Game column and the App Drawer`() {
        val went = mutableListOf<String>()
        composeRule.setContent { EchoScreenPreview { LastPlayedEmpty(onGames = { went += "games" }, onAppDrawer = { went += "drawer" }) } }

        composeRule.onNodeWithText("Start something.").assertExists()
        composeRule.onNodeWithText("Games", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("App Drawer", useUnmergedTree = true).performClick()

        assertEquals(listOf("games", "drawer"), went)
    }

    @Test
    fun `with no Game column there is no Games button`() {
        composeRule.setContent { EchoScreenPreview { LastPlayedEmpty(onGames = null, onAppDrawer = {}) } }
        composeRule.onNodeWithText("Games", useUnmergedTree = true).assertDoesNotExist()
    }
}
