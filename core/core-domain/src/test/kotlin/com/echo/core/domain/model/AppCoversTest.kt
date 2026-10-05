package com.echo.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

// owner, 2026-10-05: GameNative was wearing The Witcher 3's cover because one of the Steam games it runs had it
class AppCoversTest {
    private fun game(pkg: String, icon: String?, intent: String? = null) =
        Game(title = pkg, platformId = "x", packageName = pkg, iconUri = icon, artworkUri = "main-$pkg", launchIntentUri = intent)

    @Test
    fun `an Android game lends its cover to its app, and a launcher of many games does not`() {
        val covers = appCovers(listOf(
            game("com.game.one", icon = "cover-one"),
            game("app.gamenative", icon = "witcher", intent = "intent:a"),
            game("app.gamenative", icon = "skyrim", intent = "intent:b"),
        ))
        assertEquals(mapOf("com.game.one" to "cover-one"), covers)
    }

    // owner, 2026-10-05: the icon slot is every game's cover; the main art only when the icon slot is empty
    @Test
    fun `the icon slot is the cover, and the main art fills in only when it is empty`() {
        assertEquals("icon", coverArtOf(icon = "icon", main = "fanart"))
        assertEquals("fanart", coverArtOf(icon = null, main = "fanart"))
        assertEquals("fanart", coverArtOf(icon = " ", main = "fanart"))
        assertEquals(null, coverArtOf(icon = null, main = null))
    }
}
