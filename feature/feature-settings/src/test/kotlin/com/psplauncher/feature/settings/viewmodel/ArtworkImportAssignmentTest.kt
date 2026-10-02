package com.psplauncher.feature.settings.viewmodel

import com.psplauncher.feature.artwork.importer.ImportPlan
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkImportAssignmentTest {
    private fun plan(label: String) = ImportPlan(
        sourceId = "src",
        sourceLabel = label,
        treeUri = "content://tree",
        games = emptyList(),
        ambiguous = emptyList(),
        unmatchedCount = 0,
        skippedExistingCount = 0,
    )

    @Test
    fun an_assignment_that_finishes_after_start_import_does_not_bring_the_preview_back() {
        val shown = plan("before")
        val afterStart = ArtworkImportUiState(plan = null, importRunning = true)

        val result = afterStart.withAssignment(basedOn = shown, updated = plan("assigned"))

        assertNull(result.plan)
        assertTrue(result.importRunning)
    }

    @Test
    fun an_assignment_against_a_replaced_plan_is_dropped() {
        val shown = plan("before")
        val replaced = plan("before")
        val state = ArtworkImportUiState(plan = replaced)

        assertSame(replaced, state.withAssignment(basedOn = shown, updated = plan("assigned")).plan)
    }

    @Test
    fun an_assignment_against_the_current_plan_lands() {
        val shown = plan("before")
        val assigned = plan("assigned")
        val state = ArtworkImportUiState(plan = shown, expandedAmbiguousIndex = 0)

        val result = state.withAssignment(basedOn = shown, updated = assigned)

        assertSame(assigned, result.plan)
        assertNull(result.expandedAmbiguousIndex)
    }
}
