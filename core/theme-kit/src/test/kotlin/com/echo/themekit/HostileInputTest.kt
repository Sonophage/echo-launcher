package com.echo.themekit

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertNull

class HostileInputTest {
    @Test
    fun `pfptheme zip bomb entry is rejected`() {
        val zip = ByteArrayOutputStream().also { baos ->
            ZipOutputStream(baos).use { z ->
                z.putNextEntry(ZipEntry("manifest.json"))
                z.write("""{"manifest":"pfptheme","schemaVersion":1,"name":"Bomb","accentColor":"#FFFFFF"}""".toByteArray())
                z.closeEntry()
                z.putNextEntry(ZipEntry("wallpaper.png"))
                val chunk = ByteArray(1024 * 1024)
                repeat(96) { z.write(chunk) }
                z.closeEntry()
            }
        }.toByteArray()
        assertNull(EchoThemeCodec.read(zip))
    }
}
