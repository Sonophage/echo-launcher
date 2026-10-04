package com.echo.core.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.preview.EchoScreenPreview
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w480dp-h640dp")
class EchoHintBarCentreTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the centre is drawn between the prompts`() {
        composeRule.setContent {
            EchoScreenPreview {
                EchoHintBar(
                    items = listOf(
                        ControllerPromptItem(GamepadAction.BACK, "Back"),
                        ControllerPromptItem(GamepadAction.SELECT, "Enter"),
                    ),
                    centre = { Text("What this row does") },
                )
            }
        }
        composeRule.onNodeWithText("What this row does").assertIsDisplayed()

        val back = composeRule.onNodeWithText("Back").fetchSemanticsNode().positionInRoot.x
        val help = composeRule.onNodeWithText("What this row does").fetchSemanticsNode().positionInRoot.x
        assertTrue("the centre ($help) must sit right of Back ($back)", help > back)
    }

    @Test
    fun `a bar with no prompts still draws its centre`() {
        composeRule.setContent {
            EchoScreenPreview {
                EchoHintBar(items = emptyList(), centre = { Text("Still here") })
            }
        }
        composeRule.onNodeWithText("Still here").assertIsDisplayed()
    }

    @Test
    fun `a bar with neither prompts nor a centre draws nothing`() {
        composeRule.setContent {
            EchoScreenPreview { EchoHintBar(items = emptyList()) }
        }

        composeRule.onNodeWithText("Back").assertDoesNotExist()
    }
}
