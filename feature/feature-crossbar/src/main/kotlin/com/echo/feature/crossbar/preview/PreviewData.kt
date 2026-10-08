package com.echo.feature.crossbar.preview

import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.domain.model.Category
import com.echo.core.domain.model.CategoryType
import com.echo.feature.crossbar.viewmodel.CrossbarItem
import com.echo.feature.crossbar.viewmodel.CrossbarItemType
import com.echo.feature.crossbar.viewmodel.CrossbarUiState

object PreviewData {
    val categories = listOf(
        Category(BuiltInCategory.SETTINGS, "Settings", "ic_settings", type = CategoryType.BUILT_IN, position = 0),
        Category("photos",                 "Photo",    "ic_photos",   type = CategoryType.BUILT_IN, position = 1),
        Category("music",                  "Music",    "ic_music",    type = CategoryType.BUILT_IN, position = 2),
        Category("videos",                 "Video",    "ic_videos",   type = CategoryType.BUILT_IN, position = 3),
        Category(BuiltInCategory.GAMES,    "Game",     "ic_games",    type = CategoryType.BUILT_IN, position = 4),
        Category("network",                "Network",  "ic_network",  type = CategoryType.BUILT_IN, position = 5),
    )

    val ps2Games = listOf(
        CrossbarItem("1",  "Shadow of the Colossus",  subtitle = "PS2 · 12h 34m played"),
        CrossbarItem("2",  "God of War",               subtitle = "PS2 · 8h 02m played"),
        CrossbarItem("3",  "Gran Turismo 4",            subtitle = "PS2 · 24h 11m played"),
        CrossbarItem("4",  "Kingdom Hearts",            subtitle = "PS2"),
        CrossbarItem("5",  "Ico",                       subtitle = "PS2"),
        CrossbarItem("6",  "Silent Hill 2",             subtitle = "PS2"),
        CrossbarItem("7",  "Metal Gear Solid 3",        subtitle = "PS2 · Last played yesterday"),
        CrossbarItem("8",  "Devil May Cry 3",           subtitle = "PS2"),
        CrossbarItem("9",  "Ratchet & Clank",           subtitle = "PS2"),
        CrossbarItem("10", "Jak and Daxter",            subtitle = "PS2"),
    )

    val gbaGames = listOf(
        CrossbarItem("20", "Pokémon FireRed",           subtitle = "GBA · 47h played"),
        CrossbarItem("21", "The Legend of Zelda: Minish Cap", subtitle = "GBA"),
        CrossbarItem("22", "Metroid Fusion",            subtitle = "GBA"),
        CrossbarItem("23", "Castlevania: Aria of Sorrow", subtitle = "GBA"),
        CrossbarItem("24", "Fire Emblem",               subtitle = "GBA · 31h played"),
        CrossbarItem("25", "Golden Sun",                subtitle = "GBA"),
    )

    val emptyItems = emptyList<CrossbarItem>()

    val platformFolders = listOf(
        CrossbarItem("all_games",       "All Games",                   subtitle = "Total Games 73", type = CrossbarItemType.ALL_GAMES),
        CrossbarItem("platform_psp",    "PSP",                         subtitle = "24 Games", platformId = "psp", type = CrossbarItemType.MEMORY_CARD),
        CrossbarItem("platform_ps2",    "PlayStation 2",               subtitle = "32 Games", platformId = "ps2", type = CrossbarItemType.MEMORY_CARD),
        CrossbarItem("platform_n64",    "Nintendo 64",                 subtitle = "17 Games", platformId = "n64", type = CrossbarItemType.MEMORY_CARD),
    )

    val defaultState = CrossbarUiState(
        categories            = categories,
        selectedCategoryIndex = 4,
        currentItems          = platformFolders,
        selectedItemIndex     = 0,
        showBootSequence      = false,
    )

    val emptyLibraryState = CrossbarUiState(
        categories            = categories,
        selectedCategoryIndex = 4,
        currentItems          = emptyItems,
        showBootSequence      = false,
    )

    val bootState = CrossbarUiState(
        categories       = categories,
        currentItems     = ps2Games,
        showBootSequence = true,
    )
}
