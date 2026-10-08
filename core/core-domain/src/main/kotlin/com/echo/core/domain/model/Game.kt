package com.echo.core.domain.model

data class Game(
    val id: Long = 0,
    val title: String,
    val platformId: String,
    val romPath: String?          = null,

    val romUri: String?           = null,

    val discSetKey: String? = null,
    val discNumber: Int? = null,
    val isDiscPrimary: Boolean = false,

    val region: GameRegion? = null,
    val packageName: String?      = null,
    val emulatorPackage: String?  = null,
    val artworkUri: String?       = null,
    val logoUri: String?          = null,
    val iconUri: String?          = null,

    val description: String?      = null,
    val developer: String?        = null,
    val publisher: String?        = null,
    val releaseYear: Int?         = null,
    val genre: String?            = null,
    val genreOverride: String?    = null,

    val players: String?          = null,
    val ageRating: String?        = null,
    val franchise: String?        = null,
    val communityRating: Float?   = null,
    val releaseDate: String?      = null,
    val steamGridDbId: Long?      = null,

    val ssId: Long?               = null,
    val igdbId: Long?             = null,

    val romCrc32: String?         = null,

    val artworkKey: String?       = null,
    val isFavorite: Boolean = false,
    val favoriteSortOrder: Int = 0,
    val totalPlayTimeMillis: Long = 0,
    val lastPlayedAt: Long? = null,

    val dateAdded: Long?          = null,

    val playState: String?        = null,
    val userNote: String?   = null,
    val isManualEntry: Boolean = false,

    val scrapedTitle: String?      = null,

    val userTitleOverride: String? = null,

    val contentType: GameContentType = GameContentType.GAME,

    val shortcutId: String? = null,

    val launchIntentUri: String? = null,

    val launchToken: String? = null,

    val storefront: String? = null,
    val storefrontGameId: String? = null,

    val isMissing: Boolean = false,

    val lastSeenAt: Long? = null,
) {
    val displayTitle: String get() = userTitleOverride ?: scrapedTitle ?: title

    val discFaceUri: String? get() = listOfNotNull(artworkUri, iconUri)
        .firstOrNull { it.isNotBlank() }
}

// owner, 2026-10-05: a game's cover is its icon slot, for every system; the main art is only the fallback. The
// main art is often a wide background (ES-DE's miximages, or fanart), and the owner fills the icon slot with the
// covers he picks (Skyrim, Sekiro and The Witcher 3 already hold their SteamGridDB grids there)
fun coverArtOf(icon: String?, main: String?): String? = icon?.takeIf { it.isNotBlank() } ?: main?.takeIf { it.isNotBlank() }

// owner, 2026-10-05: an installed app that is itself a game in the library (an Android game) wears that game's
// cover, its icon slot first. An app that only launches library games (GameNative running Steam games, each with its
// own shortcut) is not any one of them, so it keeps its own icon
fun appCovers(games: List<Game>): Map<String, String> =
    games.filter { it.packageName != null && it.launchIntentUri == null }
        .groupBy { it.packageName!! }
        .mapNotNull { (pkg, owned) ->
            owned.singleOrNull()?.let { g -> coverArtOf(g.iconUri, g.artworkUri)?.let { pkg to it } }
        }
        .toMap()
