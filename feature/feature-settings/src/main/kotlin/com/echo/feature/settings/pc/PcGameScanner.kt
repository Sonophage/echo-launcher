package com.echo.feature.settings.pc

import com.echo.core.domain.model.PlatformIds.WINDOWS as WINDOWS_PLATFORM_ID

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.echo.core.common.security.ShortcutIntentSanitizer
import com.echo.core.data.database.dao.ArtworkRecordDao
import com.echo.feature.artwork.api.ArtworkImportManager
import com.echo.feature.artwork.match.MatchProvider
import com.echo.core.data.repository.RomRootRepository
import com.echo.core.data.repository.WindowsLibrarySetup
import com.echo.core.data.repository.WindowsSetupState
import com.echo.core.data.model.StorefrontIdentity
import com.echo.core.domain.model.Game
import com.echo.core.domain.model.GameContentType
import com.echo.core.domain.repository.GameRepository
import com.echo.feature.launcher.PcLauncherAdapters
import com.echo.feature.launcher.PcLauncherCatalog
import com.echo.feature.launcher.PcLauncherType
import com.echo.feature.launcher.PcShortcutImporter
import com.echo.feature.library.scanner.PcExportFile
import com.echo.feature.library.scanner.RomScanner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import com.echo.feature.library.scanner.cleanRomTitle

data class PcScanReport(
    val setup: WindowsSetupState?,
    val exportsAdded: Int,
    val pinsReconciled: Int,
    val message: String,

    val restoredCreated: Int = 0,

    val restoredMatched: Int = 0,

    val restoreSkipped: Int = 0,

    val untrustedExports: Int = 0,

    val artworkClaims: Map<Triple<String, String, String>, Long> = emptyMap(),

    val artworkRelinkedGames: Int? = null,
) {
    val newGames: Int get() = exportsAdded + pinsReconciled + restoredCreated
}

