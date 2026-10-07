package com.echo.core.data.repository

import android.content.Context
import com.echo.core.data.BuildConfig
import com.echo.themekit.CatalogTheme
import com.echo.themekit.EchoThemeCodec
import com.echo.themekit.EchoThemeFolder
import com.echo.themekit.ThemeCatalog
import com.echo.themekit.ThemeReadme
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

// the online theme store (owner, 2026-10-07): the catalog of the echo-themes repo, published on GitHub Pages.
// A theme is downloaded as a zip of its folder, checked against the catalog's size and SHA-256, read through
// EchoThemeFolder and stored like any imported theme
@Singleton
class ThemeCatalogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val http: HttpClient,
    private val store: EchoThemeStore,
) {
    sealed interface Install {
        data class Done(val theme: EchoThemeStore.SavedTheme) : Install
        data class Failed(val reason: String) : Install
    }

    // the catalog's address. A debug build reads another from files/theme-catalog-url.txt (written with
    // run-as), so a test can point it at a local server; any trouble reading it falls back to the real one
    private fun indexUrl(): String =
        if (BuildConfig.DEBUG) {
            runCatching { File(context.filesDir, "theme-catalog-url.txt").takeIf { it.isFile }?.readText()?.trim() }
                .getOrNull()?.takeIf { it.isNotEmpty() } ?: INDEX_URL
        } else INDEX_URL

    // the themes on offer, or null when the catalog cannot be read
    suspend fun load(): List<CatalogTheme>? = withContext(Dispatchers.IO) {
        val url = indexUrl()
        val text = fetch(url, MAX_INDEX_BYTES)?.decodeToString() ?: return@withContext null
        ThemeCatalog.parse(text, url).also { if (it == null) Timber.w("Theme catalog at %s is not one ECHO reads", url) }
    }

    suspend fun readme(theme: CatalogTheme): ThemeReadme? = withContext(Dispatchers.IO) {
        theme.readmeUrl?.let { fetch(it, EchoThemeCodec.MAX_README_BYTES.toLong()) }?.let { ThemeReadme.parse(it.decodeToString()) }
    }

    // downloads, checks and stores [theme]; a theme of the same name already saved is replaced
    suspend fun install(theme: CatalogTheme): Install = withContext(Dispatchers.IO) {
        val bytes = fetch(theme.archiveUrl, theme.size.coerceAtMost(SafeMedia.MAX_THEME_FILE_BYTES))
            ?: return@withContext Install.Failed("Could not download ${theme.name}")
        if (bytes.size.toLong() != theme.size || sha256(bytes) != theme.sha256) {
            return@withContext Install.Failed("${theme.name} did not match the store's checksum")
        }
        val bundle = EchoThemeFolder.fromArchive(bytes.inputStream())
            ?: return@withContext Install.Failed("${theme.name} is not a theme ECHO can read")
        val packed = EchoThemeCodec.write(bundle.copy(manifest = bundle.manifest.copy(name = theme.name)))
        val replacing = store.themes.value.firstOrNull { it.name == theme.name }?.id
        when (val result = store.importBundleDetailed(replacing) { packed.inputStream() }) {
            is EchoThemeStore.ImportResult.Success -> Install.Done(result.theme)
            else -> Install.Failed("Could not store ${theme.name}")
        }
    }

    // the body of [url], or null on any failure or past [cap] bytes
    private suspend fun fetch(url: String, cap: Long): ByteArray? = runCatching {
        val response = http.get(url)
        if (!response.status.isSuccess()) return@runCatching null
        response.bodyAsChannel().toInputStream().use { with(SafeMedia) { it.readCapped(cap) } }
    }.onFailure { Timber.w(it, "Theme store: could not fetch %s", url) }.getOrNull()

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    companion object {
        const val INDEX_URL = "https://sonophage.github.io/echo-themes/index.json"
        private const val MAX_INDEX_BYTES = 512L * 1024
    }
}
