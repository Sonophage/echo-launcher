package com.echo.studio

import com.echo.themekit.EchoThemeBundle
import com.echo.themekit.EchoThemeCodec
import com.echo.themekit.EchoThemeManifest
import com.echo.themekit.ThemeImage
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.TestScope

class RoundTripTest {
    @Test
    fun `studio state exports and reopens identically`() {
        val viewModel = StudioViewModel(TestScope())
        val state = StudioState(
            name = "Round Trip",
            accentArgb = 0xFFE87FB0.toInt(),
            iconColor = IconColorChoice.Custom(0xFFFFD700.toInt()),
            waveStyle = EchoThemeManifest.WAVE_REDUCED,
            wallpaperPng = ByteArray(256) { it.toByte() },
            iconOverrides = mapOf(
                "catbar_games" to ByteArray(64) { 1 },
                "item_playlist" to ByteArray(48) { 2 },
            ),
        )

        val manifest = viewModel.buildManifest(state, today = LocalDate.of(2026, 7, 7))
        val bundle = EchoThemeBundle(
            manifest = manifest,
            wallpaper = state.wallpaperPng,
            preview = ByteArray(32) { 3 },
            icons = state.iconOverrides.mapValues { (_, png) -> ThemeImage(png, "png") },
        )

        val decoded = assertNotNull(EchoThemeCodec.read(EchoThemeCodec.write(bundle)))
        assertEquals("Round Trip", decoded.manifest.name)
        assertEquals("#E87FB0", decoded.manifest.accentColor)
        assertEquals("#FFD700", decoded.manifest.iconColor)
        assertEquals(EchoThemeManifest.WAVE_REDUCED, decoded.manifest.waveStyle)
        assertEquals(EchoThemeManifest.SCHEMA_VERSION, decoded.manifest.schemaVersion)
        assertEquals("2026-07-07", decoded.manifest.created)
        assertContentEquals(state.wallpaperPng, decoded.wallpaper)
        assertEquals(setOf("catbar_games", "item_playlist"), decoded.icons.keys)
        assertContentEquals(state.iconOverrides["catbar_games"], decoded.icons["catbar_games"]?.bytes)
    }

    @Test
    fun `layout overrides survive export and untouched fields round-trip`() {
        val vm = StudioViewModel(TestScope())
        val layout = com.echo.themekit.CrossbarLayoutSpec(barTopFraction = 0.22f, itemTextSp = 16f)
        val manifest = vm.buildManifest(
            StudioState(name = "Aligned", layout = layout),
            today = LocalDate.of(2026, 7, 7),
        )
        val decoded = assertNotNull(
            EchoThemeCodec.read(EchoThemeCodec.write(EchoThemeBundle(manifest, null, null))),
        )
        assertEquals(layout, decoded.manifest.layout)
    }

    @Test
    fun `default layout exports as null`() {
        val manifest = StudioViewModel(TestScope())
            .buildManifest(StudioState(name = "Plain"), today = LocalDate.of(2026, 7, 7))
        assertEquals(null, manifest.layout)
    }

    @Test
    fun `setBarTopFraction clamps into the safe range`() {
        val vm = StudioViewModel(TestScope())
        vm.setBarTopFraction(0.9f)
        assertEquals(0.45f, vm.state.value.layout.barTopFraction)
        vm.setBarTopFraction(-1f)
        assertEquals(0.05f, vm.state.value.layout.barTopFraction)
    }

    @Test
    fun `auto icon color exports as the auto sentinel`() {
        val manifest = StudioViewModel(TestScope())
            .buildManifest(StudioState(name = "Auto Icons"), today = LocalDate.of(2026, 7, 7))
        assertEquals(EchoThemeManifest.ICON_COLOR_AUTO, manifest.iconColor)
    }
}
