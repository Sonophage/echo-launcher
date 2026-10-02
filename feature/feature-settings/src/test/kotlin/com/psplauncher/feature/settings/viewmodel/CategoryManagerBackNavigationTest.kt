package com.psplauncher.feature.settings.viewmodel

import com.psplauncher.core.data.repository.CategoryRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryManagerBackNavigationTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private val repo = mockk<CategoryRepositoryImpl>().also {
        every { it.observeAll() } returns flowOf(emptyList())
        every { it.isProtected(any()) } returns false
        coEvery { it.createCustomCategory(any(), any(), any()) } returns "custom_streaming_0"
    }

    private fun viewModel(): CategoryManagerViewModel = CategoryManagerViewModel(repo)

    @Test
    fun back_at_top_level_is_not_consumed() {
        val vm = viewModel()

        assertFalse(vm.onBack())
    }

    @Test
    fun back_in_submenu_collapses_one_level_then_reaches_top() {
        val vm = viewModel()
        vm.openDetail("videos")
        assertTrue(vm.onBack())
        assertFalse(vm.onBack())
    }

    @Test
    fun back_from_icon_picker_returns_to_list() {
        val vm = viewModel()
        vm.startCreate()
        vm.confirmCreateName("Streaming")
        assertTrue(vm.onBack())
        assertFalse(vm.onBack())
    }

    @Test
    fun a_double_tap_on_the_type_step_creates_one_category() = runTest(dispatcher) {
        val vm = viewModel()
        vm.startCreate()
        vm.confirmCreateName("Streaming")
        vm.chooseIcon("ic_music")
        advanceUntilIdle()

        vm.chooseType(isGaming = false)
        vm.chooseType(isGaming = false)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.createCustomCategory("Streaming", "ic_music", false) }
    }
}
