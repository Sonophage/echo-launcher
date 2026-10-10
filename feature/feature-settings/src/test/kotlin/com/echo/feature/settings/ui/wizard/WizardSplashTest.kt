package com.echo.feature.settings.ui.wizard

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.echo.core.ui.theme.EchoTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// the welcome line sat one footer high, and the A orb stands higher than the footer, so the orb covered its last line
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w821dp-h462dp")
class WizardSplashTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the welcome line ends above the A orb`() {
        composeRule.setContent { EchoTheme { WizardSplash(onBegin = {}) } }
        composeRule.waitForIdle()

        val line = composeRule.onNodeWithText("Setup takes a few short steps", substring = true, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val orb = composeRule.onNode(SemanticsMatcher("the Get started orb") {
            it.config.getOrNull(SemanticsActions.OnClick)?.label == "Get started"
        }).fetchSemanticsNode().boundsInRoot

        assertTrue("the line ends at ${line.bottom}, the orb starts at ${orb.top}", line.bottom <= orb.top)
    }
}
