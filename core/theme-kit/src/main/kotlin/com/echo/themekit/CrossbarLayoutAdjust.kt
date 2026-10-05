package com.echo.themekit

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Serializable
data class CrossbarLayoutAdjust(
    val scale: Float = 1f,
    val barLeftFraction: Float = 0f,
    val barTopFraction: Float = CrossbarLayoutSpec.DEFAULT.barTopFraction,
    // owner, 2026-10-05: the top bar and the footer are sized on their own, per device
    val headerScale: Float = 1f,
    val footerScale: Float = 1f,
) {
    companion object {
        val DEFAULT = CrossbarLayoutAdjust()

        const val SCALE_MIN = 0.6f
        const val SCALE_MAX = 1.8f
        const val CHROME_MIN = 0.75f
        const val CHROME_MAX = 1.5f
        const val LEFT_MIN = -0.25f
        const val LEFT_MAX = 0.35f

        const val TOP_MIN = CrossbarLayoutSpecCodec.BAR_TOP_MIN
        const val TOP_MAX = CrossbarLayoutSpecCodec.BAR_TOP_MAX
    }
}

enum class CrossbarFormFactor(val key: String) {
    COMPACT("compact"),
    MEDIUM("medium"),
    EXPANDED("expanded");

    companion object {
        fun forSmallestWidthDp(swDp: Int): CrossbarFormFactor = when {
            swDp < 600 -> COMPACT
            swDp < 840 -> MEDIUM
            else -> EXPANDED
        }
    }
}

object CrossbarLayoutAdjustCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val mapSerializer = MapSerializer(String.serializer(), CrossbarLayoutAdjust.serializer())

    fun encode(map: Map<String, CrossbarLayoutAdjust>): String =
        json.encodeToString(mapSerializer, map.mapValues { sanitize(it.value) })

    fun decode(encoded: String?): Map<String, CrossbarLayoutAdjust> {
        if (encoded.isNullOrBlank()) return emptyMap()
        return runCatching { json.decodeFromString(mapSerializer, encoded) }
            .getOrNull()
            ?.mapValues { sanitize(it.value) }
            ?: emptyMap()
    }

    fun sanitize(a: CrossbarLayoutAdjust): CrossbarLayoutAdjust = CrossbarLayoutAdjust(
        scale = a.scale.safe(CrossbarLayoutAdjust.DEFAULT.scale).coerceIn(CrossbarLayoutAdjust.SCALE_MIN, CrossbarLayoutAdjust.SCALE_MAX),
        barLeftFraction = a.barLeftFraction.safe(0f).coerceIn(CrossbarLayoutAdjust.LEFT_MIN, CrossbarLayoutAdjust.LEFT_MAX),
        barTopFraction = a.barTopFraction.safe(CrossbarLayoutAdjust.DEFAULT.barTopFraction)
            .coerceIn(CrossbarLayoutAdjust.TOP_MIN, CrossbarLayoutAdjust.TOP_MAX),
        headerScale = a.headerScale.safe(1f).coerceIn(CrossbarLayoutAdjust.CHROME_MIN, CrossbarLayoutAdjust.CHROME_MAX),
        footerScale = a.footerScale.safe(1f).coerceIn(CrossbarLayoutAdjust.CHROME_MIN, CrossbarLayoutAdjust.CHROME_MAX),
    )

    private fun Float.safe(fallback: Float): Float = if (isNaN() || isInfinite()) fallback else this
}
