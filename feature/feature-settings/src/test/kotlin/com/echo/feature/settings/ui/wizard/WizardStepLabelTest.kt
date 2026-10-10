package com.echo.feature.settings.ui.wizard

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.echo.core.ui.theme.EchoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// Setup showed "Setup · Step 1 of 5" over its heading and again over the side pane's hint
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w821dp-h462dp")
class WizardStepLabelTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the step is named once`() {
        composeRule.setContent {
            EchoTheme {
                WizardScaffold(stepNumber = 1, stepCount = 5, title = "Setup", heading = "What is ECHO for?",
                    hint = "Setup only asks about what you turn on.", onBack = {}) {}
            }
        }
        composeRule.waitForIdle()
        // the pane is drawn: its hint is on screen, so a missing second label is not a missing pane
        composeRule.onNodeWithText("Setup only asks about what you turn on.").assertExists()

        val labels = composeRule.onAllNodesWithText("Step 1 of 5", substring = true, ignoreCase = true, useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertEquals("nodes naming the step", 1, labels.size)
    }
}
