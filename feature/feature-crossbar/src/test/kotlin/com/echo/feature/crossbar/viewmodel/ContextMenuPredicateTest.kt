package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.domain.model.Category
import com.echo.core.domain.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextMenuPredicateTest {
    private fun state(
        categoryId: String,
        item: CrossbarItem,
        musicNav: MusicNav = MusicNav.Root,
        videoNav: VideoNav = VideoNav.Root,
        photoNav: PhotoNav = PhotoNav.Root,
    ) = CrossbarUiState(
        categories = listOf(Category(categoryId, categoryId, categoryId, type = CategoryType.BUILT_IN, position = 0)),
        selectedCategoryIndex = 0,
        currentItems = listOf(item),
        selectedItemIndex = 0,
        musicNav = musicNav,
        videoNav = videoNav,
        photoNav = photoNav,
    )

    @Test
    fun `game row has a context menu`() {
        val item = CrossbarItem(id = "g1", title = "Crisis Core", gameId = 1L)
        assertTrue(item.hasContextMenu(state(BuiltInCategory.GAMES, item)))
    }

    @Test
    fun `platform row has a context menu`() {
        val item = CrossbarItem(id = "psp", title = "PSP", platformId = "psp")
        assertTrue(item.hasContextMenu(state(BuiltInCategory.GAMES, item)))
    }

    @Test
    fun `all-games folder has a context menu`() {
        val item = CrossbarItem(id = "all", title = "All Games", type = CrossbarItemType.ALL_GAMES)
        assertTrue(item.hasContextMenu(state(BuiltInCategory.GAMES, item)))
    }

    @Test
    fun `app row has a context menu`() {
        val item = CrossbarItem(id = "app", title = "Spotify", packageName = "com.spotify.music")
        assertTrue(item.hasContextMenu(state(BuiltInCategory.GAMES, item)))
    }

    @Test
    fun `plain standard row has no context menu`() {
        val item = CrossbarItem(id = "x", title = "Nothing", type = CrossbarItemType.STANDARD)
        assertFalse(item.hasContextMenu(state(BuiltInCategory.GAMES, item)))
    }

    @Test
    fun `music track has a context menu`() {
        val item = CrossbarItem(id = "tr1", title = "Track", type = CrossbarItemType.MUSIC_TRACK)
        assertTrue(item.hasContextMenu(state(BuiltInCategory.MUSIC, item)))
    }

    @Test
    fun `now playing row has a context menu`() {
        val item = CrossbarItem(id = CrossbarViewModel.NOW_PLAYING_ITEM_ID, title = "Now Playing")
        assertTrue(item.hasContextMenu(state(BuiltInCategory.MUSIC, item)))
    }

    @Test
    fun `video file has a context menu`() {
        val item = CrossbarItem(id = "vid_1", title = "Clip", type = CrossbarItemType.VIDEO_FILE)
        assertTrue(item.hasContextMenu(state(BuiltInCategory.VIDEO, item)))
    }

    @Test
    fun `video file with wrong id prefix has no context menu`() {
        val item = CrossbarItem(id = "bad_1", title = "Clip", type = CrossbarItemType.VIDEO_FILE)
        assertFalse(item.hasContextMenu(state(BuiltInCategory.VIDEO, item)))
    }

    @Test
    fun `photo file has a context menu`() {
        val item = CrossbarItem(id = "pho_1", title = "Pic", type = CrossbarItemType.PHOTO_FILE)
        assertTrue(item.hasContextMenu(state(BuiltInCategory.PHOTO, item)))
    }

    @Test
    fun `a music track carries its menu into any column`() {
        val item = CrossbarItem(id = "mt_1", title = "Track", type = CrossbarItemType.MUSIC_TRACK)

        assertTrue(item.hasContextMenu(state(BuiltInCategory.MUSIC, item)))
        assertTrue(item.hasContextMenu(state(BuiltInCategory.RECENTLY_PLAYED, item)))
        assertTrue(item.hasContextMenu(state(BuiltInCategory.GAMES, item)))
    }

    @Test
    fun `A and Y route a media row to the same library`() {
        val cursor = BuiltInCategory.RECENTLY_PLAYED
        val cases = mapOf(
            BuiltInCategory.LIBRARY to CrossbarItem(id = "book_1", title = "A Book", type = CrossbarItemType.LIBRARY_BOOK),
            BuiltInCategory.MUSIC to CrossbarItem(id = "mt_1", title = "A Track", type = CrossbarItemType.MUSIC_TRACK),
            BuiltInCategory.VIDEO to CrossbarItem(id = "vid_1", title = "A Film", type = CrossbarItemType.VIDEO_FILE),
            BuiltInCategory.PHOTO to CrossbarItem(id = "pho_1", title = "A Photo", type = CrossbarItemType.PHOTO_FILE),
        )
        for ((expected, item) in cases) {
            assertEquals(
                "${item.type} must dispatch to its own library from any column",
                expected,
                item.menuHostCategory(cursor),
            )
            assertTrue("${item.type} must also still offer its menu", item.hasContextMenu(state(cursor, item)))
        }
    }

    @Test
    fun `a row that belongs to no library still answers to the column it is in`() {
        val plain = CrossbarItem(id = "row_1", title = "Something", type = CrossbarItemType.STANDARD)

        assertFalse(plain.hasContextMenu(state(BuiltInCategory.RECENTLY_PLAYED, plain)))
    }
}