@Singleton
class PcGameScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val windowsLibrarySetup: WindowsLibrarySetup,
    private val pcShortcutImporter: PcShortcutImporter,
    private val romScanner: RomScanner,
    private val gameRepository: GameRepository,
    private val artworkImportManager: ArtworkImportManager,
    private val artworkRecordDao: ArtworkRecordDao,
) {
    private val scanMutex = Mutex()

    suspend fun scan(overrideFolder: Uri? = null): PcScanReport = scanMutex.withLock { scanLocked(overrideFolder) }

    private suspend fun scanLocked(overrideFolder: Uri?): PcScanReport {
        val setup = runCatching { windowsLibrarySetup.ensure() }.getOrNull()
        if (overrideFolder == null && setup is WindowsSetupState.NoRomRoot) {
            return PcScanReport(
                setup, 0, 0,
                message = "Add a ROM Root first — ECHO creates <root>/windows/import for exported games.",
            )
        }

        val pm = context.packageManager
        val launchers = installedLaunchers(pm)

        val pins = runCatching { pcShortcutImporter.reconcilePinnedShortcuts() }
            .onFailure { Timber.e(it, "Pin reconcile failed") }
            .getOrDefault(0)

        var added = 0
        var alreadyInLibrary = 0
        var skipped = 0
        val importFolders = if (overrideFolder != null) {
            val treeUri = overrideFolder.toString()
            RomRootRepository.treeDocId(treeUri)?.let { listOf(treeUri to it) } ?: emptyList()
        } else {
            windowsLibrarySetup.importFolders()
        }
        val echoExports = mutableListOf<PcExportFile>()
        val droidDeckFiles = if (overrideFolder != null) emptyList() else droidDeckFiles(launchers)
        val fromDroidDeck = droidDeckFiles.mapTo(HashSet()) { it.uri }
        val scanned = importFolders.flatMap { (rootUri, importDocId) ->
            romScanner.scanPcFolder(rootUri, importDocId).filterNot { it.extension == DROIDDECK_EXTENSION }
        } + droidDeckFiles
        run {
            scanned.forEach { file ->

                if (file.extension == PcGameExportCodec.EXTENSION) {
                    echoExports += file
                    return@forEach
                }
                val launch = buildPcLaunch(file, pm, launchers, fromDroidDeck = file.uri in fromDroidDeck)
                if (launch == null) { skipped++; return@forEach }
                val intentUri = launch.intent.toUri(Intent.URI_INTENT_SCHEME)
                val sameLauncher = gameRepository.getByIntentUri(intentUri)
                    ?: findBySteamId(launch)
                    ?: findWindowsGame(launch.packageName, file.title)
                // owner, 2026-10-10: GameNative was uninstalled and DroidDeck has the same games; a game whose
                // launcher is gone moves to this one with its history, rather than being added twice
                val orphan = if (sameLauncher == null) findOrphanedSteamGame(launch, pm) else null
                val existing = sameLauncher ?: orphan
                if (existing == null) {
                    gameRepository.upsert(
                        Game(

                            title           = cleanRomTitle(file.title),
                            platformId      = WINDOWS_PLATFORM_ID,
                            packageName     = launch.packageName,
                            isManualEntry   = true,
                            contentType     = GameContentType.GAME,
                            launchIntentUri = intentUri,

                            storefront       = launch.storefront,
                            storefrontGameId = launch.storefrontGameId,
                        ),
                    )
                    added++
                } else {
                    gameRepository.updateStorefrontIdentity(
                        existing.id, launch.storefront, launch.storefrontGameId,
                    )
                    // a DroidDeck game keeps the launch DroidDeck reads now (its link, 2026-10-07)
                    if (orphan != null) {
                        gameRepository.attachLauncherHandle(existing.id, launch.packageName, null, intentUri)
                    } else if (launch.packageName == existing.packageName && existing.launchIntentUri != intentUri &&
                        file.uri in fromDroidDeck) {
                        gameRepository.attachLauncherHandle(existing.id, existing.packageName, null, intentUri)
                    }
                    alreadyInLibrary++
                }
            }
        }

        val restore = restoreFromEchoExports(echoExports, pm)
        val relink = relinkClaimedArtwork(restore.claims, restore.identitySeeds)

        runCatching { windowsLibrarySetup.ensure() }

        val pinNote = if (pins > 0) " $pins pinned shortcut(s) reconciled." else ""
        val restoreNote = buildString {
            if (restore.created + restore.matched > 0) {
                append(" Restored ${restore.created} and matched ${restore.matched} game(s) from .pfpgame files.")
            }
            if (restore.skipped > 0) append(" Skipped ${restore.skipped} .pfpgame file(s): launcher not installed, ambiguous, or pin not in the library.")
            if (restore.untrusted > 0) append(" ${restore.untrusted} .pfpgame file(s) ignored as unreadable or untrusted.")
        }
        val relinkNote = when (relink) {
            ArtworkRelink.NotNeeded -> ""
            is ArtworkRelink.Done -> " Reconnected exported artwork by name (${relink.gamesLinked} game(s) updated)."
            ArtworkRelink.FolderNotLinked -> " Link the artwork folder, then scan again to reconnect exported artwork."
            ArtworkRelink.Failed -> " Reconnecting exported artwork failed — see the log."
        }
        val message = when {
            importFolders.isEmpty() && pins == 0 ->
                "Couldn't read that folder. Pick the folder your launcher exports games into."
            added == 0 && alreadyInLibrary == 0 && skipped == 0 && pins == 0 && echoExports.isEmpty() ->
                "No exported PC games found in the selected folder."
            else ->
                "Imported $added PC game(s)" +
                    (if (skipped > 0) ", skipped $skipped (no matching launcher installed)" else "") +
                    "." + restoreNote + relinkNote + pinNote
        }
        Timber.i(
            "PC scan — importFolders=${importFolders.size} added=$added skipped=$skipped pins=$pins " +
                "pfpgame=${echoExports.size} restored=${restore.created}/${restore.matched} " +
                "restoreSkipped=${restore.skipped} untrusted=${restore.untrusted} claims=${restore.claims.size}",
        )
        return PcScanReport(
            setup, added, pins, message,
            restoredCreated = restore.created,
            restoredMatched = restore.matched,
            restoreSkipped = restore.skipped,
            untrustedExports = restore.untrusted,
            artworkClaims = restore.claims,
            artworkRelinkedGames = (relink as? ArtworkRelink.Done)?.gamesLinked,
        )
    }

    private sealed interface ArtworkRelink {
        data object NotNeeded : ArtworkRelink
        data object FolderNotLinked : ArtworkRelink
        data object Failed : ArtworkRelink
        data class Done(val gamesLinked: Int) : ArtworkRelink
    }

    private suspend fun relinkClaimedArtwork(
        claims: Map<Triple<String, String, String>, Long>,
        identitySeeds: List<com.echo.feature.artwork.portable.ArtworkIdentityIndex.Entry>,
    ): ArtworkRelink {
        if (claims.isEmpty()) return ArtworkRelink.NotNeeded
        val records = claims.values.toSet().associateWith { artworkRecordDao.getForGame(it) }
        if (PcGameArtworkClaims.unresolved(claims, records).isEmpty()) return ArtworkRelink.NotNeeded
        val result = runCatching { artworkImportManager.relinkLibrary(claims, identitySeeds) }
            .onFailure { Timber.e(it, "Relink after .pfpgame restore failed") }
        return when {
            result.isFailure -> ArtworkRelink.Failed

            result.getOrNull() == null -> ArtworkRelink.FolderNotLinked
            else -> ArtworkRelink.Done(result.getOrNull()!!.gamesLinked)
        }
    }

    private data class EchoRestore(
        val created: Int = 0,
        val matched: Int = 0,
        val skipped: Int = 0,
        val untrusted: Int = 0,
        val claims: Map<Triple<String, String, String>, Long> = emptyMap(),

        val identitySeeds: List<com.echo.feature.artwork.portable.ArtworkIdentityIndex.Entry> = emptyList(),
    )

    private suspend fun restoreFromEchoExports(files: List<PcExportFile>, pm: PackageManager): EchoRestore {
        if (files.isEmpty()) return EchoRestore()
        val games = gameRepository.getByPlatform(WINDOWS_PLATFORM_ID).toMutableList()
        val claims = PcGameArtworkClaims()
        var created = 0
        var matched = 0
        var skipped = 0
        var untrusted = 0
        for (file in files) {
            val export = when (val decoded = PcGameExportCodec.decode(file.idContent.orEmpty())) {
                is PcGameExportDecode.Valid -> decoded.export
                is PcGameExportDecode.Rejected -> {
                    Timber.w("PC scan — ignoring ${file.title}.pfpgame: this export file ${decoded.reason}")
                    untrusted++
                    continue
                }
            }
            val launch = if (export.isPin) null else checkLaunch(export, pm)
            when (val decision = PcGameImportPlanner.plan(export, launch, games)) {
                is PcGameImportDecision.Create -> {
                    val id = gameRepository.upsert(decision.game)
                    games += decision.game.copy(id = id)
                    claims.add(export, id)
                    created++
                }
                is PcGameImportDecision.Fill -> {
                    if (decision.changed) {
                        val original = games.first { it.id == decision.game.id }
                        applyFill(original, decision.game)
                        games.replaceAll { if (it.id == decision.game.id) decision.game else it }
                    }
                    claims.add(export, decision.game.id)
                    matched++
                }
                is PcGameImportDecision.Skip -> {
                    Timber.i("PC scan — skipping ${file.title}.pfpgame: ${decision.reason}")
                    if (decision.reason == PcGameImportSkip.UNTRUSTED_INTENT) untrusted++ else skipped++
                }
            }
        }
        return EchoRestore(created, matched, skipped, untrusted, claims.toMap(), claims.toIdentitySeeds())
    }

    private suspend fun applyFill(original: Game, filled: Game) {
        val id = filled.id
        if (filled.scrapedTitle != original.scrapedTitle) {
            gameRepository.updateScrapedTitle(id, filled.scrapedTitle)
        }
        if (filled.userTitleOverride != original.userTitleOverride) {
            gameRepository.updateUserTitleOverride(id, filled.userTitleOverride)
        }
        if (filled.storefront != original.storefront || filled.storefrontGameId != original.storefrontGameId) {
            gameRepository.updateStorefrontIdentity(id, filled.storefront, filled.storefrontGameId)
        }
        if (filled.ssId != original.ssId) {
            gameRepository.updateProviderMatch(id, MatchProvider.SCREENSCRAPER.name, filled.ssId)
        }
        if (filled.igdbId != original.igdbId) {
            gameRepository.updateProviderMatch(id, MatchProvider.IGDB.name, filled.igdbId)
        }
        if (filled.steamGridDbId != original.steamGridDbId) {
            gameRepository.updateProviderMatch(id, MatchProvider.STEAMGRIDDB.name, filled.steamGridDbId)
        }
    }

    private fun checkLaunch(export: PcGameExport, pm: PackageManager): LaunchCheck {
        val installed = runCatching { pm.getApplicationInfo(export.launcherPackage, 0) }.isSuccess
        val verified = installed && PcLauncherCatalog.isVerifiedPcLauncher(export.launcherPackage, pm)
        val intent = export.launchIntentUri?.let { runCatching { Intent.parseUri(it, Intent.URI_INTENT_SCHEME) }.getOrNull() }
        val sanitized = intent?.let { runCatching { ShortcutIntentSanitizer.sanitize(it, pm) }.getOrNull() }
        return LaunchCheck(
            launcherVerified = verified,
            intentPackage = intent?.component?.packageName ?: intent?.`package`,
            sanitizedIntentUri = sanitized?.toUri(Intent.URI_INTENT_SCHEME),
        )
    }

    data class LauncherExports(
        val files: List<PcExportFile>,
        val intentUris: Set<String>,
    )

    suspend fun launcherExports(): LauncherExports {
        val pm = context.packageManager
        val launchers = installedLaunchers(pm)
        val imported = windowsLibrarySetup.importFolders().flatMap { (rootUri, importDocId) ->
            romScanner.scanPcFolder(rootUri, importDocId)
        }.filterNot { it.extension == PcGameExportCodec.EXTENSION || it.extension == DROIDDECK_EXTENSION }
        val droidDeck = droidDeckFiles(launchers)
        val files = imported + droidDeck
        val intentUris = imported.mapNotNull { file -> buildPcLaunch(file, pm, launchers, fromDroidDeck = false) }
            .plus(droidDeck.mapNotNull { file -> buildPcLaunch(file, pm, launchers, fromDroidDeck = true) })
            .map { it.intent.toUri(Intent.URI_INTENT_SCHEME) }
            .toSet()
        return LauncherExports(files, intentUris)
    }

    private data class InstalledLaunchers(
        val droidDeck: String?,
        val gameNative: String?,
        val gameHub: String?,
        val winlator: String?,
    )

    private fun installedLaunchers(pm: PackageManager): InstalledLaunchers {
        fun installed(vararg pkgs: String) = pkgs.firstOrNull { runCatching { pm.getApplicationInfo(it, 0) }.isSuccess }
        return InstalledLaunchers(
            droidDeck = installed("com.droiddeck.launcher"),
            gameNative = installed("app.gamenative"),

            gameHub = PcLauncherCatalog.installedGameHubFamilyPackages(pm).firstOrNull(),
            winlator = installed("com.winlator", "com.winlator.cmod"),
        )
    }

    private data class PcLaunch(
        val intent: Intent,
        val launcherName: String,
        val packageName: String,
        val storefront: String? = null,
        val storefrontGameId: String? = null,
    )

    // DroidDeck writes its games into the windows folder itself, beside import: its own .droiddeck links
    // (owner, 2026-10-07), and since 0.3.2 a .steam file holding the Steam app id (seen on the Konker, 2026-10-10)
    private suspend fun droidDeckFiles(launchers: InstalledLaunchers): List<PcExportFile> =
        if (launchers.droidDeck == null) emptyList() else
            windowsLibrarySetup.windowsFolders().flatMap { (rootUri, docId) ->
                romScanner.scanPcFolder(rootUri, docId).filter { it.extension == DROIDDECK_EXTENSION || it.extension == "steam" }
            }.distinctBy { it.uri }

    private fun buildPcLaunch(
        file: PcExportFile,
        pm: PackageManager,
        launchers: InstalledLaunchers,
        fromDroidDeck: Boolean,
    ): PcLaunch? {
        val gameNativePkg = launchers.gameNative
        val gameHubPkg = launchers.gameHub
        val winlatorPkg = launchers.winlator
        if (file.extension == "desktop") {
            val path = file.rawPath ?: return null
            val pkg  = winlatorPkg ?: return null
            val intent = pm.getLaunchIntentForPackage(pkg)?.apply {
                putExtra("shortcut_path", path)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            } ?: return null

            return PcLaunch(intent, "Winlator", pkg)
        }

        // a file DroidDeck wrote launches in DroidDeck
        if (file.extension == DROIDDECK_EXTENSION || (fromDroidDeck && file.extension == "steam")) {
            val pkg = launchers.droidDeck ?: return null
            val appId = com.echo.feature.launcher.droidDeckAppId(file.idContent) ?: return null
            val intent = PcLauncherAdapters.forType(PcLauncherType.DROIDDECK)?.buildLaunchIntent(pkg, appId, "STEAM") ?: return null
            return PcLaunch(intent, "DroidDeck", pkg, storefront = "STEAM", storefrontGameId = appId)
        }

        val id = file.idContent?.trim()?.takeIf { it.toIntOrNull()?.let { n -> n > 0 } == true } ?: return null
        val source = PcLauncherAdapters.gameSourceForExtension(file.extension) ?: return null
        val storefront = StorefrontIdentity.normalizeStore(source)
        val storefrontId = id.takeIf { StorefrontIdentity.isPlausibleAppId(it) }

        gameNativePkg?.let { pkg ->
            val intent = PcLauncherAdapters.forType(PcLauncherType.GAMENATIVE)?.buildLaunchIntent(pkg, id, source) ?: return null
            return PcLaunch(intent, "GameNative", pkg, storefront, storefrontId)
        }

        if (file.extension == "steam" && gameHubPkg != null) {
            val type = if (gameHubPkg == "gamehub.lite") PcLauncherType.GAMEHUB_LITE else PcLauncherType.BANNERHUB_V6
            val name = if (gameHubPkg == "gamehub.lite") "GameHub Lite" else "BannerHub"
            val intent = PcLauncherAdapters.forType(type, pm)?.buildLaunchIntent(gameHubPkg, id, "STEAM") ?: return null
            return PcLaunch(intent, name, gameHubPkg, storefront = "STEAM", storefrontGameId = storefrontId)
        }
        return null
    }

    // a launcher's game is the same game when the launcher and its Steam app id match, whatever the file is
    // called (DroidDeck names its files "<Name> (<id>)" and shortens long names)
    private suspend fun findBySteamId(launch: PcLaunch): Game? =
        sameSteamGame(gameRepository.getByPlatform(WINDOWS_PLATFORM_ID), launch.packageName, launch.storefrontGameId)

    // the same Steam game under a launcher that is no longer installed
    private suspend fun findOrphanedSteamGame(launch: PcLaunch, pm: PackageManager): Game? {
        val id = launch.storefrontGameId ?: return null
        return gameRepository.getByPlatform(WINDOWS_PLATFORM_ID).firstOrNull { game ->
            game.storefrontGameId == id && game.packageName != launch.packageName &&
                game.packageName?.let { pkg -> runCatching { pm.getApplicationInfo(pkg, 0) }.isFailure } == true
        }
    }

    private suspend fun findWindowsGame(packageName: String, title: String): Game? {
        val key = WindowsGameKeys.normalizeTitle(title)
        return gameRepository.getByPlatform(WINDOWS_PLATFORM_ID).firstOrNull {
            it.packageName == packageName && WindowsGameKeys.normalizeTitle(it.displayTitle) == key
        }
    }

    private companion object {
        const val DROIDDECK_EXTENSION = "droiddeck"

    }
}

internal fun sameSteamGame(games: List<Game>, packageName: String, steamId: String?): Game? =
    steamId?.let { id -> games.firstOrNull { it.packageName == packageName && it.storefrontGameId == id } }
