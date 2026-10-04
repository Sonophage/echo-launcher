package com.echo.themekit

import kotlinx.serialization.Serializable

@Serializable
data class CrossbarLayoutSpec(

    val barTopFraction: Float = 0.11f,

    val contentTopPaddingDp: Float = 20f,

    val categoryIconSelectedDp: Float = 72f,

    val categoryIconDp: Float = 56f,

    val itemIconDp: Float = 62f,
    val itemIconSlotDp: Float = 74f,

    val itemTextSelectedSp: Float = 22f,
    val itemTextSp: Float = 18f,

    val itemTextStartGapDp: Float = 14f,

    val leftAnchorExtraDp: Float = 6f,
) {
    companion object {
        val DEFAULT = CrossbarLayoutSpec()
    }
}
