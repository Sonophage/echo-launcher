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

internal fun List<InstalledApp>.wallSections(tab: AppFilter, system: String?): Pair<List<InstalledApp>, List<InstalledApp>> =
    filter(tab::matches).ofSystem(system) to filterNot(tab::matches)

private const val ANDROID_LABEL = "Android"
