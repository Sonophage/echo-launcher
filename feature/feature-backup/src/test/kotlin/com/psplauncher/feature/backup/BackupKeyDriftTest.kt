package com.psplauncher.feature.backup

import java.io.File
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupKeyDriftTest {
    private val deliberatelyNotBackedUp = mapOf(
        "art_column_repair_done" to
            "a one-time local repair of this device's artwork columns; carrying it would " +
            "stop the repair running on a device that still needs it",

        "db_seeded_v1" to "seed marker",
        "debug_seeded_v1" to "seed marker",
        "themes_seeded_v1" to "seed marker",
        "library_consolidated_v22" to "migration marker",

        "last_played_placed_v1" to "one-shot position fix that a restore must be able to re-run",

        "network_renamed_v1" to "one-shot name fix that a restore must be able to re-run",
        "settings_on_panel_v1" to "one-shot move of Settings off the crossbar that a restore must be able to re-run",
        "initial_setup_started" to
            "marks a wizard in progress on this device; the seen flag is what a restore carries",
        "data_prep_version" to "migration marker",
        "open_play_session" to "a launch in progress on this device; it is settled and cleared on the next start",

        "session_blob" to
            "the Discord sign-in, sealed with a Keystore key that never leaves this device; " +
            "a restore could not open it, and a credential must not travel in a backup file",

        "theme_icons_stamp" to "dangling pointer into un-bundled files",

        "display_wallpaper_luma" to "recomputed by StartupDataPrep from the restored wallpaper",

        "wallpaper_accent" to "recomputed by StartupDataPrep from the restored wallpaper",

        "retroarch_documents_tree_uri" to "SAF grant cannot be restored",
        "vita3k_ux0_tree_uri" to "SAF grant cannot be restored",

        "retroarch_cached_core_files" to "rebuilt by the core scan",
        "auto_resolved_cores" to "rebuilt by emulator detection",

        "recent_app_dismissals" to
            "device-local and self-clearing: each stamp only means anything compared against " +
            "THIS device's UsageStats history, which no restore carries. Restoring them onto a " +
            "device with a different history would withhold apps the owner never dismissed there.",
    )

    // Every key in this file lives in the reader's DataStore, which the backup carries whole.
    private val readerStoreFile =
        "feature/feature-reader/src/main/kotlin/com/psplauncher/feature/reader/ReaderStore.kt"

    private val repoRoot: File by lazy {
        generateSequence(File(".").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").isFile }
            ?: error("could not find the repository root from ${File(".").absolutePath}")
    }

    private val keyPattern =
        Regex("""(?:string|boolean|int|float|long|double|stringSet)PreferencesKey\("([a-z_0-9]+)"\)""")

    private val dynamicKeyPrefixes = listOf("ui_media_name_")

    private val appSources: List<String> by lazy {
        repoRoot.walkTopDown().onEnter { it == repoRoot || !it.name.startsWith(".") }
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { "/build/" in it.path || "${File.separator}build${File.separator}" in it.path }
            .filterNot { "feature-backup" in it.path }
            .map { it.readText() }
            .toList()
    }

    private fun namedAnywhere(key: String): Boolean =
        appSources.any { "\"$key\"" in it }

    private val declared: Map<String, List<String>> by lazy {
        val found = mutableMapOf<String, MutableList<String>>()
        repoRoot.walkTopDown().onEnter { it == repoRoot || !it.name.startsWith(".") }
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { "/build/" in it.path || "${File.separator}build${File.separator}" in it.path }
            .filter { "src${File.separator}main" in it.path }

            .filterNot { "feature-backup" in it.path }
            .forEach { file ->
                keyPattern.findAll(file.readText()).forEach { m ->
                    found.getOrPut(m.groupValues[1]) { mutableListOf() }
                        .add(file.relativeTo(repoRoot).path)
                }
            }
        found
    }

    @Test
    fun `the scan actually found preferences, so the rest of this means something`() {
        assertTrue(declared.size >= 60, "only found ${declared.size} preference keys; the scan is broken")
        assertTrue("display_custom_wallpaper" in declared, "a known key is missing from the scan")
    }

    @Test
    fun `every preference the app declares is either backed up or deliberately excluded`() {
        val backedUp = BackupManager.BACKED_UP_KEY_NAMES
        val unaccounted = declared.keys
            .filterNot { it in backedUp || it in deliberatelyNotBackedUp }
            .filterNot { key -> declared.getValue(key).all { it == readerStoreFile } }
            .sorted()
        assertEquals(
            emptyList(),
            unaccounted,
            "these settings silently do not survive a restore. Add each to BackupManager's typed " +
                "lists, or to deliberatelyNotBackedUp with the reason it must not be carried: " +
                unaccounted.joinToString { "$it (${declared[it]?.firstOrNull()})" },
        )
    }

    @Test
    fun `the backup carries no key the app has stopped using`() {
        val orphans = (BackupManager.BACKED_UP_KEY_NAMES - declared.keys - LEGACY_RESTORE_ONLY)
            .filterNot { key -> dynamicKeyPrefixes.any { key.startsWith(it) } }
            .filterNot { namedAnywhere(it) }
            .sorted()
        assertEquals(
            emptyList(),
            orphans,
            "the backup writes these and nothing in the app declares them; delete them, or list " +
                "them in LEGACY_RESTORE_ONLY if an old archive still needs migrating through: $orphans",
        )
    }

    @Test
    fun `every dynamic key prefix is real, so the exemption cannot hide anything`() {
        dynamicKeyPrefixes.forEach { prefix ->
            val found = repoRoot.walkTopDown().onEnter { it == repoRoot || !it.name.startsWith(".") }
                .filter { it.isFile && it.extension == "kt" }
                .filterNot { "/build/" in it.path || "feature-backup" in it.path }
                .any { prefix in it.readText() }
            assertTrue(found, "dynamic prefix '$prefix' appears nowhere in the app")
        }
    }

    @Test
    fun `the reader store declares only types the reader snapshot carries`() {
        val source = File(repoRoot, readerStoreFile)
        assertTrue(source.isFile, "the reader store moved; point readerStoreFile at it: $readerStoreFile")
        val types = Regex("""(\w+)PreferencesKey\(""").findAll(source.readText()).map { it.groupValues[1] }.toList()
        assertTrue(types.size >= 4, "found only $types in the reader store; the scan is broken")
        assertEquals(
            emptyList(),
            types.filterNot { it == "string" || it == "float" },
            "ReaderSnapshot carries strings and floats only; a backup of any other type fails",
        )
    }

    @Test
    fun `nothing is in both lists`() {
        val both = BackupManager.BACKED_UP_KEY_NAMES intersect deliberatelyNotBackedUp.keys
        assertEquals(emptySet(), both, "a key cannot be both carried and deliberately excluded")
    }

    private companion object {
        val LEGACY_RESTORE_ONLY = setOf(

            "display_gameboot_mode",
        )
    }
}
