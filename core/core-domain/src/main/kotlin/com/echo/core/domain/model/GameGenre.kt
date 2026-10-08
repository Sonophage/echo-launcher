package com.echo.core.domain.model

// owner, 2026-10-08: one short list every game's genre maps onto, so a filter has a handful of choices. The
// scrapers' text ("Role Playing Game", "Platform / Shooter Scrolling", "Beat'em Up") is kept as it is; this is
// what it reads as, and an edit (genre_override) names one of these
enum class GameGenre(val label: String) {
    RPG("RPG"),
    ACTION("Action"),
    ADVENTURE("Adventure"),
    PLATFORMER("Platformer"),
    SHOOTER("Shooter"),
    FIGHTING("Fighting"),
    RACING("Racing"),
    PUZZLE("Puzzle"),
    STRATEGY("Strategy"),
    SPORTS("Sports"),
    SIMULATION("Simulation"),
    OTHER("Other");

    companion object {
        fun fromName(name: String?): GameGenre? = name?.let { n -> entries.firstOrNull { it.name == n } }
    }
}

// the first rule that matches wins, so the more particular ones come first: "Action RPG" is an RPG,
// "Platform / Shooter Scrolling" a platformer, "Action / Adventure" action
private val GENRE_RULES: List<Pair<GameGenre, List<String>>> = listOf(
    GameGenre.RPG to listOf("rpg", "role playing", "role-playing", "dungeon crawl"),
    GameGenre.PLATFORMER to listOf("platform"),
    GameGenre.FIGHTING to listOf("fight", "versus"),
    GameGenre.SHOOTER to listOf("shoot", "fps", "shmup", "lightgun", "run and gun", "run & gun"),
    GameGenre.RACING to listOf("racing", "driving", "kart"),
    GameGenre.PUZZLE to listOf("puzzle"),
    GameGenre.SPORTS to listOf("sport", "soccer", "football", "golf", "tennis", "basketball", "baseball", "hockey", "skate"),
    GameGenre.STRATEGY to listOf("strateg", "tactic", "tower defense"),
    GameGenre.SIMULATION to listOf("simulat", "management"),
    GameGenre.ACTION to listOf("action", "beat'em", "beat 'em", "beat em", "hack and slash", "brawler"),
    GameGenre.ADVENTURE to listOf("adventure", "point and click", "visual novel", "survival horror", "interactive movie"),
)

fun genreOf(raw: String?): GameGenre? {
    val text = raw?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
    return GENRE_RULES.firstOrNull { (_, words) -> words.any { it in text } }?.first ?: GameGenre.OTHER
}

// the genre a game shows and filters by: its edit, else what the scraped text reads as
fun effectiveGenre(raw: String?, override: String?): GameGenre? = GameGenre.fromName(override) ?: genreOf(raw)
