package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.Game
import com.echo.core.ui.components.MenuState
import com.echo.feature.crossbar.companion.CompanionKeys
import com.echo.feature.crossbar.companion.companionKeys
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

// Spec #32, #34, ADR 0002: what a Companion menu row opens (a picker or a text prompt) opens on the Companion Screen,
// in the slot its menu was in, and closes that slot only. A Main Screen row still opens on the Main Screen.
class CompanionPromptsTest {
    private val vm = mockk<CrossbarViewModel>(relaxed = true)
    private val ui = MutableStateFlow(CrossbarUiState())
    private val games = CrossbarGames(vm, ui, CoroutineScope(Dispatchers.Unconfined), mockk(relaxed = true), mockk(relaxed = true))
    private val menu = CrossbarContextMenu(state = MenuState(title = "Crisis Core", rows = emptyList()), gameId = 1L, categoryContext = "rpg")

    @Test
    fun `a picker a game's row opens follows the slot of its menu`() {
        MenuSlot.entries.forEach { slot ->
            listOf("change_emulator", "shelves", "choose_disc", "edit_genre", "move_category").forEach { games.onGameMenuItem(it, menu, slot) }
            verify { vm.openEmulatorPickerMenu(1L, slot) }
            verify { vm.openShelvesPickerMenu(1L, slot) }
            verify { vm.openDiscPickerMenu(1L, slot) }
            verify { vm.openGenrePickerMenu(1L, slot) }
            verify { vm.openGameCategoryPicker(1L, "rpg", "move", slot) }
        }
    }

    @Test
    fun `Edit Title from the Companion's menu is a prompt on the Companion, and closes only the Companion's menu`() {
        coEvery { vm.gameRepository.getById(1L) } returns mockk<Game>(relaxed = true) { every { displayTitle } returns "Crisis Core" }
        games.onGameMenuItem("detail_title", menu, MenuSlot.COMPANION)
        assertEquals("Edit Title", ui.value.collectionNameDialog?.title)
        assertEquals("the prompt is drawn on the Companion", MenuSlot.COMPANION, ui.value.namePromptSlot)
        verify { vm.closeContextMenu(MenuSlot.COMPANION) }
        verify(exactly = 0) { vm.closeContextMenu(MenuSlot.MAIN) }
    }

    @Test
    fun `New Genre from the Companion's genre picker is a prompt on the Companion`() {
        CrossbarGenres(vm, ui, CoroutineScope(Dispatchers.Unconfined)).onPick(GenreTarget.Book("b1"), GENRE_PICK_NEW, MenuSlot.COMPANION)
        assertEquals("New Genre", ui.value.collectionNameDialog?.title)
        assertEquals(MenuSlot.COMPANION, ui.value.namePromptSlot)
        verify(exactly = 0) { vm.closeContextMenu(MenuSlot.MAIN) }
    }

    @Test
    fun `a prompt on the Companion keeps its keys there and leaves the Main Screen live`() {
        val onCompanion = CrossbarUiState(showBootSequence = false, secondScreen = true, collectionNameDialog = CollectionNameDialogState(title = "Edit Title", slot = MenuSlot.COMPANION))
        assertEquals("the field is in the Companion's window", CompanionKeys.HOLD, companionKeys(onCompanion))
        assertFalse("the Main Screen is not blocked by it", onCompanion.hasBlockingOverlay)
        val onMain = onCompanion.copy(collectionNameDialog = CollectionNameDialogState(title = "Edit Title"))
        assertEquals("a prompt opened on the Main Screen is drawn there", MenuSlot.MAIN, onMain.namePromptSlot)
        assertEquals(CompanionKeys.CLAIM, companionKeys(onMain))
    }
}
