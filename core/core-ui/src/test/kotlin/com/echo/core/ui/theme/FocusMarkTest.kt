package com.echo.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.echo.themekit.FocusStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// owner, 2026-10-09: each focus style marks a focused item its own way. CLASSIC is the warm glow and draws no
// ring; HALO a ring; BRACKET four corners; LIFT a shadow under it. Any screen draws them from this one place
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class FocusMarkTest {
    @get:Rule
    val compose = createComposeRule()

    private val accent = Color(0xFFFF0000)

    private fun draw(modifier: Modifier): ImageBitmap {
        // on black, so "draws nothing" is a black pixel
        compose.setContent { Box(Modifier.size(100.dp).background(Color.Black).testTag("mark").then(modifier)) }
        return compose.onNodeWithTag("mark").captureToImage()
    }

    private fun ImageBitmap.at(fx: Float, fy: Float): Color =
        toPixelMap()[((width - 1) * fx).toInt(), ((height - 1) * fy).toInt()]

    private fun Color.isAccent() = red > 0.8f && green < 0.2f && blue < 0.2f && alpha > 0.8f

    @Test
    fun `bracket marks the four corners and leaves the middle clear`() {
        val image = draw(Modifier.focusMark(FocusStyle.BRACKET, 1f, accent))
        listOf(0f to 0f, 1f to 0f, 0f to 1f, 1f to 1f).forEach { (x, y) ->
            assertTrue("corner $x,$y", image.at(x, y).isAccent())
        }
        assertEquals(Color.Black, image.at(0.5f, 0.5f))
    }

    @Test
    fun `halo rings the item and leaves its corners clear`() {
        val image = draw(Modifier.focusMark(FocusStyle.HALO, 1f, accent))
        assertTrue("top of the ring", (0..4).any { image.at(0.5f, 0.01f + it * 0.002f).isAccent() })
        assertFalse(image.at(0f, 0f).isAccent())
    }

    @Test
    fun `classic draws no mark, its glow is the warm light`() {
        assertEquals(Color.Black, draw(Modifier.focusMark(FocusStyle.CLASSIC, 1f, accent)).at(0.5f, 0.5f))
        assertTrue(FocusStyle.CLASSIC.glows)
        FocusStyle.entries.filter { it != FocusStyle.CLASSIC }.forEach { assertFalse("$it glows", it.glows) }
    }

    @Test
    fun `the glow is warm and brightest at the centre`() {
        val image = draw(Modifier.focusGlow(visible = 1f, reach = FocusGlow.RowReach, alpha = 1f))
        val centre = image.at(0.5f, 0.5f)
        assertTrue("warm: $centre", centre.red > 0.9f && centre.red > centre.blue)
        assertTrue(centre.red > image.at(0.9f, 0.5f).red)
    }

    @Test
    fun `a hidden mark draws nothing`() {
        val image = draw(Modifier.focusMark(FocusStyle.BRACKET, 0f, accent))
        assertEquals(Color.Black, image.at(0f, 0f))
    }
}
