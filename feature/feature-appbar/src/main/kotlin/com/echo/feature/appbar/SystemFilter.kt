package com.echo.feature.appbar

import com.echo.core.domain.model.PlatformIds

data class SystemChip(val id: String?, val label: String, val count: Int)

internal val InstalledApp.systemId: String get() = platformId ?: PlatformIds.ANDROID

internal fun systemChips(games: List<InstalledApp>): List<SystemChip> =
    listOf(SystemChip(null, "All", games.size)) +
        games.groupBy { it.systemId }
            .map { (id, apps) -> SystemChip(id, apps.first().platformName ?: ANDROID_LABEL, apps.size) }
            .sortedWith(compareByDescending<SystemChip> { it.count }.thenBy { it.label.lowercase() })

internal fun List<InstalledApp>.ofSystem(id: String?): List<InstalledApp> =
    if (id == null) this else filter { it.systemId == id }

// the Games section narrowed to one genre; null keeps them all
internal fun List<InstalledApp>.ofGenre(genre: com.echo.core.domain.model.GameGenre?): List<InstalledApp> =
    if (genre == null) this else filter { it.genre == genre }

// the footer's word: the section, and the genre it is narrowed to
internal fun sectionLabel(filter: AppFilter, genre: com.echo.core.domain.model.GameGenre?): String =
    listOfNotNull(filter.label, genre?.takeIf { filter == AppFilter.GAMES }?.label).joinToString(" · ")

private const val ANDROID_LABEL = "Android"
