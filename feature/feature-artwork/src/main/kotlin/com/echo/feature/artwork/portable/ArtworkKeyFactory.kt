package com.echo.feature.artwork.portable

import com.echo.core.domain.model.Game
import com.echo.core.domain.model.GameContentType

object ArtworkKeyFactory {
    fun keyFor(game: Game): String? = when {
        !game.romPath.isNullOrBlank() -> {
            val fileName = game.romPath!!.replace('\\', '/').substringAfterLast('/')
            "rom/${game.platformId}/${ArtworkNaming.slug(ArtworkNaming.fileStem(fileName))}"
        }
        game.contentType != GameContentType.GAME || !game.packageName.isNullOrBlank() -> {
            val pkg = game.packageName ?: return null
            val shortcut = game.shortcutId
            if (shortcut.isNullOrBlank()) "app/$pkg" else "app/$pkg/${ArtworkNaming.slug(shortcut)}"
        }
        game.isManualEntry -> "manual/${ArtworkNaming.slug(game.title)}"
        else -> null
    }
}
