package com.echo.feature.artwork

import android.content.Context
import coil3.ImageLoader
import coil3.request.ImageRequest
import com.echo.core.data.database.dao.GameDao
import com.echo.core.data.database.dao.SsMediaCacheDao
import com.echo.core.data.database.entity.GameEntity
import com.echo.core.data.database.entity.SsMediaCacheEntity
import com.echo.feature.artwork.api.SsMediaSelection
import com.echo.feature.artwork.api.IgdbApi
import com.echo.feature.artwork.api.IgdbGameInfo
import com.echo.feature.artwork.api.ScrapeOptions
import com.echo.feature.artwork.api.ScreenScraperApi
import com.echo.feature.artwork.api.SteamAppArt
import com.echo.feature.artwork.api.SteamAppDetails
import com.echo.feature.artwork.api.SteamStoreApi
import com.echo.feature.artwork.api.steamAppArt
import com.echo.feature.artwork.api.steamAppIdOf
import com.echo.core.data.steamgriddb.SgdbApiKeyProvider
import com.echo.feature.artwork.api.SsGameInfo
import com.echo.core.data.steamgriddb.SteamGridDbApi
import com.echo.feature.artwork.rom.RomHasher
import com.echo.feature.artwork.rom.RomIdentity
import com.echo.feature.artwork.store.ArtworkKind
import com.echo.feature.artwork.store.ArtworkStore
import com.echo.feature.artwork.store.ArtworkTempIO
import com.echo.feature.artwork.video.VideoSnapTranscoder
import io.ktor.client.HttpClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import timber.log.Timber
import com.echo.feature.artwork.api.SsFailureReason
import javax.inject.Inject
import javax.inject.Singleton

data class MetadataFetchResult(
    val success: Boolean,
    val source: String,
    val message: String,
    val scrapedTitle: String? = null,
)

data class MetadataCandidates(
    val gameEntity: GameEntity?,

    val bestTitle: String,
    val ssInfo: SsGameInfo?,
    val romIdentity: RomIdentity?,

    val usedSsCache: Boolean,
    val cachedSsId: Long?,
    val igdbInfo: IgdbGameInfo?,
    val sgdbGameId: Long?,
    val sgdbGridUrl: String?,
    val sgdbHeroUrl: String?,
    val sgdbLogoUrl: String?,

    val steamDetails: SteamAppDetails?,

    val steamArt: SteamAppArt?,
) {
    val isEmpty: Boolean
        get() = ssInfo == null && igdbInfo == null && sgdbGridUrl == null && steamDetails == null
}

