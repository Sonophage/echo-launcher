package com.echo.feature.settings.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.data.repository.EchoThemeStore
import com.echo.core.data.wallpaper.ThemeAccent
import com.echo.core.data.wallpaper.ThemeAccent.KEY_ACCENT_OVERRIDE
import com.echo.core.data.wallpaper.ThemeAccent.followWallpaperAccent
import com.echo.core.data.wallpaper.WallpaperLuminanceProbe
import com.echo.core.domain.model.EchoTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class ThemesSettingsUiState(

    val activeThemeName: String = "Default",
    val isInstalling: Boolean = false,
    val installMessage: String? = null,

    val accentOverrideArgb: Long? = null,

    val accentFromWallpaper: Boolean = false,

    val hasWallpaper: Boolean = false,
    val iconColorArgb: Long? = null,

    val savedThemes: List<EchoThemeStore.SavedTheme> = emptyList(),

    val installedThemes: List<EchoTheme> = emptyList(),
)

@HiltViewModel
class ThemesSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val themeStore: EchoThemeStore,
) : ViewModel() {
    private val _extra = MutableStateFlow(ThemesSettingsUiState())

    val uiState: StateFlow<ThemesSettingsUiState> = combine(
        context.echoDataStore.data,
        themeStore.themes,
        _extra,
    ) { prefs, saved, extra ->
        extra.copy(
            activeThemeName    = prefs[EchoThemeStore.KEY_APPLIED_THEME_NAME] ?: "Default",
            accentOverrideArgb = prefs[KEY_ACCENT_OVERRIDE],
            accentFromWallpaper = prefs[ThemeAccent.KEY_ACCENT_FROM_WALLPAPER] == true,
            hasWallpaper       = prefs[WallpaperLuminanceProbe.KEY_WALLPAPER_ACCENT] != null,
            iconColorArgb      = prefs[KEY_ICON_COLOR],
            savedThemes        = saved,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemesSettingsUiState())

    fun dismissMessage() = _extra.update { it.copy(installMessage = null) }

    fun setIconColor(argb: Long?) {
        viewModelScope.launch {
            context.echoDataStore.edit { prefs ->
                if (argb != null) prefs[KEY_ICON_COLOR] = argb else prefs.remove(KEY_ICON_COLOR)
            }
        }
    }

    fun setAccentFromWallpaper(enabled: Boolean) {
        viewModelScope.launch {
            context.echoDataStore.edit { prefs ->
                prefs[ThemeAccent.KEY_ACCENT_FROM_WALLPAPER] = enabled
                if (enabled) prefs.followWallpaperAccent(prefs[WallpaperLuminanceProbe.KEY_WALLPAPER_ACCENT])
                else prefs.remove(KEY_ACCENT_OVERRIDE)
            }
        }
    }

    fun clearAccentOverride() {
        viewModelScope.launch { context.echoDataStore.edit { it.remove(KEY_ACCENT_OVERRIDE) } }
    }

    fun resetTheme() {
        viewModelScope.launch {
            themeStore.resetApplied()
            Timber.i("Theme reset to default")
            _extra.update { it.copy(installMessage = "Theme reset — back to the default look") }
        }
    }

    fun createThemeFromPhoto(uri: Uri) {
        viewModelScope.launch {
            _extra.update { it.copy(isInstalling = true, installMessage = null) }
            val saved = themeStore.createFromImage(uri)
            val message = if (saved != null) {
                themeStore.apply(saved.id)
                "Created \"${saved.name}\"" +
                    if (saved.accentArgb != null) " — color derived from the photo" else ""
            } else "Could not read that image"
            _extra.update { it.copy(isInstalling = false, installMessage = message) }
        }
    }

    fun applySavedTheme(id: String) {
        viewModelScope.launch {
            val ok = themeStore.apply(id)
            if (!ok) _extra.update { it.copy(installMessage = "Could not apply the theme") }
        }
    }

    fun saveCurrentLookAsTheme(name: String) {
        viewModelScope.launch {
            val saved = themeStore.saveCurrentLook(name)
            _extra.update {
                it.copy(
                    installMessage = if (saved != null) "Saved \"${saved.name}\""
                    else "Could not save the theme",
                )
            }
        }
    }

    fun deleteSavedTheme(id: String) {
        viewModelScope.launch { themeStore.delete(id) }
    }

    fun shareSavedTheme(id: String) {
        viewModelScope.launch {
            val file = themeStore.exportForShare(id)
            if (file == null) {
                _extra.update { it.copy(installMessage = "Could not export the theme") }
                return@launch
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(send, "Share theme").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun importEchoTheme(uri: Uri) {
        viewModelScope.launch {
            _extra.update { it.copy(isInstalling = true, installMessage = null) }
            val result = themeStore.importBundleDetailed(uri)
            if (result is EchoThemeStore.ImportResult.Success) themeStore.apply(result.theme.id)
            _extra.update { it.copy(isInstalling = false, installMessage = messageFor(result)) }
        }
    }

    private fun messageFor(result: EchoThemeStore.ImportResult): String = when (result) {
        is EchoThemeStore.ImportResult.Success -> "Imported \"${result.theme.name}\""
        is EchoThemeStore.ImportResult.Unreadable -> "Could not open that file"
        EchoThemeStore.ImportResult.TooLarge -> "That theme is too large to import"
        EchoThemeStore.ImportResult.OutOfMemory ->
            "Not enough memory to import that theme — its motion wallpaper is too big"
        EchoThemeStore.ImportResult.NotABundle -> "Not a valid .pfptheme file"
        EchoThemeStore.ImportResult.DamagedWallpaper -> "That theme's wallpaper is damaged"
        is EchoThemeStore.ImportResult.NotSaved -> "Could not save the imported theme"
    }

    private companion object {
        val KEY_ICON_COLOR      = longPreferencesKey("theme_icon_color")
    }
}
