package com.echo.feature.crossbar.ui.detail

import com.echo.feature.artwork.store.ArtworkKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StudioStagesTest {
    @Test
    fun `the quota reads value against limit, and never invents a denominator`() {
        assertEquals("418 / 20000", providerQuotaLabel(418, 20_000))
        assertEquals("418", providerQuotaLabel(418, null))
        assertEquals("418", providerQuotaLabel(418, 0))
        assertEquals("—", providerQuotaLabel(null, null))
        assertEquals("—", providerQuotaLabel(null, 20_000))
    }

    @Test
    fun `only ScreenScraper serves every slot, and the picker says so`() {
        val cards = providerCards(unavailable = emptySet(), requestsToday = null, dailyCap = null)
        val byName = cards.associateBy { it.source }

        assertEquals(StudioSource.entries.size, cards.size)
        assertTrue(byName.getValue(StudioSource.SCREENSCRAPER).servesAll)
        assertTrue(byName.getValue(StudioSource.LOCAL).servesAll)
        assertFalse(byName.getValue(StudioSource.STEAMGRIDDB).servesAll)
        assertFalse(byName.getValue(StudioSource.IGDB).servesAll)
        assertEquals(
            STUDIO_TABS.size - NO_IMAGE_PROVIDER_KINDS.size,
            byName.getValue(StudioSource.STEAMGRIDDB).slots.size,
        )
        assertEquals(
            "Scans for tile, background, screenshot, logo",
            byName.getValue(StudioSource.STEAMGRIDDB).scansFor,
        )
    }

    @Test
    fun `a provider without a key carries its reason and is not pickable`() {
        val cards = providerCards(setOf(StudioSource.IGDB), requestsToday = null, dailyCap = null)
        val igdb = cards.first { it.source == StudioSource.IGDB }
        val sgdb = cards.first { it.source == StudioSource.STEAMGRIDDB }

        assertFalse(igdb.pickable)
        assertTrue(igdb.reason!!.contains("Client ID"))
        assertTrue(sgdb.pickable)
        assertNull(sgdb.reason)
    }

    @Test
    fun `only ScreenScraper's card carries the daily quota`() {
        val cards = providerCards(emptySet(), requestsToday = 418, dailyCap = 20_000)
        assertEquals("418 / 20000", cards.first { it.source == StudioSource.SCREENSCRAPER }.quota)
        assertTrue(cards.filterNot { it.source == StudioSource.SCREENSCRAPER }.all { it.quota == null })
    }

    @Test
    fun `the reason a slot is not offered names the provider and the slot`() {
        val tab = STUDIO_TABS.first { it.kind == ArtworkKind.VIDEO }
        assertEquals(
            "SteamGridDB has no PREVIEW VIDEO artwork. Choose a file from this device, or change provider.",
            slotNotOfferedReason(StudioSource.STEAMGRIDDB, tab),
        )
    }

    @Test
    fun `a provider hides the slots it cannot serve, but Local File keeps them all`() {
        val onSgdb = ArtworkStudioUiState(
            sourceIndex = StudioSource.entries.indexOf(StudioSource.STEAMGRIDDB),
        )
        assertFalse(
            onSgdb.visibleSlots.any { it.kind == ArtworkKind.ICON1 },
            "SteamGridDB has no tile video, so the pill is not drawn",
        )

        val onLocal = ArtworkStudioUiState(
            sourceIndex = StudioSource.entries.indexOf(StudioSource.LOCAL),
        )
        assertEquals(
            STUDIO_TABS.size,
            onLocal.visibleSlots.size,
            "Local File is the way back to a slot another provider hides",
        )
    }

    @Test
    fun `a slot the provider came back empty on drops out, unless you are standing on it`() {
        val sgdb = StudioSource.entries.indexOf(StudioSource.STEAMGRIDDB)
        val onIcon = ArtworkStudioUiState(
            sourceIndex = sgdb,
            tabIndex = STUDIO_TABS.indexOfFirst { it.kind == ArtworkKind.ICON },
            emptySlots = setOf(ArtworkKind.LOGO, ArtworkKind.ICON),
        )
        assertFalse(onIcon.visibleSlots.any { it.kind == ArtworkKind.LOGO })
        assertTrue(
            onIcon.visibleSlots.any { it.kind == ArtworkKind.ICON },
            "the slot under the cursor cannot vanish from under it",
        )
    }

    @Test
    fun `the review reads every slot, and a pick beats a removal beats what is stored`() {
        val summary = studioReviewSummary(
            selection = setOf(ArtworkKind.BACKGROUND, ArtworkKind.LOGO, ArtworkKind.ICON1),
            removals = setOf(ArtworkKind.SCREENSHOT, ArtworkKind.BACKGROUND),
            filled = setOf(ArtworkKind.ICON, ArtworkKind.SCREENSHOT, ArtworkKind.VIDEO),
        )

        assertEquals(STUDIO_TABS.size, summary.entries.size)
        assertEquals(StudioReviewStatus.NEW, summary.of(ArtworkKind.BACKGROUND))
        assertEquals(StudioReviewStatus.NEW, summary.of(ArtworkKind.LOGO))
        assertEquals(StudioReviewStatus.REMOVED, summary.of(ArtworkKind.SCREENSHOT))
        assertEquals(StudioReviewStatus.KEPT, summary.of(ArtworkKind.ICON))
        assertEquals(StudioReviewStatus.KEPT, summary.of(ArtworkKind.VIDEO))
        assertEquals(StudioReviewStatus.EMPTY, summary.of(ArtworkKind.MANUAL))

        assertEquals(3, summary.added)
        assertEquals(1, summary.removed)
        assertEquals(2, summary.kept)
        assertEquals("3 new · 1 removed · 2 kept", summary.line)
        assertTrue(summary.hasChanges)
    }

    @Test
    fun `a count of zero is left out of the line rather than printed`() {
        val addOnly = studioReviewSummary(
            selection = setOf(ArtworkKind.ICON),
            removals = emptySet(),
            filled = emptySet(),
        )
        assertEquals("1 new", addOnly.line)
    }

    @Test
    fun `an untouched library has nothing to apply`() {
        val nothing = studioReviewSummary(emptySet(), emptySet(), emptySet())
        assertEquals("Nothing to apply", nothing.line)
        assertFalse(nothing.hasChanges)

        val keptOnly = studioReviewSummary(emptySet(), emptySet(), setOf(ArtworkKind.ICON))
        assertEquals("1 kept", keptOnly.line)
        assertFalse(keptOnly.hasChanges)
    }
}
