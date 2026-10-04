package com.echo.feature.settings.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.themekit.CrossbarFormFactor
import com.echo.themekit.CrossbarLayoutAdjust
import com.echo.themekit.CrossbarLayoutAdjustCodec
import com.echo.themekit.CrossbarLayoutPreset

internal object ClassicCrossbarLayout {
    val KEY = stringPreferencesKey("display_xmb_layout_adjust")

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

    fun write(prefs: MutablePreferences, target: Target) {
        val map = CrossbarLayoutAdjustCodec.decode(prefs[KEY]).toMutableMap()
        map[target.bucketKey] = target.preset
        prefs[KEY] = CrossbarLayoutAdjustCodec.encode(map)
    }
}
