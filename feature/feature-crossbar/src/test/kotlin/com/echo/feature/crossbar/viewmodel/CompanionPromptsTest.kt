package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.Game
import com.echo.core.ui.components.MenuState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Spec #32, #34, ADR 0002: what a Companion menu row opens (a picker or a text prompt) opens on the Companion Screen,
// in the slot its menu was in. A Main Screen row still opens on the Main Screen.
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
    fun `Edit Title from the Companion's menu is a prompt on the Companion`() {
        coEvery { vm.gameRepository.getById(1L) } returns mockk<Game>(relaxed = true) { every { displayTitle } returns "Crisis Core" }
        games.onGameMenuItem("detail_title", menu, MenuSlot.COMPANION)
        assertEquals("the prompt is drawn on the Companion", "Edit Title", ui.value.companionPrompts.collectionNameDialog?.title)
        assertNull("and not on the Main Screen", ui.value.mainPrompts.collectionNameDialog)
        // the row's menu is closed by activateContextMenuItem, before the row runs; the row closes no menu of its own
        verify(exactly = 0) { vm.closeContextMenu(any()) }
    }

    @Test
    fun `New Genre from the Companion's genre picker is a prompt on the Companion`() {
        CrossbarGenres(vm, ui, CoroutineScope(Dispatchers.Unconfined)).onPick(GenreTarget.Book("b1"), GENRE_PICK_NEW, MenuSlot.COMPANION)
        assertEquals("New Genre", ui.value.companionPrompts.collectionNameDialog?.title)
        assertNull(ui.value.mainPrompts.collectionNameDialog)
        verify(exactly = 0) { vm.closeContextMenu(MenuSlot.MAIN) }
    }

    // review P6: each screen holds its own prompts. A prompt opened on one screen neither replaces nor hides the
    // other's, and each takes only the text typed on its own screen
    @Test
    fun `a prompt on each screen stays open, and each takes the text typed on its own screen`() {
        val companionTitle = CrossbarUiState().withPrompts(MenuSlot.COMPANION) { copy(collectionNameDialog = CollectionNameDialogState(title = "Edit Title")) }
        val both = companionTitle.withPrompts(MenuSlot.MAIN) { copy(renameAppTarget = "com.a", renameAppCurrent = "A", renameAppText = "A") }
        assertEquals("a Main Screen Rename leaves the Companion's prompt", "Edit Title", both.companionPrompts.collectionNameDialog?.title)
        assertEquals("com.a", both.mainPrompts.renameAppTarget)
        assertNull("the Companion's Rename is not the Main Screen's", both.companionPrompts.renameAppTarget)

        val typed = both.withNamePromptText(MenuSlot.COMPANION, "Crisis Core").withNamePromptText(MenuSlot.MAIN, "Pocket Casts")
        assertEquals("Crisis Core", typed.companionPrompts.collectionNameDialog?.text)
        assertEquals("Pocket Casts", typed.mainPrompts.renameAppText)
    }

    @Test
    fun `Cancel on the Companion's prompt closes it, and leaves the Main Screen's`() {
        ui.value = CrossbarUiState()
            .withPrompts(MenuSlot.COMPANION) { copy(collectionNameDialog = CollectionNameDialogState(title = "Edit Title")) }
            .withPrompts(MenuSlot.MAIN) { copy(collectionNameDialog = CollectionNameDialogState(title = "Rename Column")) }
        games.onCancelCollectionName(MenuSlot.COMPANION)
        assertNull(ui.value.companionPrompts.collectionNameDialog)
        assertEquals("Rename Column", ui.value.mainPrompts.collectionNameDialog?.title)
    }

    @Test
    fun `a prompt on the Companion leaves the Main Screen live`() {
        val onCompanion = CrossbarUiState(showBootSequence = false, secondScreen = true)
            .withPrompts(MenuSlot.COMPANION) { copy(collectionNameDialog = CollectionNameDialogState(title = "Edit Title")) }
        assertFalse("the Main Screen is not blocked by it", onCompanion.hasBlockingOverlay)
        assertTrue("one on the Main Screen blocks it", onCompanion.withPrompts(MenuSlot.MAIN) { copy(renameAppTarget = "com.a") }.hasBlockingOverlay)
    }
}
