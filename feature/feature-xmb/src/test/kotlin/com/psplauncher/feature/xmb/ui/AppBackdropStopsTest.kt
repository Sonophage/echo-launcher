package com.psplauncher.feature.xmb.ui

import androidx.compose.ui.graphics.Color
import com.psplauncher.core.ui.theme.contrastRatio
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The app backdrop replaced a 24px icon upscaled to fill the screen. It is now
 * a gradient mixed from the app's own icon colour, and it is the only scrim
 * under that backdrop -- XMBShell skips its own for the AppIcon case. So this
 * gradient alone has to keep the XMB's white text readable.
 *
 * AccentDeriver forces its result to saturation >= 0.55 and value >= 0.85, so
 * the worst case it can hand us is the brightest, least-saturated end of that
 * range. That is what is swept here, not a comfortable fixture.
 */
class AppBackdropStopsTest {
    private val whiteOnDarkMinimum = 4.5

    private fun maxChannelDistance(a: Color, b: Color): Float =
        maxOf(
            kotlin.math.abs(a.red - b.red),
            kotlin.math.abs(a.green - b.green),
            kotlin.math.abs(a.blue - b.blue),
        )

    @Test
    fun `every stop keeps white text readable for any accent the deriver can emit`() {
        for (degrees in 0 until 360 step 15) {
            val accent = Color.hsv(degrees.toFloat(), 0.55f, 1f)
            appBackdropStops(accent).forEachIndexed { index, stop ->
                val ratio = contrastRatio(Color.White, stop)
                assertTrue(
                    "hue $degrees stop $index is too bright for white text: $ratio",
                    ratio >= whiteOnDarkMinimum,
                )
            }
        }
    }

    @Test
    fun `white is not the accent the deriver emits but the gradient must survive it anyway`() {
        appBackdropStops(Color.White).forEach { stop ->
            assertTrue(
                "a white icon must not wash the backdrop out",
                contrastRatio(Color.White, stop) >= whiteOnDarkMinimum,
            )
        }
    }

    @Test
    fun `the gradient carries the app's colour rather than collapsing to the base`() {
        for (degrees in 0 until 360 step 15) {
            val accent = Color.hsv(degrees.toFloat(), 0.9f, 1f)
            val stops = appBackdropStops(accent)
            val reach = maxChannelDistance(stops.first(), AppBackdropBase)
            val span = maxChannelDistance(accent, AppBackdropBase)

            assertTrue(
                "hue $degrees barely moves off the base: $reach of $span",
                reach >= 0.2f * span,
            )
        }
    }

    @Test
    fun `the gradient darkens toward its end so the colour reads as a wash`() {
        val stops = appBackdropStops(Color.hsv(210f, 0.9f, 1f))
        assertTrue(
            "the last stop must be the base itself",
            stops.last() == AppBackdropBase,
        )
        assertTrue(
            "the gradient must fall from coloured to base, not rise",
            contrastRatio(Color.White, stops.first()) < contrastRatio(Color.White, stops.last()),
        )
    }
}
