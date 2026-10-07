package com.echo.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// owner, 2026-10-07: the online store installs a theme only when it is exactly the file the catalog names
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ThemeCatalogRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val archive = ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { z ->
            z.putNextEntry(ZipEntry("Wild/theme.json"))
            z.write("""{"manifest":"echo-theme","name":"Wild","accentColor":"#E8C66A","buttonSet":"NINTENDO"}""".toByteArray())
            z.closeEntry()
        }
    }.toByteArray()

    @Before
    fun clear() { File(context.filesDir, "pfpthemes").deleteRecursively() }

    private fun repo(served: ByteArray, sha: String = sha256(archive)): ThemeCatalogRepository {
        val index = """{"format":1,"themes":[{"id":"Wild","name":"Wild","archive":"themes/Wild.zip","sha256":"$sha","size":${archive.size}}]}"""
        val http = HttpClient(MockEngine { request ->
            when (request.url.encodedPath) {
                "/echo-themes/index.json" -> respond(index)
                "/echo-themes/themes/Wild.zip" -> respond(served)
                else -> respond("", HttpStatusCode.NotFound)
            }
        })
        return ThemeCatalogRepository(context, http, EchoThemeStore(context, UiMediaStore(context)))
    }

    @Test
    fun `a theme matching the catalog installs under the catalog's name`() = runTest {
        val r = repo(archive)
        val theme = assertNotNull(r.load()).single()
        val done = assertIs<ThemeCatalogRepository.Install.Done>(r.install(theme))
        assertEquals("Wild", done.theme.name)
        assertTrue(com.echo.themekit.ThemePart.BUTTONS in done.theme.parts)
    }

    @Test
    fun `a download that does not match the catalog's checksum is refused`() = runTest {
        val tampered = archive.copyOf().also { it[it.size - 30] = (it[it.size - 30] + 1).toByte() }
        val r = repo(tampered)
        assertIs<ThemeCatalogRepository.Install.Failed>(r.install(assertNotNull(r.load()).single()))
        assertEquals(emptyList(), EchoThemeStore(context, UiMediaStore(context)).themes.value)
    }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
