package com.echo.core.ui.design

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.echo.core.ui.theme.EchoTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// the spine's "VHS" tab showed the top of "VH" on the Konker: the theme's body line height (24 sp) is taller than
// the tab, so the word sat low and was cut off
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1027dp-h577dp-374dpi")
class VhsTabFitTest {
    @get:Rule
    val compose = createComposeRule()

    private fun assertTabFits(scale: Float) {
        compose.setContent {
            EchoTheme { VhsCase("App", Color.Blue, DesignUnits(scale, LocalDensity.current), Modifier.size(220.dp, 300.dp)) {} }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("VHS").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val tab = layouts.single()
        val room = tab.layoutInput.constraints
        val need = tab.multiParagraph
        assertTrue(
            "\"VHS\" needs ${need.maxIntrinsicWidth} x ${need.height} px in ${room.maxWidth} x ${room.maxHeight} at scale $scale",
            need.maxIntrinsicWidth <= room.maxWidth && need.height <= room.maxHeight,
        )
    }

    @Test
    fun `the VHS tab fits the spine at the Konker's scale`() = assertTabFits(0.8f)

    @Test
    fun `the VHS tab fits the spine at a small handheld's scale`() = assertTabFits(0.5f)

    @Test
    fun `the VHS tab fits the spine at full scale`() = assertTabFits(1f)
}
