package com.echo.themekit

import kotlinx.serialization.json.Json

object CrossbarLayoutSpecCodec {
    const val BAR_TOP_MIN = 0.05f
    const val BAR_TOP_MAX = 0.45f

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(spec: CrossbarLayoutSpec): String =
        json.encodeToString(CrossbarLayoutSpec.serializer(), sanitize(spec))

    fun decode(encoded: String?): CrossbarLayoutSpec? {
        if (encoded.isNullOrBlank()) return null
        return runCatching { json.decodeFromString(CrossbarLayoutSpec.serializer(), encoded) }
            .getOrNull()
            ?.let(::sanitize)
    }

    fun sanitize(spec: CrossbarLayoutSpec): CrossbarLayoutSpec {
        val d = CrossbarLayoutSpec.DEFAULT
        return CrossbarLayoutSpec(
            barTopFraction = spec.barTopFraction.safe(d.barTopFraction, BAR_TOP_MIN, BAR_TOP_MAX),
            contentTopPaddingDp = spec.contentTopPaddingDp.safe(d.contentTopPaddingDp, 0f, 120f),
            categoryIconSelectedDp = spec.categoryIconSelectedDp.safe(d.categoryIconSelectedDp, 16f, 160f),
            categoryIconDp = spec.categoryIconDp.safe(d.categoryIconDp, 16f, 160f),
            itemIconDp = spec.itemIconDp.safe(d.itemIconDp, 16f, 160f),
            itemIconSlotDp = spec.itemIconSlotDp.safe(d.itemIconSlotDp, 16f, 160f),
            itemTextSelectedSp = spec.itemTextSelectedSp.safe(d.itemTextSelectedSp, 8f, 40f),
            itemTextSp = spec.itemTextSp.safe(d.itemTextSp, 8f, 40f),
            itemTextStartGapDp = spec.itemTextStartGapDp.safe(d.itemTextStartGapDp, 0f, 60f),
            leftAnchorExtraDp = spec.leftAnchorExtraDp.safe(d.leftAnchorExtraDp, -60f, 120f),
        )
    }

    private fun Float.safe(default: Float, min: Float, max: Float): Float =
        (if (isFinite()) this else default).coerceIn(min, max)
}
