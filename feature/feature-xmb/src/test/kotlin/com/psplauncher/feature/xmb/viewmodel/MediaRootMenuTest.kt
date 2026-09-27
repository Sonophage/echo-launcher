package com.psplauncher.feature.xmb.viewmodel

import com.psplauncher.core.data.repository.MediaRootKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaRootMenuTest {

    @Test
    fun `every folder menu row maps to an action`() {
        MediaRootKind.entries.forEach { kind ->
            listOf(true, false).forEach { linked ->
                mediaRootContextMenuItems(linked, kind).forEach { row ->
                    assertNotNull(
                        "${kind.name} row '${row.label}' (${row.action}) has no action, so pressing it " +
                            "would do nothing and no test would notice",
                        row.action?.let(::mediaRootActionOf),
                    )
                }
            }
        }
    }

    @Test
    fun `every library menu row maps to an action`() {
        MediaRootKind.entries.forEach { kind ->
            mediaFoldersContextMenuItems(kind).forEach { row ->
                assertNotNull(
                    "${kind.name} row '${row.label}' (${row.action}) has no action",
                    row.action?.let(::mediaFoldersActionOf),
                )
            }
        }
    }

    @Test
    fun `a default-app row maps to PICK_APP whatever the package is called`() {
        assertEquals(
            MediaFoldersAction.PICK_APP,
            mediaFoldersActionOf("${MEDIA_APP_PREFIX}org.videolan.vlc"),
        )
        assertEquals(
            "the System Default row carries the sentinel, not a package",
            MediaFoldersAction.PICK_APP,
            mediaFoldersActionOf("$MEDIA_APP_PREFIX${XMBViewModel.MEDIA_APP_NONE}"),
        )
    }

    @Test
    fun `an id nothing offers maps to nothing`() {
        assertNull(
            "a mapper that answers for unknown ids would hide a renamed row instead of surfacing it",
            mediaRootActionOf("rename_folder"),
        )
        assertNull(mediaFoldersActionOf("scan_folder"))
    }

    @Test
    fun `removing a folder is destructive, so it is confirmed before it runs`() {
        val remove = mediaRootContextMenuItems(linked = true, kind = MediaRootKind.MUSIC)
            .single { it.action == "media_root_remove" }

        assertTrue("dropping a root discards its scanned library", remove.isDestructive)
        assertTrue("a destructive row must confirm", remove.confirms)
    }

    @Test
    fun `a folder whose grant is lost still offers Relink and Remove, but not Rescan`() {
        val ids = mediaRootContextMenuItems(linked = false, kind = MediaRootKind.MUSIC)
            .mapNotNull { it.action }

        assertTrue("Relink is the only way back, so it must always be offered", "media_root_relink" in ids)
        assertTrue("a stranded root must still be removable", "media_root_remove" in ids)
        assertTrue(
            "scanning a folder we cannot read only produces a failed task",
            "media_root_rescan" !in ids,
        )
    }

    @Test
    fun `only the kinds that have a cache offer to clear one`() {
        val withCache = MediaRootKind.entries.filter { kind ->
            mediaFoldersContextMenuItems(kind).any { it.action == "media_clear_cache" }
        }

        assertEquals(
            "music and video generate no thumbnails, so the row would clear nothing",
            listOf(MediaRootKind.PHOTO, MediaRootKind.BOOK),
            withCache,
        )
    }

    @Test
    fun `photo has no default-app row because there is no photo player setting`() {
        assertTrue(
            mediaFoldersContextMenuItems(MediaRootKind.PHOTO).none { it.action == "media_default_app" },
        )
        MediaRootKind.entries.filter { it != MediaRootKind.PHOTO }.forEach { kind ->
            assertTrue(
                "${kind.name} lost its default app row",
                mediaFoldersContextMenuItems(kind).any { it.action == "media_default_app" },
            )
        }
    }

    @Test
    fun `every row that opens a media menu also advertises one`() {
        val state = XMBUiState()

        MediaRootKind.entries.forEach { kind ->
            val foldersRow = XMBItem(
                id = XMBViewModel.mediaFoldersItemId(kind),
                title = "Folders",
                type = XMBItemType.MEDIA_ROOT,
                mediaRootKind = kind,
            )
            assertTrue(
                "the Folders row opens a menu, so the hint bar must offer Options or nobody finds it",
                foldersRow.hasContextMenu(state),
            )

            val rootRow = foldersRow.copy(
                id = XMBViewModel.mediaRootItemId(kind, "content://tree/x"),
                mediaRootUri = "content://tree/x",
            )
            assertTrue(
                "a root row opens Rescan/Relink/Remove and must advertise it",
                rootRow.hasContextMenu(state),
            )

            val addRow = XMBItem(
                id = XMBViewModel.addMediaRootItemId(kind),
                title = "Add Folder",
                type = XMBItemType.ADD_ACTION,
                mediaRootKind = kind,
            )
            assertFalse(
                "Add Folder does its one job on confirm; giving it the library menu titles that menu " +
                    "'Add Folder' and gives the row two meanings",
                addRow.hasContextMenu(state),
            )
        }
    }

    @Test
    fun `a root row's verb says what confirm does, which is open its menu`() {
        val root = XMBItem(
            id = "romroot_x",
            title = "ROMs",
            type = XMBItemType.MEDIA_ROOT,
            mediaRootUri = "content://tree/x",
        )

        assertEquals(
            "confirm on a root raises Rescan/Relink/Remove; promising Open makes it read like a drill-in",
            "Manage",
            primaryVerbFor(root),
        )
    }
}
