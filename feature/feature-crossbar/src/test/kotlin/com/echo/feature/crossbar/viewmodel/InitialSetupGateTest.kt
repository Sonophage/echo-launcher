package com.echo.feature.crossbar.viewmodel

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.domain.model.MemoryCard
import com.echo.core.domain.model.PlatformIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InitialSetupGateTest {
    @Test fun `a fresh install has no existing config`() {
        assertFalse(CrossbarViewModel.hasExistingSetupConfig(preferencesOf()))
    }

    @Test fun `any root folder counts as existing config`() {
        listOf(
            "library_rom_root_tree_uris",
            "library_rom_root_tree_uri",
            "music_root_tree_uris",
            "video_root_tree_uris",
            "photo_root_tree_uris",
            "book_root_tree_uris",
            "artwork_folder_tree_uri",
        ).forEach { key ->
            assertTrue(
                key,
                CrossbarViewModel.hasExistingSetupConfig(
                    preferencesOf(stringPreferencesKey(key) to "content://tree/primary%3AStuff")
                ),
            )
        }
    }

    @Test fun `a blank stored root does not count`() {
        val prefs = preferencesOf(stringPreferencesKey("music_root_tree_uris") to "  ")
        assertFalse(CrossbarViewModel.hasExistingSetupConfig(prefs))
    }

    @Test fun `stored service credentials count as existing config`() {
        listOf(
            "sgdb_api_key",
            "igdb_client_id",
            "ss_username",
            "tmdb_api_key",
        ).forEach { key ->
            assertTrue(
                key,
                CrossbarViewModel.hasExistingSetupConfig(
                    preferencesOf(stringPreferencesKey(key) to "some-value")
                ),
            )
        }
    }

    @Test fun `every media root kind counts, so a new kind cannot be forgotten`() {
        com.echo.core.data.repository.MediaRootKind.entries.forEach { kind ->
            assertTrue(
                "${kind.name} roots do not stop the first-run wizard",
                CrossbarViewModel.hasExistingSetupConfig(
                    preferencesOf(stringPreferencesKey(kind.key) to "content://tree/primary%3AStuff")
                ),
            )
        }
    }

    @Test fun `a completed library setup counts as existing config`() {
        val prefs = preferencesOf(booleanPreferencesKey("library_setup_complete") to true)
        assertTrue(CrossbarViewModel.hasExistingSetupConfig(prefs))
    }

    private val seededAndroidCard = MemoryCard(platformId = PlatformIds.ANDROID, displayName = "Android Memory Card")

    private val romRoot = stringPreferencesKey("library_rom_root_tree_uris") to "content://tree/primary%3AROMs"

    @Test fun `a fresh install whose only card is the auto-seeded Android card still shows the wizard`() {
        assertEquals(
            InitialSetupDecision.OPEN_WIZARD,
            CrossbarViewModel.initialSetupDecision(preferencesOf(), listOf(seededAndroidCard)),
        )
    }

    @Test fun `a wizard that started but never finished reopens, even with the folders it already saved`() {
        val prefs = preferencesOf(booleanPreferencesKey("initial_setup_started") to true, romRoot)
        assertEquals(
            InitialSetupDecision.OPEN_WIZARD,
            CrossbarViewModel.initialSetupDecision(prefs, listOf(seededAndroidCard, MemoryCard("psp", "PSP"))),
        )
    }

    @Test fun `a genuine upgrade with real setup skips the wizard`() {
        assertEquals(
            InitialSetupDecision.SEED_AS_SEEN,
            CrossbarViewModel.initialSetupDecision(preferencesOf(romRoot), listOf(seededAndroidCard)),
        )
        assertEquals(
            InitialSetupDecision.SEED_AS_SEEN,
            CrossbarViewModel.initialSetupDecision(preferencesOf(), listOf(MemoryCard("psp", "PSP"))),
        )
    }

    @Test fun `an upgrade whose library is only Android apps skips the wizard`() {
        assertEquals(
            InitialSetupDecision.SEED_AS_SEEN,
            CrossbarViewModel.initialSetupDecision(preferencesOf(), listOf(seededAndroidCard.copy(gameCount = 3))),
        )
    }

    @Test fun `a finished wizard never reopens`() {
        val prefs = preferencesOf(
            booleanPreferencesKey("initial_setup_seen") to true,
            booleanPreferencesKey("initial_setup_started") to true,
        )
        assertEquals(InitialSetupDecision.ALREADY_SEEN, CrossbarViewModel.initialSetupDecision(prefs, emptyList()))
    }

    @Test fun `only the first-run wizard holds back the startup notifications prompt, so it is asked once`() {
        assertTrue(CrossbarViewModel.wizardOwnsNotificationPrompt(InitialSetupDecision.OPEN_WIZARD))
        assertFalse(CrossbarViewModel.wizardOwnsNotificationPrompt(InitialSetupDecision.ALREADY_SEEN))
        assertFalse(CrossbarViewModel.wizardOwnsNotificationPrompt(InitialSetupDecision.SEED_AS_SEEN))
    }
}
