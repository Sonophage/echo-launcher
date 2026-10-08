package com.echo.feature.launcher

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager

interface PcLauncherAdapter {
    val type: PcLauncherType

    val idPrompt: String

    val requiresIntegerId: Boolean

    val sources: List<String>

    fun buildLaunchIntent(packageName: String, gameId: String, source: String?): Intent?
}

private class GameHubFamilyAdapter(
    override val type: PcLauncherType,
    private val generationOf: (String) -> GameHubGeneration,
) : PcLauncherAdapter {
    override val idPrompt =
        "Game ID — on the game's page: tap Copy next to Local Game ID, or ⋮ → Banner Tools → Show Game ID"
    override val requiresIntegerId = false
    override val sources = emptyList<String>()

    override fun buildLaunchIntent(packageName: String, gameId: String, source: String?): Intent? {
        val trimmed = gameId.trim()
        val isLocalId = trimmed.startsWith(LOCAL_ID_PREFIX) && trimmed.length > LOCAL_ID_PREFIX.length
        val numericId = trimmed.toIntOrNull()?.takeIf { it > 0 }
        if (!isLocalId && numericId == null) return null
        val generation = generationOf(packageName)
        val activity = when (generation) {
            GameHubGeneration.V6 -> PcLauncherCatalog.V6_DEEP_LINK_ACTIVITY
            GameHubGeneration.V5 -> PcLauncherCatalog.V5_GAME_DETAIL_ACTIVITY
        }
        return Intent().apply {
            component = ComponentName(packageName, activity)
            action = "$packageName.LAUNCH_GAME"
            when {
                isLocalId -> putExtra("localGameId", trimmed)

                source == "STEAM" -> putExtra("steamAppId", numericId.toString())

                generation == GameHubGeneration.V5 -> {
                    putExtra("steamAppId", numericId.toString())
                    putExtra("localGameId", numericId.toString())
                }
                else -> putExtra("localGameId", numericId.toString())
            }
            putExtra("autoStartGame", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private companion object {
        const val LOCAL_ID_PREFIX = "local_"
    }
}

private class GameNativeAdapter(
    override val type: PcLauncherType = PcLauncherType.GAMENATIVE,
    private val activity: String = "app.gamenative.MainActivity",
    override val sources: List<String> = listOf("STEAM", "EPIC", "GOG", "AMAZON"),
    override val idPrompt: String = "Steam App ID (or store app id) for the installed game",
) : PcLauncherAdapter {
    override val requiresIntegerId = true

    override fun buildLaunchIntent(packageName: String, gameId: String, source: String?): Intent? {
        val id = gameId.trim().toIntOrNull()?.takeIf { it > 0 } ?: return null
        return Intent().apply {
            component = ComponentName(packageName, activity)
            action = "$packageName.LAUNCH_GAME"
            putExtra("app_id", id)
            putExtra("game_source", source?.takeIf { it.isNotBlank() } ?: "STEAM")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}

// DroidDeck launches a Steam game from its own link, droiddeck://game/<app id>: what its export files hold and the
// only way in on 0.3.1, the release on the owner's devices. Its LAUNCH_GAME action came after 0.3.1 (seen on the
// Konker, 2026-10-07: the action opened DroidDeck's home and nothing more, the link started the game). Steam only
private class DroidDeckAdapter : PcLauncherAdapter {
    override val type = PcLauncherType.DROIDDECK
    override val idPrompt = "Steam App ID for the installed game"
    override val requiresIntegerId = true
    override val sources = listOf("STEAM")

    override fun buildLaunchIntent(packageName: String, gameId: String, source: String?): Intent? {
        if (source != null && !source.equals("STEAM", ignoreCase = true)) return null
        val id = droidDeckAppId(gameId) ?: return null
        return Intent(Intent.ACTION_VIEW, android.net.Uri.parse("droiddeck://game/$id")).apply {
            component = ComponentName(packageName, "com.droiddeck.launcher.MainActivity")
            // DroidDeck already running gets the link as a new intent, not just brought to the front
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
    }
}

// the Steam app id a DroidDeck export file names: "droiddeck://game/<id>", or the bare id; null for anything else
fun droidDeckAppId(text: String?): String? {
    val line = text?.trim()?.lineSequence()?.firstOrNull()?.trim() ?: return null
    val id = line.removePrefix("droiddeck://game/")
    return id.takeIf { it.isNotEmpty() && it.length <= 10 && it.all(Char::isDigit) && it.toLongOrNull()?.let { n -> n in 1..Int.MAX_VALUE } == true }
}

object PcLauncherAdapters {
    fun forType(type: PcLauncherType, pm: PackageManager? = null): PcLauncherAdapter? = when (type) {
        PcLauncherType.BANNERHUB_V6,
        PcLauncherType.GAMEHUB_LITE -> GameHubFamilyAdapter(type) { pkg ->
            pm?.let { PcLauncherCatalog.gameHubGeneration(pkg, it) } ?: GameHubGeneration.V6
        }
        PcLauncherType.GAMENATIVE   -> GameNativeAdapter()
        PcLauncherType.DROIDDECK    -> DroidDeckAdapter()

        PcLauncherType.WINLATOR,
        PcLauncherType.MANUAL       -> null
    }

    internal fun gameHubAdapterFor(
        type: PcLauncherType,
        generationOf: (String) -> GameHubGeneration,
    ): PcLauncherAdapter = GameHubFamilyAdapter(type, generationOf)

    fun gameSourceForExtension(extension: String): String? = when (extension.lowercase()) {
        "steam"  -> "STEAM"
        "droiddeck" -> "STEAM"
        "epic"   -> "EPIC"
        "gog"    -> "GOG"
        "amazon" -> "AMAZON"
        "pcgame" -> "CUSTOM_GAME"
        else     -> null
    }
}
