package com.psplauncher.feature.xmb.ui.detail

import com.psplauncher.feature.artwork.store.ArtworkKind

internal fun servesKind(source: StudioSource, kind: ArtworkKind): Boolean = when (source) {
    StudioSource.SCREENSCRAPER -> SS_TYPES_FOR_KIND.containsKey(kind)
    StudioSource.STEAMGRIDDB,
    StudioSource.IGDB          -> kind !in NO_IMAGE_PROVIDER_KINDS
    StudioSource.LOCAL         -> true
}

data class StudioProviderCard(
    val source: StudioSource,
    val serves: Int,
    val total: Int,
    val reason: String?,
    val quota: String?,
) {
    val pickable: Boolean get() = reason == null
    val servesAll: Boolean get() = serves == total
}

fun providerQuotaLabel(requestsToday: Int?, dailyCap: Int?): String = when {
    requestsToday == null -> "—"
    dailyCap == null || dailyCap <= 0 -> requestsToday.toString()
    else -> "$requestsToday / $dailyCap"
}

fun providerUnavailableReason(source: StudioSource): String = when (source) {
    StudioSource.IGDB -> "Needs a Client ID and Secret. Add them in Settings ▸ Artwork."
    else              -> "Needs an API key. Add one in Settings ▸ Artwork."
}

fun providerCards(
    unavailable: Set<StudioSource>,
    requestsToday: Int?,
    dailyCap: Int?,
): List<StudioProviderCard> = StudioSource.entries.map { source ->
    StudioProviderCard(
        source = source,
        serves = STUDIO_TABS.count { servesKind(source, it.kind) },
        total = STUDIO_TABS.size,
        reason = if (source in unavailable) providerUnavailableReason(source) else null,
        quota = if (source == StudioSource.SCREENSCRAPER) providerQuotaLabel(requestsToday, dailyCap) else null,
    )
}

enum class StudioReviewStatus(val label: String) {
    NEW("New"),
    KEPT("Kept"),
    REMOVED("Removed"),
    EMPTY("Empty"),
}

data class StudioReviewEntry(
    val kind: ArtworkKind,
    val label: String,
    val status: StudioReviewStatus,
)

data class StudioReviewSummary(val entries: List<StudioReviewEntry>) {
    private fun count(status: StudioReviewStatus) = entries.count { it.status == status }

    val added: Int get() = count(StudioReviewStatus.NEW)
    val removed: Int get() = count(StudioReviewStatus.REMOVED)
    val kept: Int get() = count(StudioReviewStatus.KEPT)

    val hasChanges: Boolean get() = added > 0 || removed > 0

    val line: String
        get() = listOfNotNull(
            added.takeIf { it > 0 }?.let { "$it new" },
            removed.takeIf { it > 0 }?.let { "$it removed" },
            kept.takeIf { it > 0 }?.let { "$it kept" },
        ).joinToString(" · ").ifEmpty { "Nothing to apply" }

    fun of(kind: ArtworkKind): StudioReviewStatus =
        entries.firstOrNull { it.kind == kind }?.status ?: StudioReviewStatus.EMPTY
}

fun studioReviewSummary(
    selection: Set<ArtworkKind>,
    removals: Set<ArtworkKind>,
    filled: Set<ArtworkKind>,
): StudioReviewSummary = StudioReviewSummary(
    STUDIO_TABS.map { tab ->
        StudioReviewEntry(
            kind = tab.kind,
            label = tab.label,
            status = when {
                tab.kind in selection -> StudioReviewStatus.NEW
                tab.kind in removals  -> StudioReviewStatus.REMOVED
                tab.kind in filled    -> StudioReviewStatus.KEPT
                else                  -> StudioReviewStatus.EMPTY
            },
        )
    }
)
