package com.echo.feature.settings.ui.wizard

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.echo.core.ui.design.LocalPanelTextScale
import com.echo.core.ui.theme.EchoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// a theme's text size reached the crossbar and stopped there: Settings and Setup kept ECHO's sizes whatever the
// layout said. They now size their words by LocalPanelTextScale, which the crossbar provides from the theme
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w821dp-h462dp")
class ThemeTextScaleTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `a theme's larger text reaches the setup pages`() {
        var scale by mutableFloatStateOf(1f)
        composeRule.setContent {
            CompositionLocalProvider(LocalPanelTextScale provides scale) {
                EchoTheme {
                    WizardScaffold(stepNumber = 1, stepCount = 5, title = "Setup", heading = "What is ECHO for?",
                        hint = "Pick", onBack = {}) {}
                }
            }
        }
        fun hintHeight() = composeRule.onNodeWithText("Pick", useUnmergedTree = true).fetchSemanticsNode().size.height.toFloat()
        composeRule.waitForIdle()
        val plain = hintHeight()
        scale = 1.5f
        composeRule.waitForIdle()
        assertEquals("the pane's words at 1.5 against 1", 1.5f, hintHeight() / plain, 0.08f)
    }
}
