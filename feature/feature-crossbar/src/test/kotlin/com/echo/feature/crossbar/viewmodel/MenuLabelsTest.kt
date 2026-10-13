package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.GameGenre
import com.echo.core.domain.model.ControllerDisplayType
import com.echo.core.domain.model.IconLegibilityStyle
import com.echo.core.domain.model.ScrollSpeed
import com.echo.core.domain.model.StickSensitivity
import com.echo.core.domain.model.TriggerSensitivity
import com.echo.core.domain.model.VideoSnapPlacement
import com.echo.core.domain.model.displayLabel
import com.echo.core.data.repository.ButtonHints
import com.echo.core.ui.icons.GameIconStyle
import com.echo.core.ui.wave.WaveDesign
import com.echo.core.ui.wave.WaveStyle
import com.echo.feature.appbar.AppMenuAction
import com.echo.feature.artwork.studio.ArtworkStudioUiState
import com.echo.feature.artwork.studio.CropShapeChoice
import com.echo.feature.artwork.studio.StudioAction
import com.echo.feature.artwork.studio.StudioLeaveChoice
import com.echo.feature.artwork.studio.StudioReplaceChoice
import com.echo.feature.crossbar.ui.QuickSettingsState
import com.echo.feature.crossbar.ui.quickTileText
import com.echo.feature.photos.PhotoControl
import com.echo.feature.photos.PhotoInfoAction
import com.echo.feature.photos.PhotoViewerAction
import com.echo.feature.settings.ui.LOGS_MENU_ROWS
import com.echo.feature.settings.ui.addPcGameRows
import com.echo.feature.settings.ui.savedThemeMenuOptions
import com.echo.feature.video.videoOptionsMenu
import com.echo.themekit.ThemePart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Every menu and rail speaks CONTEXT.md's words (spec #2): one name per action, in Title Case with the short words
// lower case. ITEM_MENUS is the one list of item menu builders and OTHER_RAILS the one list of every other rail, and
// of the Library Manager's rows (ticket #16); a new builder is added to one of them, and each builder is called once per state that changes its
// rows, so every label it can show is read. This module depends on every module that draws a rail, so the rules
// are defined here once and no other module copies them
class MenuLabelsTest {
    private val quickOn = QuickSettingsState(wave = WaveStyle.ANIMATED, backdropOn = true, rowCoverArt = true, recentAppsOn = true, chips = emptyList())
    private val photo = ItemMenus.photo
    private val ITEM_MENUS = ItemMenus.all

    // every rail outside the crossbar's item menus, as the labels it draws. A picker whose rows are the player's own
    // data (emulators, playlists, categories, genres) is read for its fixed rows
    private val OTHER_RAILS: List<Pair<String, List<String>>> = listOf(
        "App Drawer" to AppMenuAction.entries.map { it.label },
        "Library Manager's Add PC Game" to (addPcGameRows("", "", emptyList(), null) +
            addPcGameRows("570", "Dota 2", listOf("STEAM", "EPIC"), "STEAM")).map { it.label },
        "theme" to savedThemeMenuOptions({}, {}, {}).map { it.label },
        "Logs" to LOGS_MENU_ROWS.map { it.label },
        "game genre picker" to (gameGenrePickerRows(scraped = "Action", current = null) + gameGenrePickerRows(null, GameGenre.entries.first())).map { it.label },
        "media genre picker" to (genrePickerRows("Rock", null, listOf("Jazz")) + genrePickerRows(null, "Jazz", emptyList())).map { it.label },
        "settings pickers" to ControllerDisplayType.entries.map { it.displayLabel() } + StickSensitivity.entries.map { it.displayLabel() } +
            TriggerSensitivity.entries.map { it.label } + ScrollSpeed.entries.map { it.displayLabel() } + WaveDesign.entries.map { it.label } +
            IconLegibilityStyle.entries.map { it.label } + ButtonHints.entries.map { it.label } +
            VideoSnapPlacement.entries.map { it.label } + GameIconStyle.entries.map { it.label },
        "Artwork Studio actions" to listOf(false, true).flatMap { nsfw -> StudioAction.entries.map { ArtworkStudioUiState(includeNsfw = nsfw).labelOf(it) } },
        "Artwork Studio prompts" to StudioLeaveChoice.entries.map { it.label } + StudioReplaceChoice.entries.map { it.label } +
            CropShapeChoice.entries.map { it.label },
        "video player Options" to videoOptionsMenu(0, 1.5f, "Off", "English", "Fit").rows.map { it.label },
        "photo viewer bar" to PhotoControl.entries.map { it.label },
        "photo viewer info panel" to listOf(photo, photo.copy(isFavorite = true)).flatMap { p -> PhotoInfoAction.entries.map { it.labelFor(p) } },
        "Library Manager" to libraryManagerRowLabels(),
        "theme store's parts" to ThemePart.entries.map { it.label },
        "Game Info's views" to (listOf(null) + GameInfoAction.entries).map(::gameInfoSectionLabel),
        "notification panel tabs" to PanelTab.entries.map { it.label },
        "Quick Settings tiles" to listOf(quickOn, quickOn.copy(backdropOn = false, rowCoverArt = false, recentAppsOn = false, secondScreenOn = false))
            .flatMap { q -> QuickSetting.entries.flatMap { quickTileText(it, q).toList() } },
    )

    private val shown get() = ITEM_MENUS.flatMap { (menu, rows) -> rows.filterNot { it.hidden }.map { menu to it.label } } +
        OTHER_RAILS.flatMap { (rail, labels) -> labels.map { rail to it } }

