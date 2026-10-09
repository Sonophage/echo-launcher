package com.echo.feature.settings.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.echo.core.ui.theme.EchoTheme
import com.echo.feature.settings.viewmodel.ThemePage
import com.echo.themekit.ThemePart
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// owner, 2026-10-09: a theme page lists ten parts, more than a handheld's screen holds. The rail follows the
// cursor, so the part being ticked is always on screen
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp")
class ThemePageRailTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the rail scrolls to the focused part, down to the last one`() {
        val last = ThemePart.entries.last()
        val page = ThemePage(name = "Every Part", parts = ThemePart.entries.toSet(), cursor = ThemePart.entries.size)
        composeRule.setContent {
            EchoTheme { ThemePageOverlay(page = page, shot = 0, partSources = emptyMap(), onRow = {}, onBack = {}, onShot = {}) }
        }
        composeRule.onNodeWithText(last.label).assertIsDisplayed()
    }
}
