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
import com.echo.themekit.ThemePart
import com.echo.themekit.CatalogTheme
import com.echo.core.data.repository.ThemeCatalogRepository
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

    // which theme each part in use came from (the Mix screen)
    val partSources: Map<ThemePart, String> = emptyMap(),
    // the theme whose store page is open
    val page: ThemePage? = null,
    // the online store's themes; null while loading or when it could not be reached
    val online: List<CatalogTheme>? = null,
    val onlineFailed: Boolean = false,
)

// a theme's store page, for a saved theme or an online one. A applies a saved theme ([savedId]); an online
// theme not yet saved is downloaded first
data class ThemePage(
    val name: String,
    val author: String? = null,
    val version: String? = null,
    val description: String? = null,
    val parts: Set<ThemePart> = emptySet(),
    val hero: String? = null,
    val body: String? = null,
    val screenshots: List<String> = emptyList(),
    val savedId: String? = null,
    val online: CatalogTheme? = null,
    val busy: Boolean = false,
) {
    val actionLabel: String get() = when {
        busy -> "Downloading"
        savedId != null -> "Apply"
        else -> "Download"
    }
}

internal fun pageOf(theme: EchoThemeStore.SavedTheme, details: EchoThemeStore.ThemeDetails?) = ThemePage(
    name = theme.name, author = theme.author, version = theme.version,
    description = details?.readme?.description ?: theme.description, parts = theme.parts,
    hero = theme.heroPath ?: theme.previewPath, body = details?.readme?.body,
    screenshots = details?.screenshotPaths.orEmpty(), savedId = theme.id,
)

// owner, 2026-10-07: with parts taken from more than one theme, the look in use is a mix, not the last theme applied
internal fun activeThemeLabel(applied: String?, sources: Map<ThemePart, String>): String = when {
    sources.values.toSet().size > 1 -> "Mixed"
    else -> sources.values.firstOrNull() ?: applied ?: "Default"
}

@HiltViewModel
class ThemesSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val themeStore: EchoThemeStore,
    private val catalog: ThemeCatalogRepository,
) : ViewModel() {
    init { refreshOnline() }

    fun refreshOnline() {
        viewModelScope.launch {
            val themes = catalog.load()
            _extra.update { it.copy(online = themes, onlineFailed = themes == null) }
        }
    }

    private val _extra = MutableStateFlow(ThemesSettingsUiState())

    val uiState: StateFlow<ThemesSettingsUiState> = combine(
        context.echoDataStore.data,
        themeStore.themes,
        _extra,
    ) { prefs, saved, extra ->
        extra.copy(
            partSources        = EchoThemeStore.decodeSources(prefs[EchoThemeStore.KEY_PART_SOURCES]),
            activeThemeName    = activeThemeLabel(
                prefs[EchoThemeStore.KEY_APPLIED_THEME_NAME],
                EchoThemeStore.decodeSources(prefs[EchoThemeStore.KEY_PART_SOURCES]),
            ),
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

    fun applySavedTheme(id: String) {
        viewModelScope.launch {
            val ok = themeStore.apply(id)
            if (!ok) _extra.update { it.copy(installMessage = "Could not apply the theme") }
        }
    }

    // the theme store's page for one theme: its hero, README and screenshots
    fun openThemePage(id: String) {
        val theme = uiState.value.savedThemes.firstOrNull { it.id == id } ?: return
        _extra.update { it.copy(page = pageOf(theme, null)) }
        viewModelScope.launch {
            val details = themeStore.details(id)
            _extra.update { e -> if (e.page?.savedId == id) e.copy(page = pageOf(theme, details)) else e }
        }
    }

    // an online theme's page: its hero and screenshots from the store, its details from its README
    fun openOnlinePage(id: String) {
        val theme = uiState.value.online?.firstOrNull { it.id == id } ?: return
        val saved = uiState.value.savedThemes.firstOrNull { it.name == theme.name }
        _extra.update { it.copy(page = ThemePage(name = theme.name, hero = theme.heroUrl, screenshots = theme.screenshotUrls, savedId = saved?.id, online = theme)) }
        viewModelScope.launch {
            val readme = catalog.readme(theme) ?: return@launch
            _extra.update { e ->
                val page = e.page?.takeIf { it.online?.id == id } ?: return@update e
                e.copy(page = page.copy(author = readme.author, version = readme.version, description = readme.description, body = readme.body))
            }
        }
    }

    // A on a page: apply a saved theme, or download an online one first
    fun pageAction() {
        val page = uiState.value.page ?: return
        if (page.busy) return
        page.savedId?.let { applySavedTheme(it); closeThemePage(); return }
        val online = page.online ?: return
        _extra.update { it.copy(page = page.copy(busy = true)) }
        viewModelScope.launch {
            when (val result = catalog.install(online)) {
                is ThemeCatalogRepository.Install.Done -> _extra.update { e ->
                    e.copy(page = e.page?.copy(busy = false, savedId = result.theme.id, parts = result.theme.parts), installMessage = "Downloaded \"${result.theme.name}\"")
                }
                is ThemeCatalogRepository.Install.Failed -> _extra.update { e -> e.copy(page = e.page?.copy(busy = false), installMessage = result.reason) }
            }
        }
    }

    fun closeThemePage() = _extra.update { it.copy(page = null) }

    // one part of a theme, from the Mix screen
    fun applyPart(id: String, part: ThemePart) {
        viewModelScope.launch {
            if (!themeStore.apply(id, setOf(part))) _extra.update { it.copy(installMessage = "Could not apply the theme's ${part.label.lowercase()}") }
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
        EchoThemeStore.ImportResult.NotABundle -> "Not a valid ECHO theme file"
        EchoThemeStore.ImportResult.DamagedWallpaper -> "That theme's wallpaper is damaged"
        is EchoThemeStore.ImportResult.NotSaved -> "Could not save the imported theme"
    }

    private companion object {
        val KEY_ICON_COLOR      = longPreferencesKey("theme_icon_color")
    }
}