    @Test
    fun `every menu and rail label is in Title Case with to, from, in, on, as, of and and lower case`() {
        val wrong = shown.filterNot { (_, label) -> isTitleCase(label) }
        assertEquals("labels not in Title Case", emptyList<Pair<String, String>>(), wrong)
    }

    @Test
    fun `no menu or rail label uses a word CONTEXT_md lists under Avoid`() {
        val avoid = avoidedTerms()
        assertTrue("CONTEXT.md's Avoid lists were not read", avoid.isNotEmpty())
        val wrong = shown.filter { (_, label) -> label !in NAMES_OF_OTHER_SCREENS && avoid.any { it.matches(label) } }
        assertEquals("labels CONTEXT.md says to avoid", emptyList<Pair<String, String>>(), wrong)
    }

    // CONTEXT.md: Remove never deletes from the device, so a row that erases a file says Delete
    @Test
    fun `every row that erases a file says Delete`() {
        val erases = listOf(
            "photo viewer bar" to PhotoControl.REMOVE.label,
            "photo viewer info panel" to PhotoInfoAction.REMOVE.label,
            "photo viewer Options" to PhotoViewerAction.REMOVE.label,
            "theme" to savedThemeMenuOptions({}, {}, {}).single { it.destructive }.label,
        )
        assertEquals("rows that erase a file", emptyList<Pair<String, String>>(), erases.filterNot { (_, label) -> label.startsWith("Delete") })
    }

    @Test
    fun `a game's menu has one Manual row, also with Game Info open`() {
        val manuals = ITEM_MENUS.first { it.first == "game with Game Info open" }.second.count { it.label == "Manual" }
        assertEquals(1, manuals)
    }

    @Test
    fun `the Library Manager's rows are read from its screen`() {
        val labels = libraryManagerRowLabels()
        assertTrue("read ${labels.size}: $labels", "Scan This Console" in labels && "Pin to Top" in labels && labels.size >= 15)
    }

    @Test
    fun `the rules catch what they are for`() {
        assertTrue(isTitleCase("Pin to Top") && isTitleCase("Default Emulator (PPSSPP)") && isTitleCase("Playback Speed · 1.5×") && isTitleCase("Zoom In"))
        assertTrue(!isTitleCase("Pin To Top") && !isTitleCase("Remove from this Playlist") && !isTitleCase("Set as wallpaper") && !isTitleCase("to Top") && !isTitleCase("Scan For Vita Games"))
        val avoid = avoidedTerms()
        assertTrue("Info" in avoid.filter { it.matches("Info") }.map { it.term })
        assertTrue(avoid.any { it.matches("Pin") } && avoid.none { it.matches("Pin to Top") })
        assertTrue(avoid.any { it.matches("Game Info") } && avoid.any { it.matches("Show Information") })
    }

    companion object {
        // App Info opens Android's own screen of that name, not ECHO's Details (owner, 2026-10-12: App Info in About)
        private val NAMES_OF_OTHER_SCREENS = setOf("App Info")

        // spec #2's short words, and "by" (Group by Genre) and "for" (Scan for Vita Games)
        private val SHORT_WORDS = setOf("to", "from", "in", "on", "as", "of", "and", "by", "for")

        // every word starts upper case, except a short word between the first and the last, which is all lower case
        // ("Pin to Top", but "Zoom In"). A word that does not start with a letter (a number, ·, ×) has no case
        fun isTitleCase(label: String): Boolean {
            val words = label.split(' ').filter { it.isNotEmpty() }
            return words.withIndex().all { (i, raw) ->
                val word = raw.trimStart('(', '"', '\'')
                when {
                    word.isEmpty() || !word[0].isLetter() -> true
                    i in 1 until words.lastIndex && word.lowercase() in SHORT_WORDS -> word == word.lowercase()
                    else -> word[0].isUpperCase()
                }
            }
        }

        // a term CONTEXT.md lists under _Avoid_. "X (alone)" forbids the label X itself; "X (when ...)" and the like
        // depend on what the row does, so a reviewer judges them; any other X is forbidden as words in a label
        class Avoided(val term: String, private val alone: Boolean) {
            fun matches(label: String): Boolean =
                if (alone) label.equals(term, ignoreCase = true)
                else Regex("\\b${Regex.escape(term)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(label)
        }

        // the Library Manager's rows are drawn inline in its screen, so their fixed labels are read from the source: each
        // SettingsRow or SettingsToggleRow whose label is a plain string. A label built from data (a system's or an
        // app's name, an extension) is not read
        fun libraryManagerRowLabels(): List<String> {
            val path = "feature/feature-settings/src/main/kotlin/com/echo/feature/settings/ui/LibraryManagerScreen.kt"
            val source = listOf(java.io.File("../../$path"), java.io.File(path)).first { it.exists() }.readText()
            return Regex("""Settings(?:Row|ToggleRow)\(\s*label\s*=\s*"([^"$\\]*)"""").findAll(source).map { it.groupValues[1] }.toList()
        }

        fun avoidedTerms(): List<Avoided> {
            val context = listOf(java.io.File("../../CONTEXT.md"), java.io.File("CONTEXT.md")).first { it.exists() }
            return context.readLines().filter { it.startsWith("_Avoid_: ") }
                .flatMap { it.removePrefix("_Avoid_: ").split(',') }
                .map { it.trim() }
                .mapNotNull { entry ->
                    val term = entry.substringBefore('(').trim()
                    val qualifier = entry.substringAfter('(', "").substringBefore(')')
                    when {
                        qualifier.isEmpty() -> Avoided(term, alone = false)
                        qualifier == "alone" -> Avoided(term, alone = true)
                        qualifier.split(' ').first() in setOf("when", "for", "as") -> null
                        else -> Avoided(term, alone = false)
                    }
                }
        }
    }
}
