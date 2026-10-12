package com.echo.feature.crossbar.bottomscreen

import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.domain.model.Category
import com.echo.core.domain.model.CategoryType
import com.echo.core.ui.components.MenuState
import com.echo.feature.crossbar.viewmodel.CrossbarContextMenu
import com.echo.feature.crossbar.viewmodel.CrossbarUiState
import com.echo.feature.crossbar.viewmodel.SearchScope
import com.echo.feature.crossbar.viewmodel.SearchState
import com.echo.feature.crossbar.viewmodel.withMusicPlayer
import com.echo.feature.settings.ui.storeDrawsHero
import org.junit.Assert.assertEquals
import org.junit.Test

// ADR 0001: the Main Screen is single-screen ECHO. An attached Companion Screen may add to it, but it must
// not change what the Main Screen shows or where things open on it. This test fails if anything moves off
// the Main Screen again because a Companion Screen is attached.
class MainScreenRollbackGuardTest {
    // what the Main Screen derives from the Crossbar's state, and the conditions its composables decide by: the menu
    // CrossbarShell draws, and whether the Theme Store draws its own hero while Settings reports to a Companion
    // Screen. When a ticket brings a screen back to the Main Screen, add what it reads here.
    private data class MainScreen(
        val reachableColumns: List<String>,
        val waveShown: Boolean,
        val hasBlockingOverlay: Boolean,
        val enterOpensAppDrawer: Boolean,
        val musicPlayerShown: Boolean,
        val search: SearchState?,
        val menu: CrossbarContextMenu?,
        val storeHero: Boolean?,
    )

    private fun mainScreen(s: CrossbarUiState) = MainScreen(
        reachableColumns = s.categories.filter(s::categoryReachable).map { it.id },
        waveShown = s.waveShown,
        hasBlockingOverlay = s.hasBlockingOverlay,
        enterOpensAppDrawer = s.enterOpensAppDrawer,
        musicPlayerShown = s.musicPlayerVisible,
        search = s.search,
        menu = mainScreenMenu(s),
        storeHero = storeDrawsHero(previewReported = companionOnShow(s.secondScreen, s.companionInPlace))
            .takeIf { s.activeSettingsScreen == THEME_STORE },
    )

    private fun column(id: String, position: Int) =
        Category(id = id, name = id, iconKey = "", type = CategoryType.BUILT_IN, position = position)

    // past the boot sequence, which would otherwise cover everything and hide a difference
    private val crossbar = CrossbarUiState(
        showBootSequence = false,
        categories = listOf(column(BuiltInCategory.RECENTLY_PLAYED, 0), column(BuiltInCategory.ANDROID, 1)),
    )

    // one case for each thing that once left the Main Screen for the Companion Screen: what the player does,
    // applied to the Crossbar with and without a Companion Screen. When a ticket brings one back, add its case.
    private val cases: Map<String, (CrossbarUiState) -> CrossbarUiState> = mapOf(
        "Recent is a column on the Crossbar" to { it },
        "the App Drawer opens on the Main Screen" to { it.copy(activeAppDrawerFilter = "APPS") },
        "Search, its field and its results, opens on the Main Screen" to { it.copy(search = SearchState(scope = SearchScope.ALL)) },
        "Settings opens on the Main Screen" to { it.copy(activeSettingsScreen = "settings_display") },
        "the Theme Store opens on the Main Screen" to { it.copy(activeSettingsScreen = THEME_STORE) },
        "the music player opens on the Main Screen when a track starts" to { it.withMusicPlayer() },
        "a context menu opens on the Main Screen" to {
            it.copy(activeContextMenu = CrossbarContextMenu(state = MenuState(title = "Options", rows = emptyList())))
        },
    )

    @Test
    fun `the Main Screen is the same with and without a Companion Screen attached`() {
        cases.forEach { (case, act) ->
            assertEquals(case, mainScreen(act(crossbar)), mainScreen(act(crossbar.copy(secondScreen = true))))
        }
    }
}

private const val THEME_STORE = "settings_theme_store"
