package com.echo.feature.crossbar.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import com.echo.core.ui.preview.EchoScreenPreview
import com.echo.feature.crossbar.viewmodel.CrossbarItem
import com.echo.feature.crossbar.viewmodel.SearchScope
import com.echo.feature.crossbar.viewmodel.SearchState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// owner, 2026-10-05: the search banner had no Open or Options buttons, unlike the App Drawer's
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1067dp-h668dp")
class SearchBannerButtonsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the banner's Open and Options act on the highlighted result`() {
        val opened = mutableListOf<Int>()
        val options = mutableListOf<Int>()
        composeRule.setContent {
            EchoScreenPreview {
                SearchScreen(
                    state = SearchState(
                        scope = SearchScope.entries.first(),
                        query = "d",
                        rows = listOf(CrossbarItem(id = "a", title = "Alpha Video"), CrossbarItem(id = "b", title = "Dune")),
                        selectedIndex = 1,
                        loaded = true,
                    ),
                    onQueryChange = {},
                    onActivateAt = { opened += it },
                    onBack = {},
                    onOptionsAt = { options += it },
                )
            }
        }

        // the banner's buttons come before the footer's hints of the same name
        composeRule.onAllNodesWithText("Options").onFirst().performClick()
        composeRule.onAllNodesWithText("Open").onFirst().performClick()

        assertEquals(listOf(1), options)
        assertEquals(listOf(1), opened)
    }
}
