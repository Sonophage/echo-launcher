package com.echo.feature.crossbar.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NamePromptTextTest {
    private val prompts: List<Triple<String, NamePrompts, (NamePrompts) -> String?>> =
        listOf(
            Triple("rename shortcut", NamePrompts(renameAppTarget = "com.example", renameAppCurrent = "Example"), { p -> p.renameAppText }),
            Triple("edit title", NamePrompts(collectionNameDialog = CollectionNameDialogState(title = "Edit Title")), { p -> p.collectionNameDialog?.text }),
            Triple("playlist name", NamePrompts(playlistNameDialog = PlaylistNameDialogState(title = "New Playlist")), { p -> p.playlistNameDialog?.text }),
            Triple("save theme name", NamePrompts(saveThemeNameDialog = PlaylistNameDialogState(title = "Save as Theme")), { p -> p.saveThemeNameDialog?.text }),
        )

    @Test
    fun `every name prompt receives what was typed into it`() {
        assertEquals("the prompt table must cover all four name prompts", 4, prompts.size)

        for ((name, open, read) in prompts) {
            val typed = CrossbarUiState(mainPrompts = open).withNamePromptText(MenuSlot.MAIN, "Backlog")
            assertEquals("$name must receive the typed text", "Backlog", read(typed.mainPrompts))
        }
    }

    @Test
    fun `a prompt opens with its seed text already in the live field`() {
        val seeded = CollectionNameDialogState(title = "Edit Title", initialText = "Shooters")
        assertEquals("the live field starts at the seed", "Shooters", seeded.text)
    }

    @Test
    fun `typing with no prompt open changes nothing`() {
        val idle = CrossbarUiState()
        val after = idle.withNamePromptText(MenuSlot.MAIN, "stray")
        assertEquals("no prompt open means no rename text", "", after.mainPrompts.renameAppText)
        assertNull("no prompt open means no text prompt", after.mainPrompts.collectionNameDialog)
        assertEquals("the state is untouched", idle, after)
    }

    @Test
    fun `the first prompt in gamepad order wins when two are somehow open`() {
        val both = CrossbarUiState(
            mainPrompts = NamePrompts(
                renameAppTarget = "com.example",
                renameAppCurrent = "Example",
                collectionNameDialog = CollectionNameDialogState(title = "Edit Title"),
            ),
        ).withNamePromptText(MenuSlot.MAIN, "Backlog")

        assertEquals("the rename prompt is checked first, so it takes the text", "Backlog", both.mainPrompts.renameAppText)
        assertEquals(
            "the text prompt must not also take it",
            "",
            both.mainPrompts.collectionNameDialog?.text,
        )
    }
}
