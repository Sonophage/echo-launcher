package com.echo.core.ui.theme

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.ui.test.junit4.createComposeRule
import com.echo.themekit.FocusStyle
import com.echo.themekit.MotionPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// spec #3: whatever ECHO draws inside its theme presses with the press mark, in the theme's focus style and
// motion, and no Material ripple. Every window (both screens, the Reader) is drawn inside EchoTheme
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EchoThemeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `inside the theme a press is the press mark and Material draws no ripple`() {
        var indication: Indication? = null
        var ripple: RippleConfiguration? = RippleConfiguration()
        compose.setContent {
            EchoTheme {
                indication = LocalIndication.current
                ripple = LocalRippleConfiguration.current
            }
        }
        assertSame(PressMark, indication)
        assertNull(ripple)
    }

    @Test
    fun `the theme presses in the focus style and motion it is given, and a theme inside it keeps them`() {
        var outer: Pair<FocusStyle, MotionPreset>? = null
        var inner: Pair<FocusStyle, MotionPreset>? = null
        compose.setContent {
            EchoTheme(focusStyle = FocusStyle.HALO, motion = MotionPreset.SOFT) {
                outer = LocalFocusStyle.current to LocalMotion.current
                EchoTheme { inner = LocalFocusStyle.current to LocalMotion.current }
            }
        }
        assertEquals(FocusStyle.HALO to MotionPreset.SOFT, outer)
        assertEquals(FocusStyle.HALO to MotionPreset.SOFT, inner)
    }
}
