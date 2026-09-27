package com.psplauncher.core.data.repository

import com.psplauncher.core.domain.model.MemoryCard

sealed interface RomFolderEntry {
    data class Root(
        val treeUri: String,
        val name: String,
        val linked: Boolean,
        val consoleCount: Int,
        val gameCount: Int,
    ) : RomFolderEntry

    data class Console(
        val platformId: String,
        val displayName: String,
        val romDirectory: String?,
        val gameCount: Int,

        val underRoot: Boolean,
    ) : RomFolderEntry
}

fun isRomDirUnder(romDirectory: String, rootRawPath: String): Boolean {
    val dir = romDirectory.trimEnd('/')
    val root = rootRawPath.trimEnd('/')
    return dir == root || dir.startsWith("$root/")
}

fun romFolderEntries(
    roots: List<String>,
    persistedReadUris: Set<String>,
    cards: List<MemoryCard>,
    rawPathOfTree: (String) -> String?,
    fallbackName: (String) -> String,
): List<RomFolderEntry> {
    val claimed = mutableSetOf<String>()

    val grouped = roots.map { treeUri ->
        val rawPath = rawPathOfTree(treeUri)
        val mine = if (rawPath == null) emptyList() else cards.filter { card ->
            val dir = card.romDirectory
            dir != null && card.platformId !in claimed && isRomDirUnder(dir, rawPath)
        }
        mine.forEach { claimed.add(it.platformId) }

        val root = RomFolderEntry.Root(
            treeUri = treeUri,
            name = rawPath?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                ?: fallbackName(treeUri),
            linked = SafGrants.linkStatus(treeUri, persistedReadUris) == FolderLinkStatus.LINKED,
            consoleCount = mine.size,
            gameCount = mine.sumOf { it.gameCount },
        )
        listOf(root) + mine.sortedBy { it.displayName.lowercase() }.map { it.toConsoleEntry(true) }
    }.flatten()

    val orphans = cards
        .filter { it.platformId !in claimed }
        .sortedBy { it.displayName.lowercase() }
        .map { it.toConsoleEntry(false) }

    return grouped + orphans
}

private fun MemoryCard.toConsoleEntry(underRoot: Boolean) = RomFolderEntry.Console(
    platformId = platformId,
    displayName = displayName,
    romDirectory = romDirectory,
    gameCount = gameCount,
    underRoot = underRoot,
)
