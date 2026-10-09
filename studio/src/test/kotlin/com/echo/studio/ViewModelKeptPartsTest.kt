package com.echo.studio

import com.echo.themekit.EchoThemeBundle
import com.echo.themekit.EchoThemeCodec
import com.echo.themekit.EchoThemeManifest
import com.echo.themekit.ThemeImage
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

// owner, 2026-10-07: a theme carries parts Studio does not edit. Opening a theme in Studio and exporting it
// again must not drop them
class ViewModelKeptPartsTest {
    @Test
    fun `a theme opened and exported again keeps its media, console icons and style choices`() = runBlocking {
        val png = pngBytes()
        val source = File.createTempFile("studio-kept", ".echo-theme")
        val out = File.createTempFile("studio-kept-out", ".echo-theme")
        try {
            source.writeBytes(EchoThemeCodec.write(EchoThemeBundle(
                manifest = EchoThemeManifest(
                    name = "Kept", accentColor = "#123456", waveDesign = "ECHO_ARCS", buttonSet = "NINTENDO",
                    settings = kotlinx.serialization.json.buildJsonObject { put("pref_xmb_row_cover_art", kotlinx.serialization.json.JsonPrimitive(true)) },
                ),
                wallpaper = null,
                preview = null,
                sysicons = mapOf("psx" to ThemeImage(png, "png")),
                media = mapOf("sound_back" to ThemeImage("RIFF....WAVE".toByteArray(), "wav")),
            )))
            val vm = StudioViewModel(CoroutineScope(Dispatchers.Default))
            vm.openFile(source)
            vm.awaitIdle()
            vm.exportTo(out) { null }
            vm.awaitIdle()

            val exported = assertNotNull(EchoThemeCodec.read(out), "export: ${vm.state.value.dialog}")
            assertEquals(setOf("sound_back"), exported.media.keys)
            assertEquals(setOf("psx"), exported.sysicons.keys)
            assertEquals("ECHO_ARCS", exported.manifest.waveDesign)
            assertEquals("NINTENDO", exported.manifest.buttonSet)
            assertEquals("true", exported.manifest.settings?.get("pref_xmb_row_cover_art")?.toString(), "display settings")
        } finally {
            source.delete()
            out.delete()
        }
    }

    private fun pngBytes(size: Int = 32): ByteArray {
        val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        for (x in 0 until size) for (y in 0 until size) img.setRGB(x, y, 0xFF3366AA.toInt())
        return ByteArrayOutputStream().also { ImageIO.write(img, "png", it) }.toByteArray()
    }

    private suspend fun StudioViewModel.awaitIdle() {
        withTimeout(10_000) {
            delay(50)
            while (state.value.busy) delay(25)
        }
    }
}
