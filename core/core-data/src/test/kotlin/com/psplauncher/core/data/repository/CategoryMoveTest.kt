package com.psplauncher.core.data.repository

import com.psplauncher.core.data.database.dao.CategoryDao
import com.psplauncher.core.data.database.entity.CategoryEntity
import com.psplauncher.core.domain.model.CategoryType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryMoveTest {
    private class FakeCategories(seed: List<CategoryEntity>) {
        val rows = seed.associateBy { it.id }.toMutableMap()
        val dao: CategoryDao = mockk(relaxed = true)

        init {
            coEvery { dao.getAll() } coAnswers {
                val snapshot = rows.values.sortedBy { it.position }
                yield()
                snapshot
            }
            coEvery { dao.updatePosition(any(), any()) } answers {
                val id = firstArg<String>()
                rows[id] = rows.getValue(id).copy(position = secondArg())
                Unit
            }
        }

        fun order(): List<String> = rows.values.sortedBy { it.position }.map { it.id }
        fun positions(): List<Int> = rows.values.map { it.position }
    }

    private fun category(id: String, position: Int) = CategoryEntity(
        id = id, name = id, iconKey = "ic_$id", type = CategoryType.MANUAL.name, position = position,
    )

    @Test
    fun `two quick moves land both, with unique positions`() = runTest {
        val fake = FakeCategories(listOf(category("a", 0), category("b", 1), category("c", 2)))
        val repo = CategoryRepositoryImpl(fake.dao)

        launch { repo.move("c", up = true) }
        launch { repo.move("c", up = true) }
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("c", "a", "b"), fake.order())
        assertEquals(3, fake.positions().toSet().size)
    }

    @Test
    fun `a move swaps with the next row the manager shows, not a hidden legacy row`() = runTest {
        val fake = FakeCategories(
            listOf(category("a", 0), category("music_apps", 1), category("b", 2)),
        )

        CategoryRepositoryImpl(fake.dao).move("b", up = true)

        assertEquals(listOf("b", "music_apps", "a"), fake.order())
    }
}
