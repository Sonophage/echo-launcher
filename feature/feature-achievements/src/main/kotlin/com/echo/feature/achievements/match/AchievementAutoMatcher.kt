package com.echo.feature.achievements.match

import com.echo.core.data.database.dao.AchievementMatchNoteDao
import com.echo.core.data.database.dao.ProviderGameLinkDao
import com.echo.core.data.database.entity.AchievementMatchNoteEntity
import com.echo.core.domain.achievement.AchievementProvider
import com.echo.core.domain.model.Game
import com.echo.core.domain.repository.GameRepository
import com.echo.feature.achievements.AchievementController
import com.echo.feature.achievements.provider.retro.RaHashLookup
import com.echo.feature.achievements.provider.retro.RaHashResolver
import com.echo.feature.achievements.provider.steam.SteamShortcut
import com.echo.core.data.steamgriddb.SteamGridDbApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class UnmatchedGame(
    val gameId: Long,
    val title: String,
    val platformId: String,
    val reason: String,
)

data class MatchReport(
    val matched: Int,
    val unmatched: List<UnmatchedGame>,
)

@Singleton
class AchievementAutoMatcher @Inject constructor(
    private val gameRepository: GameRepository,
    private val linkDao: ProviderGameLinkDao,
    private val matchNoteDao: AchievementMatchNoteDao,
    private val raHashResolver: RaHashResolver,
    private val repository: AchievementController,
    private val romReader: RomBytesReader,
    private val discOpener: DiscImageOpener,
    private val steamGridDb: SteamGridDbApi,
) {
    private sealed interface Outcome {
        data object Matched : Outcome
        data class Unmatched(val reason: String) : Outcome
    }

    private sealed interface HashAttempt {
        data class Hashed(val hash: String) : HashAttempt
        data object Unreadable : HashAttempt
        data object DiscUnreadable : HashAttempt
        data object DiscUnidentified : HashAttempt
        data object NoHasher : HashAttempt
    }

    suspend fun matchUnlinked(onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): MatchReport {
        val unlinked = gameRepository.observeGamesOnly().first()
            .filter { linkDao.getForGame(it.id) == null }

        matchNoteDao.clear()
        val now = System.currentTimeMillis()

        var matched = 0
        val unmatched = mutableListOf<UnmatchedGame>()
        Timber.d("auto-match: %d unlinked of %d games", unlinked.size, unlinked.size)
        unlinked.forEachIndexed { index, game ->
            onProgress(index, unlinked.size)
            val outcome = matchOne(game)
            Timber.d("auto-match [%s] %s -> %s", game.platformId, game.displayTitle, outcome)
            when (outcome) {
                Outcome.Matched -> matched++
                is Outcome.Unmatched -> {
                    unmatched += UnmatchedGame(game.id, game.displayTitle, game.platformId, outcome.reason)
                    matchNoteDao.upsert(AchievementMatchNoteEntity(game.id, outcome.reason, now))
                }
            }
        }
        onProgress(unlinked.size, unlinked.size)
        return MatchReport(matched, unmatched)
    }

    private suspend fun matchOne(game: Game): Outcome {
        if (game.platformId == "windows") return matchWindows(game)

        val consoleId = RaConsole.idFor(game.platformId)
            ?: return Outcome.Unmatched("RetroAchievements has no achievements for ${platformLabel(game.platformId)}")

        val attempt = attemptHash(game)

        if (attempt is HashAttempt.Hashed) {
            Timber.d("auto-match hash [%s] %s = %s", game.platformId, game.displayTitle, attempt.hash)
        }
        if (attempt !is HashAttempt.Hashed) return Outcome.Unmatched(reasonFor(attempt))

        return when (val lookup = raHashResolver.lookup(consoleId, attempt.hash)) {
            is RaHashLookup.Found -> {
                repository.linkManually(game.id, AchievementProvider.RETRO_ACHIEVEMENTS, lookup.gameId)
                Outcome.Matched
            }
            RaHashLookup.NotRegistered ->
                Outcome.Unmatched("ROM hash isn't registered on RetroAchievements")
            RaHashLookup.Unavailable ->
                Outcome.Unmatched(
                    "Couldn't load the RetroAchievements game list — check your connection and " +
                        "your RetroAchievements credentials in Settings, then sync again",
                )
        }
    }

    private suspend fun matchWindows(game: Game): Outcome {
        val steam = runCatching { matchSteam(game) }.getOrElse { e ->
            if (e is CancellationException) throw e
            return Outcome.Unmatched("Couldn't reach the Steam store, check your connection and sync again")
        }
        if (steam is Outcome.Unmatched) {
            return Outcome.Unmatched(
                "Not found on Steam (a DRM-free or non-Steam copy has no achievement data)",
            )
        }
        return steam
    }

    private suspend fun matchSteam(game: Game): Outcome {
        SteamShortcut.appIdFrom(game)?.let { appId ->
            repository.linkManually(game.id, AchievementProvider.STEAM, appId)
            return Outcome.Matched
        }
        game.steamGridDbId?.let { sgdbId ->
            steamGridDb.getSteamAppId(sgdbId)?.let { appId ->
                repository.linkManually(game.id, AchievementProvider.STEAM, appId)
                return Outcome.Matched
            }
        }

        for (title in steamTitleCandidates(game)) {
            if (repository.resolveSteamLink(game.id, title) != null) return Outcome.Matched
        }
        return Outcome.Unmatched("Not found on Steam (no embedded appid, no SteamGridDB or title match)")
    }

    private fun steamTitleCandidates(game: Game): List<String> =
        listOfNotNull(game.title, game.scrapedTitle, game.displayTitle)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    private suspend fun attemptHash(game: Game): HashAttempt {
        if (RaRomHasher.isSupported(game.platformId)) {
            if (game.platformId == "nds" && !game.isZippedRom()) {
                val source = discOpener.openRawSource(game) ?: return HashAttempt.Unreadable
                val hash = source.use { RaRomHasher.hashNds(it) } ?: return HashAttempt.Unreadable
                return HashAttempt.Hashed(hash)
            }
            val bytes = romReader.read(game) ?: return HashAttempt.Unreadable
            val hash = RaRomHasher.hash(game.platformId, bytes) ?: return HashAttempt.Unreadable
            return HashAttempt.Hashed(hash)
        }

        if (game.isChdImage()) {
            val image = discOpener.openChd(game) ?: return HashAttempt.DiscUnreadable
            val hash = image.use { hashCdDiscImage(game.platformId, it) }
            return if (hash == null) HashAttempt.DiscUnidentified else HashAttempt.Hashed(hash)
        }
        if (RaNintendoDiscHasher.isSupported(game.platformId)) {
            val source = discOpener.openRawSource(game) ?: return HashAttempt.DiscUnreadable
            val hash = source.use { RaNintendoDiscHasher.hash(game.platformId, it) }
            return if (hash == null) HashAttempt.DiscUnidentified else HashAttempt.Hashed(hash)
        }
        if (RaSegaDiscHasher.isSupported(game.platformId)) {
            val image = discOpener.openRawCd(game) ?: return HashAttempt.DiscUnreadable
            val hash = image.use { RaSegaDiscHasher.hash(it) }
            return if (hash == null) HashAttempt.DiscUnidentified else HashAttempt.Hashed(hash)
        }
        if (RaDreamcastHasher.isSupported(game.platformId)) {
            val image = discOpener.openGdi(game) ?: return HashAttempt.DiscUnreadable
            val hash = image.use { RaDreamcastHasher.hash(it) }
            return if (hash == null) HashAttempt.DiscUnidentified else HashAttempt.Hashed(hash)
        }
        if (RaDiscHasher.isSupported(game.platformId)) {
            val image = discOpener.open(game) ?: return HashAttempt.DiscUnreadable
            val hash = image.use { RaDiscHasher.hash(game.platformId, it) }
            return if (hash == null) HashAttempt.DiscUnidentified else HashAttempt.Hashed(hash)
        }
        return HashAttempt.NoHasher
    }

    private fun hashCdDiscImage(platformId: String, image: DiscImage): String? = when {
        RaSegaDiscHasher.isSupported(platformId) -> RaSegaDiscHasher.hash(image)
        RaDreamcastHasher.isSupported(platformId) -> RaDreamcastHasher.hash(image)
        RaDiscHasher.isSupported(platformId) -> RaDiscHasher.hash(platformId, image)
        else -> null
    }

    private fun Game.isChdImage(): Boolean =
        romPath?.endsWith(".chd", ignoreCase = true) == true ||
            romUri?.endsWith(".chd", ignoreCase = true) == true

    private fun Game.isZippedRom(): Boolean =
        romPath?.endsWith(".zip", ignoreCase = true) == true ||
            romUri?.endsWith(".zip", ignoreCase = true) == true

    private fun reasonFor(attempt: HashAttempt): String = when (attempt) {
        is HashAttempt.Hashed -> "ROM hash isn't registered on RetroAchievements"
        HashAttempt.Unreadable -> "Couldn't read the ROM file"
        HashAttempt.DiscUnreadable -> "Unsupported disc image (e.g. NKit, CHD, or compressed) — can't hash"
        HashAttempt.DiscUnidentified -> "Couldn't find the disc's boot executable"
        HashAttempt.NoHasher -> "Disc hashing for this system isn't supported yet"
    }

    private fun platformLabel(platformId: String): String = when (platformId) {
        "x360" -> "Xbox 360"
        "xbox" -> "Xbox"
        "xboxone" -> "Xbox One"
        "psvita" -> "PS Vita"
        "ps4" -> "PlayStation 4"
        "n3ds" -> "Nintendo 3DS"
        "wiiu" -> "Wii U"
        "switch" -> "Nintendo Switch"
        else -> "this system ($platformId)"
    }
}
