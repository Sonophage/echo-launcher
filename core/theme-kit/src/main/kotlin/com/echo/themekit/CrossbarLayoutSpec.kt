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

    // how much larger the selected category icon is drawn than ECHO draws it: the theme's selected-to-resting
    // ratio over the default's, so ECHO's own layout (72 to 56) draws exactly as before, at 1. The crossbar and
    // Theme Studio's preview both read it
    fun selectedIconScale(): Float =
        (categoryIconSelectedDp / categoryIconDp) / (DEFAULT.categoryIconSelectedDp / DEFAULT.categoryIconDp)
}
