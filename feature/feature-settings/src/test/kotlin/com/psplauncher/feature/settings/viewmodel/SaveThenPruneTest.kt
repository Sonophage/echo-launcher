package com.psplauncher.feature.settings.viewmodel

import java.io.File
import java.io.IOException
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SaveThenPruneTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun pruneAllBut(dir: File, keep: File): suspend () -> Unit = {
        dir.listFiles()?.forEach { if (it.name != keep.name) it.delete() }
    }

    @Test fun `a wallpaper whose save fails keeps the old wallpaper file`() = runBlocking {
        val dir = tmp.newFolder("wallpaper")
        val old = File(dir, "wallpaper_1.jpg").apply { writeText("old") }
        val new = File(dir, "wallpaper_2.jpg").apply { writeText("new") }

        val saved = saveThenPrune(
            save = { yield(); throw IOException("disk full") },
            prune = pruneAllBut(dir, keep = new),
        )

        assertFalse(saved, "a failed save must be reported, not shown as applied")
        assertTrue(old.isFile, "the stored path still points at the old file, so it must survive")
    }

    @Test fun `a wallpaper whose save succeeds prunes the old file`() = runBlocking {
        val dir = tmp.newFolder("wallpaper")
        val old = File(dir, "wallpaper_1.jpg").apply { writeText("old") }
        val new = File(dir, "wallpaper_2.jpg").apply { writeText("new") }

        val saved = saveThenPrune(save = { yield() }, prune = pruneAllBut(dir, keep = new))

        assertTrue(saved)
        assertFalse(old.isFile, "the replaced wallpaper must not pile up in storage")
        assertTrue(new.isFile)
    }
}