@Singleton
class MetadataRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameDao: GameDao,
    private val screenScraper: ScreenScraperApi,
    private val romHasher: RomHasher,
    private val steamGridDb: SteamGridDbApi,
    private val steamStoreApi: SteamStoreApi,
    private val igdbApi: IgdbApi,
    private val sgdbKeyProvider: SgdbApiKeyProvider,
    private val imageLoader: ImageLoader,
    private val artworkStore: ArtworkStore,
    private val httpClient: HttpClient,
    private val videoSnapTranscoder: VideoSnapTranscoder,
    private val ssMediaCacheDao: SsMediaCacheDao,
) {
    @Volatile private var ssStopped = false
    @Volatile private var ssUnhashedStopped = false

    @Volatile var ssStopReason: SsFailureReason? = null
        private set

    fun resetSsBatchGuards() {
        ssStopped = false
        ssUnhashedStopped = false
        ssStopReason = null
    }

    suspend fun fetchCandidates(
        gameId: Long,
        title: String,
        platformId: String,
        romPath: String?,
        options: ScrapeOptions = ScrapeOptions(),
        onAssetProgress: ((source: String, asset: String) -> Unit)? = null,
    ): MetadataCandidates {
        val gameEntity = gameDao.getById(gameId)
        val bestTitle = gameEntity?.userTitleOverride?.takeIf { it.isNotBlank() }
            ?: gameEntity?.scrapedTitle?.takeIf { it.isNotBlank() }
            ?: title

        if (bestTitle != title) {
            Timber.d("MetadataRepository: using bestTitle='$bestTitle' instead of raw title='$title'")
        }

        var steamDetails: SteamAppDetails? = null
        var steamArt: SteamAppArt? = null
        steamAppIdOf(gameEntity?.storefront, gameEntity?.storefrontGameId)?.let { appId ->
            onAssetProgress?.invoke("Steam", "Reading store page…")
            steamDetails = runCatching { steamStoreApi.appDetails(appId) }
                .onFailure { Timber.w(it, "Steam store error for app $appId") }
                .getOrNull()

            steamArt = steamAppArt(appId)
        }

        var ssInfo: SsGameInfo? = null
        var romIdentity: RomIdentity? = null
        var usedSsCache = false
        val cachedSsId = gameEntity?.ssId
        if (screenScraper.isEnabled() && !ssStopped) {
            if (!options.bypassSsCache && cachedSsId != null && gameEntity.description != null) {
                ssMediaCacheDao.get(cachedSsId)?.let { row ->
                    SsMediaSelection.decode(row.mediasJson)?.let { medias ->
                        ssInfo = SsMediaSelection.infoFromCache(cachedSsId, medias)
                        usedSsCache = true
                        Timber.i("SS media cache hit (ssId=$cachedSsId) — skipping jeuInfos for '$bestTitle'")
                    }
                }
            }
            if (!usedSsCache) {
            onAssetProgress?.invoke("ScreenScraper", "Hashing ROM…")
            romIdentity = romHasher.identify(gameEntity?.romPath ?: romPath, gameEntity?.romUri)
            val skipUnhashed = ssUnhashedStopped && romIdentity.crc32 == null && gameEntity?.ssId == null
            if (!skipUnhashed) {
                onAssetProgress?.invoke("ScreenScraper", "Searching…")
                val ssResult = screenScraper.fetchGameInfo(
                    platformId = platformId,
                    rom        = romIdentity,
                    ssGameId   = gameEntity?.ssId,
                )
                if (ssResult.isBatchStopper) {
                    ssStopped = true

                    if (ssStopReason == null) ssStopReason = ssResult.diagnostics.failureReason
                }
                if (ssResult.stopsUnhashedLookups) ssUnhashedStopped = true
                ssInfo = ssResult.info

                ssResult.info?.let { info ->
                    val id = info.ssId
                    if (id != null && info.medias.isNotEmpty()) {
                        ssMediaCacheDao.upsert(
                            SsMediaCacheEntity(id, SsMediaSelection.encode(info.medias), System.currentTimeMillis())
                        )
                    }
                }
            }
            }
        }

        var igdbInfo: IgdbGameInfo? = null
        if (igdbApi.hasCredentials() && !options.metadataOnly) {
            val needsBoxArt = ssInfo?.artworkUrl == null
            val needsHero   = ssInfo?.heroUrl == null
            if (needsBoxArt || needsHero) {
                onAssetProgress?.invoke("IGDB", "Searching…")
                igdbInfo = runCatching {
                    igdbApi.fetchGameInfo(platformId, bestTitle)
                }.onFailure { Timber.w(it, "IGDB error for '$bestTitle'") }.getOrNull()
            }
        }

        val sgdbKey = sgdbKeyProvider.getKey()
        var sgdbGridUrl: String? = null
        var sgdbHeroUrl: String? = null
        var sgdbLogoUrl: String? = null
        var sgdbGameId:  Long?   = null

        if (!sgdbKey.isNullOrBlank() && !options.metadataOnly) {
            onAssetProgress?.invoke("SteamGridDB", "Searching…")
            runCatching {
                val match = steamGridDb.searchGame(bestTitle).getOrNull()?.firstOrNull()
                if (match != null) {
                    sgdbGameId  = match.id
                    sgdbGridUrl = steamGridDb.getBestGridUrl(match.id)
                    sgdbHeroUrl = steamGridDb.getBestHeroUrl(match.id)
                    if (options.downloadClearLogos) sgdbLogoUrl = steamGridDb.getBestLogoUrl(match.id)
                }
            }.onFailure { Timber.w(it, "SteamGridDB error for '$bestTitle'") }
        }

        return MetadataCandidates(
            gameEntity  = gameEntity,
            bestTitle   = bestTitle,
            ssInfo      = ssInfo,
            romIdentity = romIdentity,
            usedSsCache = usedSsCache,
            cachedSsId  = cachedSsId,
            igdbInfo    = igdbInfo,
            sgdbGameId  = sgdbGameId,
            sgdbGridUrl = sgdbGridUrl,
            sgdbHeroUrl = sgdbHeroUrl,
            sgdbLogoUrl = sgdbLogoUrl,
            steamDetails = steamDetails,
            steamArt = steamArt,
        )
    }

    suspend fun fetchForGame(
        gameId: Long,
        title: String,
        platformId: String,
        romPath: String?,
        options: ScrapeOptions = ScrapeOptions(),
        onAssetProgress: ((source: String, asset: String) -> Unit)? = null,
    ): MetadataFetchResult {
        val candidates = fetchCandidates(gameId, title, platformId, romPath, options, onAssetProgress)

        if (candidates.isEmpty) {
            Timber.i("No metadata found for '${candidates.bestTitle}'")
            return MetadataFetchResult(false, "none", "Not found on any source")
        }

        val gameEntity  = candidates.gameEntity
        val bestTitle   = candidates.bestTitle
        val ssInfo      = candidates.ssInfo
        val romIdentity = candidates.romIdentity
        val usedSsCache = candidates.usedSsCache
        val cachedSsId  = candidates.cachedSsId
        val igdbInfo    = candidates.igdbInfo
        val sgdbGameId  = candidates.sgdbGameId
        val sgdbGridUrl = candidates.sgdbGridUrl
        val sgdbHeroUrl = candidates.sgdbHeroUrl
        val sgdbLogoUrl = candidates.sgdbLogoUrl
        val steamDetails = candidates.steamDetails
        val steamArt    = candidates.steamArt

        val finalBoxArtUrl = steamArt?.boxArtUrl ?: ssInfo?.artworkUrl ?: igdbInfo?.artworkUrl ?: sgdbGridUrl
        val finalBannerUrl = steamArt?.heroUrl ?: ssInfo?.heroUrl ?: igdbInfo?.heroUrl ?: sgdbHeroUrl
        val finalLogoUrl = if (options.downloadClearLogos)
            steamArt?.logoUrl ?: ssInfo?.logoUrl ?: igdbInfo?.logoUrl ?: sgdbLogoUrl
        else null

        val src = primarySource(ssInfo, igdbInfo, sgdbGridUrl, steamDetails)

        val failedSsKinds = mutableSetOf<ArtworkKind>()
        suspend fun savedTracked(kind: ArtworkKind, url: String, fromSs: Boolean): String? {
            val path = artworkStore.saveFromUrl(gameId, kind, url)
            if (path == null && fromSs && usedSsCache) failedSsKinds += kind
            return path
        }

        var backgroundPath: String? = null
        var logoPath: String? = null

        if (!options.metadataOnly) {
        onAssetProgress?.invoke(src, "Background")
        backgroundPath = finalBannerUrl?.let { savedTracked(ArtworkKind.BACKGROUND, it, fromSs = it == ssInfo?.heroUrl) }
            ?: finalBoxArtUrl?.let { savedTracked(ArtworkKind.BACKGROUND, it, fromSs = it == ssInfo?.artworkUrl) }

        if (options.downloadClearLogos) onAssetProgress?.invoke(src, "Logo")
        logoPath = finalLogoUrl?.let { savedTracked(ArtworkKind.LOGO, it, fromSs = it == ssInfo?.logoUrl) }

        ssInfo?.screenshotUrl?.let {
            onAssetProgress?.invoke("ScreenScraper", "Screenshot")
            savedTracked(ArtworkKind.SCREENSHOT, it, fromSs = true)
        }

        if (options.downloadManuals) ssInfo?.manualUrl?.let {
            onAssetProgress?.invoke("ScreenScraper", "Manual")
            savedTracked(ArtworkKind.MANUAL, it, fromSs = true)
        }
        if (options.downloadVideoSnaps) {
            val snapUrl = ssInfo?.videoUrl
            val rawUrl  = ssInfo?.videoRawUrl

            val rawFile = rawUrl?.let {
                onAssetProgress?.invoke("ScreenScraper", "Video")
                ArtworkTempIO.downloadToTemp(httpClient, context.cacheDir, ArtworkKind.VIDEO, it)
            }

            if (rawFile != null) {
                val vCopy = java.io.File.createTempFile("vid_", ".mp4", context.cacheDir)
                runCatching { rawFile.copyTo(vCopy, overwrite = true) }
                    .onSuccess { artworkStore.saveFromFile(gameId, ArtworkKind.VIDEO, vCopy) }
                    .onFailure { vCopy.delete() }
            }

            when {
                snapUrl != null -> {
                    onAssetProgress?.invoke("ScreenScraper", "Icon Video")
                    savedTracked(ArtworkKind.ICON1, snapUrl, fromSs = true)
                }
                rawFile != null -> {
                    onAssetProgress?.invoke("ScreenScraper", "Icon Video (converting)")
                    val snap = java.io.File.createTempFile("snap_", ".mp4", context.cacheDir)
                    val ok = runCatching { videoSnapTranscoder.transcode(rawFile, snap) }
                        .onFailure { Timber.w(it, "Snap transcode crashed") }
                        .getOrDefault(false)
                    if (ok) artworkStore.saveFromFile(gameId, ArtworkKind.ICON1, snap) else snap.delete()
                }
            }
            rawFile?.delete()
        }
        }

        val newScrapedTitle = ssInfo?.title ?: steamDetails?.title
        val existingOverride = gameEntity?.userTitleOverride

        gameDao.updateMetadata(
            id           = gameId,

            description  = ssInfo?.description ?: steamDetails?.description,
            developer    = ssInfo?.developer ?: steamDetails?.developer,
            publisher    = ssInfo?.publisher ?: steamDetails?.publisher,
            releaseYear  = ssInfo?.releaseYear ?: steamDetails?.releaseYear,
            genre        = ssInfo?.genre ?: steamDetails?.genre,

            artworkUri   = if (options.metadataOnly) null else backgroundPath ?: finalBannerUrl ?: finalBoxArtUrl,
            logoUri      = if (options.metadataOnly) null else logoPath ?: finalLogoUrl,
            players      = ssInfo?.players,
            ageRating    = ssInfo?.ageRating,
            franchise    = ssInfo?.franchise,
            communityRating = ssInfo?.communityRating,
            releaseDate  = ssInfo?.releaseDate,
            ssId         = ssInfo?.ssId,
            steamGridDbId = sgdbGameId,
            romCrc32     = romIdentity?.crc32,
        )

        if (newScrapedTitle != null && existingOverride == null) {
            gameDao.fillScrapedTitleIfMissing(gameId, newScrapedTitle)
        }

        if (usedSsCache && failedSsKinds.isNotEmpty() && cachedSsId != null) {
            Timber.w("SS cached URLs failed for $failedSsKinds — refreshing cache, retrying once")
            ssMediaCacheDao.delete(cachedSsId)
            val fresh = screenScraper.fetchGameInfo(platformId, rom = null, ssGameId = cachedSsId).info
            if (fresh != null) {
                if (fresh.medias.isNotEmpty()) {
                    ssMediaCacheDao.upsert(
                        SsMediaCacheEntity(cachedSsId, SsMediaSelection.encode(fresh.medias), System.currentTimeMillis())
                    )
                }
                for (kind in failedSsKinds) {
                    val url = when (kind) {
                        ArtworkKind.BACKGROUND     -> fresh.heroUrl ?: fresh.artworkUrl
                        ArtworkKind.LOGO           -> fresh.logoUrl
                        ArtworkKind.SCREENSHOT     -> fresh.screenshotUrl
                        ArtworkKind.MANUAL         -> fresh.manualUrl
                        ArtworkKind.ICON1          -> fresh.videoUrl
                        else                       -> null
                    } ?: continue
                    val path = artworkStore.saveFromUrl(gameId, kind, url) ?: continue
                    when (kind) {
                        ArtworkKind.BACKGROUND     -> gameDao.updateMetadata(gameId, artworkUri = path)
                        ArtworkKind.LOGO           -> gameDao.updateMetadata(gameId, logoUri = path)
                        else                       -> Unit
                    }
                }
            }
        }

        prewarm(backgroundPath, logoPath)
        if (!options.metadataOnly) fetchHorizontalIcon(gameId, bestTitle, sgdbGameId)

        Timber.i("Metadata from $src: '$bestTitle' (scrapedTitle='$newScrapedTitle')")
        return MetadataFetchResult(true, src, "Found via $src", scrapedTitle = newScrapedTitle)
    }

    suspend fun fetchMissingMetadata(onProgress: (current: Int, total: Int) -> Unit) {
        resetSsBatchGuards()
        val games = gameDao.getGamesWithoutArtwork()
        games.forEachIndexed { index, game ->
            onProgress(index + 1, games.size)
            fetchForGame(
                gameId     = game.id,
                title      = game.title,
                platformId = game.platformId,
                romPath    = game.romPath,
            )
            if (index < games.size - 1) delay(500)
        }
    }

    private fun primarySource(
        ss: SsGameInfo?,
        igdb: IgdbGameInfo?,
        sgdbUrl: String?,
        steam: SteamAppDetails?,
    ): String =
        when {
            ss != null      -> "screenscraper"

            steam != null   -> "steam"
            igdb != null    -> "igdb"
            sgdbUrl != null -> "steamgriddb"
            else            -> "mixed"
        }

    private suspend fun fetchHorizontalIcon(gameId: Long, title: String, knownSgdbId: Long?) {
        val key = sgdbKeyProvider.getKey()
        if (key.isNullOrBlank()) return
        runCatching {
            val id = knownSgdbId
                ?: steamGridDb.searchGame(title).getOrNull()?.firstOrNull()?.id
                ?: return
            val url  = steamGridDb.getBestHorizontalGridUrl(id) ?: return
            val path = artworkStore.saveFromUrl(gameId, ArtworkKind.ICON, url) ?: url
            gameDao.updateIconUri(gameId, path)
            prewarm(path)
            Timber.d("Horizontal icon from SteamGridDB: '$title'")
        }
    }

    private fun prewarm(vararg urls: String?) {
        urls.filterNotNull().forEach { url ->
            imageLoader.enqueue(ImageRequest.Builder(context).data(url).build())
        }
    }
}
