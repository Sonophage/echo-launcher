package com.echo.themekit

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EchoThemeCodecV3Test {
    private val manifest = EchoThemeManifest(name = "V3 Pink", accentColor = "#FF72B1")

    @Test
    fun `v3 round-trips gif icons and sysicons and motion`() {
        val bundle = EchoThemeBundle(
            manifest = manifest,
            wallpaper = null,
            preview = null,
            icons = mapOf(
                "catbar_games" to ThemeImage(gifBytes(), "gif"),
                "catbar_music" to ThemeImage(pngV3Bytes(), "png"),
            ),
            sysicons = mapOf(
                "psx" to ThemeImage(pngV3Bytes(), "png"),
                "nes" to ThemeImage(gifBytes(), "gif"),
            ),
            motion = ThemeMotion.ofBytes(mp4Bytes(), "mp4"),
        )

        val decoded = assertNotNull(EchoThemeCodec.read(EchoThemeCodec.write(bundle)))

        assertEquals(bundle.manifest, decoded.manifest)
        assertEquals(setOf("catbar_games", "catbar_music"), decoded.icons.keys)
        assertEquals("gif", decoded.icons["catbar_games"]?.extension)
        assertTrue(decoded.icons["catbar_games"]!!.bytes.contentEquals(gifBytes()))
        assertEquals(setOf("psx", "nes"), decoded.sysicons.keys)
        assertEquals("mp4", decoded.motion?.extension)

        assertTrue(decoded.motion!!.drain().contentEquals(mp4Bytes()))
    }

    @Test
    fun `write sorts bundle entries for byte-deterministic output`() {
        fun build(icons: Map<String, ThemeImage>) = EchoThemeCodec.write(
            EchoThemeBundle(
                manifest = manifest,
                wallpaper = null,
                preview = null,
                icons = icons,
                sysicons = mapOf("nes" to ThemeImage(gifBytes(), "gif"), "psx" to ThemeImage(pngV3Bytes(), "png")),
            ),
        )

        val a = build(linkedMapOf("status_bluetooth" to ThemeImage(gifBytes(), "gif"), "catbar_games" to ThemeImage(gifBytes(), "gif")))
        val b = build(linkedMapOf("catbar_games" to ThemeImage(gifBytes(), "gif"), "status_bluetooth" to ThemeImage(gifBytes(), "gif")))

        assertTrue(a.contentEquals(b), "same entries in different insertion order must produce identical bytes")
    }

    @Test
    fun `write still drops unregistered icon keys but accepts console keys`() {
        val written = EchoThemeCodec.write(
            EchoThemeBundle(
                manifest = manifest,
                wallpaper = null,
                preview = null,
                icons = mapOf(
                    "not_a_slot" to ThemeImage(pngV3Bytes(), "png"),
                    "catbar_games" to ThemeImage(pngV3Bytes(), "png"),
                ),
            ),
        )
        val decoded = assertNotNull(EchoThemeCodec.read(written))
        assertEquals(setOf("catbar_games"), decoded.icons.keys)
    }

    @Test
    fun `read rejects sysicon keys that are not registered console slots`() {
        val hostile = zip(
            "manifest.json" to jsonManifest(),
            "sysicons/../evil.png" to ByteArray(4),
            "sysicon_default.png" to ByteArray(4),
            "sysicons/not_a_platform.png" to ByteArray(4),
            "sysicons/psx.png" to ByteArray(4),
        )
        val decoded = assertNotNull(EchoThemeCodec.read(hostile))
        assertEquals(setOf("psx"), decoded.sysicons.keys, "only registered console keys survive read")
    }

    @Test
    fun `read rejects sysicons entries outside the accepted extensions`() {
        val hostile = zip(
            "manifest.json" to jsonManifest(),
            "sysicons/psx.bmp" to ByteArray(4),
            "sysicons/psx.png" to pngV3Bytes(),
        )
        val decoded = assertNotNull(EchoThemeCodec.read(hostile))
        assertEquals(setOf("psx"), decoded.sysicons.keys)
    }

    private fun ThemeMotion.drain(): ByteArray =
        java.io.ByteArrayOutputStream().also { copyTo(it) }.toByteArray()

    @Test
    fun `read accepts only the known motion extensions`() {
        val withMotion = zip("manifest.json" to jsonManifest(), "motion.webm" to mp4Bytes())
        assertEquals("webm", assertNotNull(EchoThemeCodec.read(withMotion)).motion?.extension)

        val hostile = zip("manifest.json" to jsonManifest(), "motion.exe" to mp4Bytes())
        val decoded = assertNotNull(EchoThemeCodec.read(hostile))
        assertNull(decoded.motion, "an unknown motion extension is not a motion entry")
    }

    @Test
    fun `read keeps IconSlots gating on the icons directory`() {
        val hostile = zip(
            "manifest.json" to jsonManifest(),
            "icons/catbar_games.gif" to gifBytes(),
            "icons/not_a_slot.png" to pngV3Bytes(),
            "icons/../../evil.gif" to gifBytes(),
            "sysicon_snes.png" to pngV3Bytes(),
        )
        val decoded = assertNotNull(EchoThemeCodec.read(hostile))
        assertEquals(setOf("catbar_games"), decoded.icons.keys)
        assertTrue(decoded.sysicons.isEmpty(), "sysicon keys live under sysicons/, not icons/")
    }

    @Test
    fun `limits are raised for v3`() {
        assertEquals(256, EchoThemeCodec.BUNDLE_LIMITS.maxEntries)
        assertEquals(64L * 1024 * 1024, EchoThemeCodec.BUNDLE_LIMITS.maxEntryBytes)
        assertEquals(256L * 1024 * 1024, EchoThemeCodec.BUNDLE_LIMITS.maxTotalBytes)
        assertEquals((8 * 1024 * 1024).toLong(), EchoThemeCodec.MAX_ICON_BYTES.toLong())
    }

    @Test
    fun `oversized icon entries are dropped on read, not fatal`() {
        val big: ByteArray = ByteArray(EchoThemeCodec.MAX_ICON_BYTES + 1)
        val bundle = zip(
            "manifest.json" to jsonManifest(),
            "icons/catbar_games.png" to big,
            "icons/catbar_music.png" to pngV3Bytes(),
        )
        val decoded = assertNotNull(EchoThemeCodec.read(bundle), "an over-cap icon is dropped, the bundle still reads")
        assertEquals(setOf("catbar_music"), decoded.icons.keys)
    }

    // owner, 2026-10-07: .pfptheme is renamed .echo-theme; every theme already made or shared keeps opening
    @Test
    fun `a theme is written as echo-theme and one marked pfptheme still reads`() {
        assertEquals("echo-theme", EchoThemeCodec.FILE_EXTENSION)
        assertTrue("pfptheme" in EchoThemeCodec.READABLE_EXTENSIONS)
        val legacy = zip("manifest.json" to """{"manifest":"pfptheme","schemaVersion":3,"name":"Old","accentColor":"#FF0000"}""".toByteArray())
        assertNotNull(EchoThemeCodec.read(legacy), "a .pfptheme from before the rename opens")

        val rewritten = EchoThemeCodec.write(EchoThemeBundle(manifest.copy(manifest = "pfptheme"), null, null))
        assertEquals("echo-theme", assertNotNull(EchoThemeCodec.read(rewritten)).manifest.manifest)

        val stranger = zip("manifest.json" to """{"manifest":"zip","name":"Other","accentColor":"#FF0000"}""".toByteArray())
        assertNull(EchoThemeCodec.read(stranger), "another tool's manifest.json is not a theme")
    }

    @Test
    fun `v2-shaped bundle still reads unchanged`() {
        val v2 = zip(
            "manifest.json" to """{"manifest":"pfptheme","schemaVersion":2,"name":"Legacy","accentColor":"#FF0000"}""".toByteArray(),
            "icons/catbar_games.png" to pngV3Bytes(),
        )
        val decoded = assertNotNull(EchoThemeCodec.read(v2))
        assertEquals(2, decoded.manifest.schemaVersion)
        assertEquals(setOf("catbar_games"), decoded.icons.keys)
        assertEquals("png", decoded.icons["catbar_games"]?.extension)
        assertTrue(decoded.sysicons.isEmpty())
        assertNull(decoded.motion)
    }

    @Test
    fun `unknown manifest keys and unknown zip entries are ignored`() {
        val future = zip(
            "manifest.json" to """{"manifest":"pfptheme","schemaVersion":4,"name":"Future","accentColor":"#FF0000","hologram":"true"}""".toByteArray(),
            "sounds/cursor.at3" to ByteArray(4),
        )
        val decoded = assertNotNull(EchoThemeCodec.read(future))
        assertEquals(4, decoded.manifest.schemaVersion, "version is informational — readers never gate on it")
    }

    @Test
    fun `textColor round-trips and the schema version does not move`() {
        assertEquals(3, EchoThemeManifest.SCHEMA_VERSION, "textColor is additive — v3 stands")

        val written = EchoThemeCodec.write(
            EchoThemeBundle(
                manifest = EchoThemeManifest(
                    name = "Tinted",
                    accentColor = "#0055AA",
                    textColor = "#FF8800",
                ),
                wallpaper = null,
                preview = null,
            ),
        )
        val decoded = assertNotNull(EchoThemeCodec.read(written))
        assertEquals("#FF8800", decoded.manifest.textColor)
        assertEquals(3, decoded.manifest.schemaVersion)
    }

    @Test
    fun `a manifest predating textColor reads as auto`() {
        val older = zip(
            "manifest.json" to
                """{"manifest":"pfptheme","schemaVersion":3,"name":"Old","accentColor":"#FF0000"}""".toByteArray(),
        )
        val decoded = assertNotNull(EchoThemeCodec.read(older))
        assertEquals(EchoThemeManifest.ICON_COLOR_AUTO, decoded.manifest.textColor)
    }

    @Test
    fun `manifest without schemaVersion still reads (older writer)`() {
        val v1 = zip("manifest.json" to """{"manifest":"pfptheme","name":"Ancient","accentColor":"#FF0000"}""".toByteArray())
        val decoded = assertNotNull(EchoThemeCodec.read(v1))
        assertEquals("Ancient", decoded.manifest.name)
    }

    @Test
    fun `readManifest stops at the manifest and ignores the rest of the archive`() {
        val entries = arrayOf("manifest.json" to jsonManifest()) +
            Array(EchoThemeCodec.BUNDLE_LIMITS.maxEntries + 8) { i -> "filler/$i.png" to pngV3Bytes() }
        val file = tempBundle(zip(*entries))

        assertNull(EchoThemeCodec.read(file), "a full read is refused: too many entries")
        assertEquals("V3 Pink", assertNotNull(EchoThemeCodec.readManifest(file)).name)
    }

    @Test
    fun `read from a file streams the motion entry back out`() {
        val file = tempBundle(
            EchoThemeCodec.write(
                EchoThemeBundle(
                    manifest = manifest,
                    wallpaper = null,
                    preview = null,
                    motion = ThemeMotion.ofBytes(mp4Bytes(), "mp4"),
                ),
            ),
        )

        val decoded = assertNotNull(EchoThemeCodec.read(file))

        assertEquals("mp4", decoded.motion?.extension)
        assertTrue(decoded.motion!!.drain().contentEquals(mp4Bytes()), "streams from the file")

        assertTrue(decoded.motion!!.drain().contentEquals(mp4Bytes()), "and can be streamed again")
    }

    @Test
    fun `a bundle read from a bare stream reports no motion`() {
        val bytes = EchoThemeCodec.write(
            EchoThemeBundle(manifest, null, null, motion = ThemeMotion.ofBytes(mp4Bytes(), "mp4")),
        )

        assertNull(EchoThemeCodec.read(bytes.inputStream()).let { assertNotNull(it).motion })
    }

    private fun tempBundle(bytes: ByteArray): java.io.File =
        java.io.File.createTempFile("bundle", ".pfptheme").apply { deleteOnExit(); writeBytes(bytes) }

    private fun jsonManifest(): ByteArray =
        """{"manifest":"pfptheme","schemaVersion":3,"name":"V3 Pink","accentColor":"#FF72B1"}""".toByteArray()

    private fun gifBytes(): ByteArray =
        "GIF89a".toByteArray() + ByteArray(16) { it.toByte() }

    private fun pngV3Bytes(): ByteArray =
        byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + ByteArray(8) { it.toByte() }

    private fun mp4Bytes(): ByteArray =
        "ftypmp42".toByteArray() + ByteArray(12) { it.toByte() }

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray =
        ByteArrayOutputStream().also { baos ->
            ZipOutputStream(baos).use { z ->
                for ((name, data) in entries) {
                    z.putNextEntry(ZipEntry(name))
                    z.write(data)
                    z.closeEntry()
                }
            }
        }.toByteArray()
}
