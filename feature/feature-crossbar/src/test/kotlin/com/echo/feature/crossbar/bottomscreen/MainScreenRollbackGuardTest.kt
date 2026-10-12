package com.echo.feature.crossbar.bottomscreen

import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.domain.model.Category
import com.echo.core.domain.model.CategoryType
import com.echo.feature.crossbar.viewmodel.CrossbarUiState
import com.echo.feature.crossbar.viewmodel.SearchScope
import com.echo.feature.crossbar.viewmodel.SearchState
import org.junit.Assert.assertEquals
import org.junit.Test

// ADR 0001: the Main Screen is single-screen ECHO. An attached Companion Screen may add to it, but it must
// not change what the Main Screen shows or where things open on it. This test fails if anything moves off
// the Main Screen again because a Companion Screen is attached.
class MainScreenRollbackGuardTest {
    // what the Main Screen derives from the Crossbar's state. When a ticket brings a screen back to the Main
    // Screen, add what it reads here.
    private data class MainScreen(
        val reachableColumns: List<String>,
        val waveShown: Boolean,
        val hasBlockingOverlay: Boolean,
        val enterOpensAppDrawer: Boolean,
        val search: SearchState?,
    )

    private fun mainScreen(s: CrossbarUiState) = MainScreen(
        reachableColumns = s.categories.filter(s::categoryReachable).map { it.id },
        waveShown = s.waveShown,
        hasBlockingOverlay = s.hasBlockingOverlay,
        enterOpensAppDrawer = s.enterOpensAppDrawer,
        search = s.search,
    )

    private fun column(id: String, position: Int) =
        Category(id = id, name = id, iconKey = "", type = CategoryType.BUILT_IN, position = position)

    // past the boot sequence, which would otherwise cover everything and hide a difference
    private val crossbar = CrossbarUiState(
        showBootSequence = false,
        categories = listOf(column(BuiltInCategory.RECENTLY_PLAYED, 0), column(BuiltInCategory.ANDROID, 1)),
    )

    // one case for each thing that once left the Main Screen for the Companion Screen. When a ticket brings
    // one back, add its case here.
    private val cases = mapOf(
        "Recent is a column on the Crossbar" to crossbar,
        "the App Drawer opens on the Main Screen" to crossbar.copy(activeAppDrawerFilter = "APPS"),
        "Search, its field and its results, opens on the Main Screen" to crossbar.copy(search = SearchState(scope = SearchScope.ALL)),
    )

    @Test
    fun `the Main Screen is the same with and without a Companion Screen attached`() {
        cases.forEach { (case, state) ->
            assertEquals(case, mainScreen(state), mainScreen(state.copy(secondScreen = true)))
        }
    }
}
