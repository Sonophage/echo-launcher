package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.BUILT_IN_CATEGORIES
import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.domain.model.Category
import com.echo.core.domain.model.CategoryType
import com.echo.core.domain.model.HideLocationType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Spec #32, ADR 0002, review S2 and P4: Options on the Companion opens the menu of the item it keeps, as the Main
// Screen would where the item was hovered, after the Main Screen has moved to another column. The openers read
// [at], the Main Screen's state where the Companion took the item, not the Main Screen's state now.
class CompanionOptionsTest {
    private fun on(column: String, categories: List<Category> = BUILT_IN_CATEGORIES) =
        CrossbarUiState(categories = categories, selectedCategoryIndex = categories.indexOfFirst { it.id == column })

    private val vm = mockk<CrossbarViewModel>(relaxed = true)
    private val ui = MutableStateFlow(on(BuiltInCategory.GAMES))
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    init {
        // the Main Screen has moved on to Games
        every { vm.currentCategory() } answers { ui.value.currentCategoryOrNull() }
    }

    @Test
    fun `a playlist kept from Music opens its menu on the Companion after the Main Screen moved to Games`() {
        val music = CrossbarMusic(vm, ui, scope, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
        val playlist = CrossbarItem(id = "pl_7", title = "Night Drive", playlistId = 7L, type = CrossbarItemType.PLAYLIST)

        assertTrue("a playlist has a menu where it was", music.openMusicContextMenu(playlist, MenuSlot.COMPANION, at = on(BuiltInCategory.MUSIC)))

        assertEquals(7L, ui.value.companionMenu?.playlistId)
        assertNull("the Main Screen's slot is left alone", ui.value.activeContextMenu)
    }

    @Test
    fun `a game kept from a column hides from and files under that column, not the one the Main Screen is on`() {
        val retro = Category(id = "retro", name = "Retro", iconKey = "retro", type = CategoryType.SMART, position = 9, isGamingCategory = true)
        val columns = BUILT_IN_CATEGORIES + retro
        val where = on("retro", columns)
        ui.value = on(BuiltInCategory.GAMES, columns)
        every { vm.hideLocation(where) } returns Triple(HideLocationType.CATEGORY, "retro", "Retro")
        every { vm.hideLocation(ui.value) } returns Triple(HideLocationType.ALL_GAMES, "", "All Games")
        val games = CrossbarGames(vm, ui, scope, mockk(relaxed = true), mockk(relaxed = true))

        games.openGameContextMenu(CrossbarItem(id = "g1", title = "Crisis Core", platformId = "psp"), MenuSlot.COMPANION, at = where)

        val menu = ui.value.companionMenu!!
        assertEquals("Hide from <column> hides from where the game was", "retro", menu.hideLocation?.second)
        assertEquals("the category rows act on where the game was", "retro", menu.categoryContext)
    }
}
