package com.echo.feature.settings.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import com.echo.themekit.CrossbarFormFactor
import com.echo.themekit.CrossbarLayoutAdjust
import com.echo.themekit.CrossbarLayoutAdjustCodec
import com.echo.themekit.CrossbarLayoutPreset

internal object ClassicCrossbarLayout {
    val KEY = com.echo.core.data.datastore.CROSSBAR_LAYOUT_ADJUST_KEY

    data class Target(val bucketKey: String, val preset: CrossbarLayoutAdjust)

    fun forWindow(context: Context): Target {
        val config = context.resources.configuration
        return Target(
            bucketKey = CrossbarFormFactor.forSmallestWidthDp(config.smallestScreenWidthDp).key,
            preset = CrossbarLayoutPreset.computeForWindowDp(
                widthDp = config.screenWidthDp.toFloat(),
                heightDp = config.screenHeightDp.toFloat(),
                density = context.resources.displayMetrics.density,
            ),
        )
    }

    fun isApplied(prefs: Preferences, target: Target): Boolean =
        CrossbarLayoutPreset.matches(CrossbarLayoutAdjustCodec.decode(prefs[KEY])[target.bucketKey], target.preset)

    // this screen size's layout, as the crossbar reads it
    fun current(prefs: Preferences, bucketKey: String): CrossbarLayoutAdjust =
        CrossbarLayoutAdjustCodec.decode(prefs[KEY])[bucketKey] ?: CrossbarLayoutAdjust.DEFAULT

    // owner, 2026-10-06: Settings ▸ Layout's sliders write the layout straight in, no live overlay
    fun update(prefs: MutablePreferences, bucketKey: String, transform: (CrossbarLayoutAdjust) -> CrossbarLayoutAdjust) {
        val map = CrossbarLayoutAdjustCodec.decode(prefs[KEY]).toMutableMap()
        map[bucketKey] = CrossbarLayoutAdjustCodec.sanitize(transform(map[bucketKey] ?: CrossbarLayoutAdjust.DEFAULT))
        prefs[KEY] = CrossbarLayoutAdjustCodec.encode(map)
    }

    fun write(prefs: MutablePreferences, target: Target) {
        val map = CrossbarLayoutAdjustCodec.decode(prefs[KEY]).toMutableMap()
        map[target.bucketKey] = target.preset
        prefs[KEY] = CrossbarLayoutAdjustCodec.encode(map)
    }
}
