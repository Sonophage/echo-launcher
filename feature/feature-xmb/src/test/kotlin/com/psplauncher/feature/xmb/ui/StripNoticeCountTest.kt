package com.psplauncher.feature.xmb.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.psplauncher.core.ui.preview.PfpScreenPreview
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w821dp-h462dp")
class StripNoticeCountTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun strip(
        noticeCount: Int,
        live: StripLiveActivity? = null,
        onCountTapped: () -> Unit = {},
    ) {
        composeRule.setContent {
            PfpScreenPreview {
                XmbPspStatusStrip(
                    live = live,
                    noticeCount = noticeCount,
                    onNoticeCountTapped = onCountTapped,
                )
            }
        }
    }

    @Test
    fun `the count sits in the right-hand cluster, so the island is free for media and games`() {
        strip(noticeCount = 3, live = StripLiveActivity(art = null, title = "Some Song", detail = "An Artist"))

        composeRule.onNodeWithContentDescription("3 notifications").assertIsDisplayed()

        val countRight = composeRule.onNodeWithContentDescription("3 notifications")
            .fetchSemanticsNode().boundsInRoot.left
        val islandRight = composeRule.onNodeWithText("Some Song")
            .fetchSemanticsNode().boundsInRoot.right

        assertEquals(
            "the notification count must draw to the right of the island, not inside it",
            true,
            countRight > islandRight,
        )
    }

    @Test
    fun `no notifications draws no counter at all, rather than a zero`() {
        strip(noticeCount = 0)
        composeRule.onNodeWithText("0").assertDoesNotExist()
    }

    @Test
    fun `tapping the count is what opens the notification bar`() {
        var taps = 0
        strip(noticeCount = 2, onCountTapped = { taps++ })

        composeRule.onNodeWithContentDescription("2 notifications").performClick()

        assertEquals("the count is the tap target for the bar", 1, taps)
    }
}
