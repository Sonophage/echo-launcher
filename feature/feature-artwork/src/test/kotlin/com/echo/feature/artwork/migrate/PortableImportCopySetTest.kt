package com.echo.feature.artwork.migrate

import com.echo.core.data.database.entity.GameEntity
import com.echo.feature.artwork.migrate.PortableArtworkImportWorker.Companion.columnRefsOf
import com.echo.feature.artwork.migrate.PortableArtworkImportWorker.Companion.shouldCopy
import com.echo.feature.artwork.store.ArtworkKind
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortableImportCopySetTest {
    private fun game(
        id: Long = 1L,
        artwork: String? = null,
        icon: String? = null,
        logo: String? = null,
    ) = GameEntity(
        id = id, title = "Game $id", platformId = "ps2", romPath = null,
        packageName = null, emulatorPackage = null,
        artworkUri = artwork, logoUri = logo, iconUri = icon,
        description = null, developer = null, publisher = null,
        releaseYear = null, genre = null, steamGridDbId = null, createdAt = 0L,
    )

    @Test
    fun `a kind with a studio tab is copied`() {
        PortableArtworkImportWorker.KEPT_KINDS.forEach { kind ->
            assertTrue(
                shouldCopy(kind.name, "content://tree/x", emptySet()),
                "$kind has a slot, so leaving it behind loses art the user can still see",
            )
        }
    }

    @Test
    fun `a dropped kind is left behind when nothing points at its file`() {
        listOf(ArtworkKind.HERO, ArtworkKind.BOX_ART, ArtworkKind.BOX_3D, ArtworkKind.PHYSICAL_MEDIA)
            .forEach { kind ->
                assertFalse(shouldCopy(kind.name, "content://tree/x", emptySet()))
            }
    }

    @Test
    fun `a dropped kind IS copied when a game column names its file`() {
        val heroFile = "content://tree/ES-DE/miximages/Crash.png"
        val refs = columnRefsOf(listOf(game(artwork = heroFile)))

        assertTrue(
            shouldCopy(ArtworkKind.HERO.name, heroFile, refs),
            "artwork_uri points at the HERO file for most scraped games. Skipping it because " +
                "HERO lost its tab leaves the background pointing into a folder the import is " +
                "about to unlink.",
        )
    }

    @Test
    fun `every column is consulted, not just the background`() {
        val icon = "content://tree/icon.png"
        val logo = "content://tree/logo.png"
        val refs = columnRefsOf(listOf(game(icon = icon), game(id = 2L, logo = logo)))

        assertTrue(shouldCopy(ArtworkKind.BOX_ART.name, icon, refs))
        assertTrue(shouldCopy(ArtworkKind.BOX_3D.name, logo, refs))
    }

    @Test
    fun `a blank column does not make every record eligible`() {
        val refs = columnRefsOf(listOf(game(artwork = "", icon = "   ", logo = null)))

        assertFalse(
            shouldCopy(ArtworkKind.HERO.name, "", refs),
            "an empty column must not match an empty documentUri and drag the whole library across",
        )
    }

    @Test
    fun `a stored type that is not a kind at all is not copied on its own`() {
        assertFalse(shouldCopy("NOT_A_KIND", "content://tree/x", emptySet()))
    }
}
