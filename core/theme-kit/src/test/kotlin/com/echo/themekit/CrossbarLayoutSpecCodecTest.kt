package com.echo.themekit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CrossbarLayoutSpecCodecTest {

    @Test
    fun `round-trips a customized spec`() {
        val spec = CrossbarLayoutSpec(barTopFraction = 0.22f, itemTextSp = 16f)
        assertEquals(spec, CrossbarLayoutSpecCodec.decode(CrossbarLayoutSpecCodec.encode(spec)))
    }

    @Test
    fun `default spec round-trips unchanged`() {
        assertEquals(
            CrossbarLayoutSpec.DEFAULT,
            CrossbarLayoutSpecCodec.decode(CrossbarLayoutSpecCodec.encode(CrossbarLayoutSpec.DEFAULT)),
        )
    }

    @Test
    fun `malformed input decodes to null`() {
        assertNull(CrossbarLayoutSpecCodec.decode(null))
        assertNull(CrossbarLayoutSpecCodec.decode(""))
        assertNull(CrossbarLayoutSpecCodec.decode("not json"))
        assertNull(CrossbarLayoutSpecCodec.decode("""{"barTopFraction":"a string"}"""))
    }

    @Test
    fun `unknown keys are ignored and missing keys default`() {
        val decoded = CrossbarLayoutSpecCodec.decode("""{"barTopFraction":0.2,"futureField":true}""")
        assertEquals(0.2f, decoded?.barTopFraction)
        assertEquals(CrossbarLayoutSpec.DEFAULT.itemIconDp, decoded?.itemIconDp)
    }

    @Test
    fun `hostile values clamp into safe ranges`() {
        val hostile = CrossbarLayoutSpec(
            barTopFraction = 9f,
            contentTopPaddingDp = -50f,
            categoryIconSelectedDp = 100_000f,
            itemTextSp = 0f,
            leftAnchorExtraDp = -999f,
        )
        val safe = CrossbarLayoutSpecCodec.sanitize(hostile)
        assertEquals(CrossbarLayoutSpecCodec.BAR_TOP_MAX, safe.barTopFraction)
        assertEquals(0f, safe.contentTopPaddingDp)
        assertEquals(160f, safe.categoryIconSelectedDp)
        assertEquals(8f, safe.itemTextSp)
        assertEquals(-60f, safe.leftAnchorExtraDp)
    }

    @Test
    fun `NaN and infinity fall back to the field default before clamping`() {
        val safe = CrossbarLayoutSpecCodec.sanitize(
            CrossbarLayoutSpec(barTopFraction = Float.NaN, itemIconDp = Float.POSITIVE_INFINITY),
        )
        assertEquals(CrossbarLayoutSpec.DEFAULT.barTopFraction, safe.barTopFraction)
        assertEquals(CrossbarLayoutSpec.DEFAULT.itemIconDp, safe.itemIconDp)
    }

    @Test
    fun `decode clamps hostile json values`() {
        val decoded = CrossbarLayoutSpecCodec.decode("""{"barTopFraction":0.9}""")
        assertEquals(CrossbarLayoutSpecCodec.BAR_TOP_MAX, decoded?.barTopFraction)
    }

    @Test
    fun `a layout saved before the previous-item peek was removed still loads`() {
        val saved = CrossbarLayoutSpecCodec.encode(CrossbarLayoutSpec(itemTextSp = 21f)).dropLast(1) + ",\"previousItemRiseRows\":0.5}"
        assertEquals(21f, CrossbarLayoutSpecCodec.decode(saved)?.itemTextSp)
    }
}
