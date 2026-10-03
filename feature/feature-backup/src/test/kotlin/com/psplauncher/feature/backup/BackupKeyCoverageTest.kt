package com.psplauncher.feature.backup

import org.junit.Assert.assertTrue
import org.junit.Test

class BackupKeyCoverageTest {
    private val covered = BackupManager.BACKED_UP_KEY_NAMES

    private fun assertCovered(vararg keys: String) {
        keys.forEach { key ->
            assertTrue("$key is not in BackupManager's backed-up key list", key in covered)
        }
    }

    @Test
    fun `the theme's text colour is backed up`() {
        assertCovered("display_text_color")
    }

    @Test
    fun `the XMB's own display switches are backed up`() {
        assertCovered("pref_xmb_game_metadata", "pref_xmb_item_backdrop")
    }

    @Test
    fun `icon and text appearance settings are backed up`() {
        assertCovered(
            "display_icon_legibility",
            "display_fade_by_distance",
            "display_card_art_grid",
            "display_text_shadow",
            "pref_animated_icons",
            "pref_icon1_linger_delay_seconds",
            "pref_video_snap_placement",
            "pref_xmb_game_metadata",
        )
    }

    @Test
    fun `XMB geometry is backed up`() {
        assertCovered(
            "display_xmb_layout_adjust",
        )
    }

    @Test
    fun `the one-colour theme cascade is backed up`() {
        assertCovered(
            "theme_accent_override",
            "theme_icon_color",
            "theme_applied_name",
            "theme_layout_spec",
        )
    }

    @Test
    fun `launch and artwork behaviour is backed up`() {
        assertCovered(
            "artwork_import_move_files",
            "pref_dl_manuals",
            "pref_dl_video_snaps",
        )
    }

    @Test
    fun `the interface choices added with the settings pass are backed up`() {
        assertCovered(
            "interface_show_device_notifications",
            "interface_island_shows_recent",
            "interface_last_played_size",
            "library_rescan_on_return",
            "video_seek_step_seconds",
            "video_controls_hide_ms",
            "controller_trigger_sensitivity",
            "controller_shoulder_hold",
        )
    }

    @Test
    fun `the photo viewer choice is backed up`() {
        assertCovered("photo_default_viewer")
    }

    @Test
    fun `the Library section's reader choice is backed up`() {
        assertCovered(
            "books_default_reader",

            "book_root_tree_uris",
        )
    }

    @Test
    fun `controller preferences are backed up`() {
        assertCovered(
            "controller_confirm_back_layout",
            "controller_xy_layout",
            "controller_display_type",
            "controller_scroll_speed",
            "controller_left_backs_out",
            "controller_mappings_v1",
        )
    }

    @Test
    fun `the wallpaper and scheme the font colour is measured against are backed up`() {
        assertCovered("display_custom_wallpaper", "display_color_scheme")
    }

    @Test
    fun `the RetroAchievements and Steam accounts are backed up, their API keys as encrypted credentials`() {
        assertCovered("ra_username", "ra_api_key", "steam_id64", "steam_api_key", "achievements_enabled")
        listOf("ra_api_key", "steam_api_key").forEach { key ->
            assertTrue(
                "$key is sealed with KeystoreSecretCipher; a restore must drop it when this device cannot decrypt it",
                key in BackupManager.ENCRYPTED_CREDENTIAL_KEYS,
            )
        }
    }

    @Test
    fun `keys excluded on purpose stay excluded`() {
        val migrationMarkers = listOf(
            "debug_seeded_v1", "themes_seeded_v1", "library_consolidated_v22", "data_prep_version",
        )

        val danglingStamp = listOf("theme_icons_stamp")

        val sessionState = listOf("achievements_sync_last", "session_blob")

        val derivedCaches = listOf("display_wallpaper_luma", "wallpaper_accent")

        (migrationMarkers + danglingStamp + sessionState + derivedCaches).forEach { key ->
            assertTrue(
                "$key is carried by BackupManager — if that is now intended, move it out of this list",
                key !in covered,
            )
        }
    }
}
