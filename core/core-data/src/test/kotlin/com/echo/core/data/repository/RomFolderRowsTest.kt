package com.echo.core.data.repository

import com.echo.core.domain.model.MemoryCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RomFolderRowsTest {
    private val romsTree = "content://com.android.externalstorage.documents/tree/primary%3ARoms"
    private val sdTree = "content://com.android.externalstorage.documents/tree/1A2B-3C4D%3ARoms"

    private val rawPaths = mapOf(
        romsTree to "/storage/emulated/0/Roms",
        sdTree to "/storage/1A2B-3C4D/Roms",
    )

    private fun card(platformId: String, dir: String?, games: Int = 0) = MemoryCard(
        platformId = platformId,
        displayName = "${platformId.uppercase()} Memory Card",
        romDirectory = dir,
        gameCount = games,
    )

    private fun entries(
        roots: List<String> = listOf(romsTree),
        cards: List<MemoryCard> = emptyList(),
        persisted: Set<String>? = null,
    ) = romFolderEntries(
        roots = roots,
        persistedReadUris = persisted ?: roots.toSet(),
        cards = cards,
        rawPathOfTree = { rawPaths[it] },
        fallbackName = { "Roms" },
    )

    @Test
    fun `each console is listed under the root its directory sits in`() {
        val listed = entries(
            roots = listOf(romsTree, sdTree),
            cards = listOf(
                card("psx", "/storage/emulated/0/Roms/psx", games = 12),
                card("gba", "/storage/1A2B-3C4D/Roms/gba", games = 4),
            ),
        )

        assertEquals(
            "a console belongs to the grant its files are actually under, not to the first root",
            listOf("Roms", "psx", "Roms", "gba"),
            listed.map {
                when (it) {
                    is RomFolderEntry.Root -> it.name
                    is RomFolderEntry.Console -> it.platformId
                }
            },
        )
    }

    @Test
    fun `a root counts the consoles and games beneath it, not the whole library`() {
        val root = entries(
            roots = listOf(romsTree, sdTree),
            cards = listOf(
                card("psx", "/storage/emulated/0/Roms/psx", games = 12),
                card("nds", "/storage/emulated/0/Roms/nds", games = 7),
                card("gba", "/storage/1A2B-3C4D/Roms/gba", games = 4),
            ),
        ).filterIsInstance<RomFolderEntry.Root>().first()

        assertEquals(2, root.consoleCount)
        assertEquals("a root's game count is its own consoles' games", 19, root.gameCount)
    }

    @Test
    fun `a sibling directory with the same prefix is not swallowed`() {
        val listed = entries(
            roots = listOf(romsTree),
            cards = listOf(card("psx", "/storage/emulated/0/Roms2/psx")),
        )

        val root = listed.filterIsInstance<RomFolderEntry.Root>().single()
        assertEquals(
            "/Roms2 is a different folder from /Roms, and a raw startsWith would claim it",
            0,
            root.consoleCount,
        )
        assertFalse(listed.filterIsInstance<RomFolderEntry.Console>().single().underRoot)
    }

    @Test
    fun `a console under no known root is still listed, at the end, marked adrift`() {
        val listed = entries(
            roots = listOf(romsTree),
            cards = listOf(
                card("psx", "/storage/emulated/0/Roms/psx"),
                card("windows", null),
            ),
        )

        val last = listed.last() as RomFolderEntry.Console
        assertEquals(
            "a console whose root was removed must not vanish from the only screen that manages it",
            "windows",
            last.platformId,
        )
        assertFalse(last.underRoot)
    }

    @Test
    fun `a console is claimed once even when two roots could contain it`() {
        val nested = "content://com.android.externalstorage.documents/tree/primary%3ARoms%2Fpsx"
        val listed = romFolderEntries(
            roots = listOf(romsTree, nested),
            persistedReadUris = setOf(romsTree, nested),
            cards = listOf(card("psx", "/storage/emulated/0/Roms/psx")),
            rawPathOfTree = mapOf(
                romsTree to "/storage/emulated/0/Roms",
                nested to "/storage/emulated/0/Roms/psx",
            )::get,
            fallbackName = { "Roms" },
        )

        assertEquals(
            "granting a subfolder of an existing root must not list its console twice",
            1,
            listed.filterIsInstance<RomFolderEntry.Console>().count { it.platformId == "psx" },
        )
    }

    @Test
    fun `a root whose grant is lost is still listed and marked unlinked`() {
        val root = entries(persisted = emptySet())
            .filterIsInstance<RomFolderEntry.Root>().single()

        assertTrue(root.treeUri == romsTree)
        assertFalse("a lost grant must still offer Relink", root.linked)
    }
}
