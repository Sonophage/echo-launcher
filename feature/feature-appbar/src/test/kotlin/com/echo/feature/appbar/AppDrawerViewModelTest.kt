package com.echo.feature.appbar

import android.graphics.drawable.Drawable
import app.cash.turbine.test
import com.echo.core.domain.model.GamepadAction
import io.mockk.coEvery
import io.mockk.every
import io.mockk.verify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppDrawerViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: InstalledAppRepository
    private lateinit var games: com.echo.core.domain.repository.GameRepository
    private lateinit var viewModel: AppDrawerViewModel
    private val appCategories = mockk<AppCategoryRepository>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        coEvery { repository.getInstalledApps() } returns fakeApps()
        every { repository.hasUsageAccess() } returns true
        games = mockk(relaxed = true)
        every { games.observeAllGames() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        viewModel = AppDrawerViewModel(
            repository,
            mockk(relaxed = true),
            games,
            mockk(relaxed = true),

            mockk(relaxed = true),
            mockk(relaxed = true),
            appCategories,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `A opens an app only after the hold, and letting go early opens nothing`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.handleGamepadAction(GamepadAction.SELECT)
        testDispatcher.scheduler.advanceTimeBy(com.echo.core.ui.design.LAUNCH_HOLD_MS / 2)
        viewModel.onSelectReleased()
        testDispatcher.scheduler.advanceUntilIdle()
        verify(exactly = 0) { repository.launchApp(any()) }

        viewModel.handleGamepadAction(GamepadAction.SELECT)
        testDispatcher.scheduler.advanceUntilIdle()
        verify(exactly = 1) { repository.launchApp(any()) }
    }

    @Test
    fun `the drawer opens on Recently Used, not on the full alphabetical list`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(AppFilter.RECENT, state.activeFilter)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `APPS shows neither emulators nor games, and an app that is both is in both`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setFilter(AppFilter.APPS)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.test {
            val apps = awaitItem().visibleApps
            assertTrue("Apps must contain no emulators", apps.none { it.isEmulator })
            assertTrue("Apps must contain no games", apps.none { it.isGame })
            cancelAndIgnoreRemainingEvents()
        }

        val both = fakeApps().filter { it.isEmulator && it.isGame }.map { it.label }
        assertTrue("the fixture must contain an app that is both, or this proves nothing", both.isNotEmpty())

        viewModel.setFilter(AppFilter.EMULATORS)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.test {
            assertTrue(awaitItem().visibleApps.map { it.label }.containsAll(both))
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.setFilter(AppFilter.GAMES)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.test {
            assertTrue(
                "an app that is both an emulator and a game must appear under Games too",
                awaitItem().visibleApps.map { it.label }.containsAll(both),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `EMULATORS filter shows only emulator-tagged apps`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setFilter(AppFilter.EMULATORS)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue("the wall holds only emulators", state.visibleApps.all { it.isEmulator })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GAMES filter shows only game-tagged apps`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setFilter(AppFilter.GAMES)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue("the wall holds only games", state.visibleApps.all { it.isGame })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `RECENT filter shows timestamped apps newest first`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setFilter(AppFilter.RECENT)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(listOf("Minecraft", "Browser"), state.visibleApps.map { it.label })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `usage access state reflects repository result`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.hasUsageAccess)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `openUsageAccessSettings delegates to repository`() = runTest {
        viewModel.openUsageAccessSettings()
        verify { repository.openUsageAccessSettings() }
    }

    // the footer said Menu = Search while the hero said X = Options; Menu is Options everywhere (owner)
    @Test
    fun `X does not open the options menu, Menu does`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setFilter(AppFilter.EMULATORS)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.handleGamepadAction(GamepadAction.CHANGE_SORT)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(null, viewModel.uiState.value.menuApp)
        viewModel.handleGamepadAction(GamepadAction.OPEN_CONTEXT_MENU)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Dolphin", viewModel.uiState.value.menuApp?.label)
    }

    @Test
    fun `back on the open options menu closes just the menu`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setFilter(AppFilter.EMULATORS)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleGamepadAction(GamepadAction.OPEN_CONTEXT_MENU)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Dolphin", state.menuApp?.label)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.handleGamepadAction(GamepadAction.BACK)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(null, state.menuApp)
            assertEquals(null, state.confirmUninstall)
            assertEquals(0, state.selectedIndex)
            assertEquals(fakeApps().count { it.isEmulator }, state.visibleApps.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // owner, 2026-10-05: Hide Everywhere hides an app from the whole system, the drawer included
    @Test
    fun `an app hidden everywhere is not in the drawer`() = runTest {
        coEvery { appCategories.hiddenEverywhere() } returns setOf("com.example.browser")
        viewModel = AppDrawerViewModel(
            repository,
            mockk(relaxed = true),
            games,
            mockk(relaxed = true),

            mockk(relaxed = true),
            mockk(relaxed = true),
            appCategories,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.allApps.any { it.packageName == "com.example.browser" })
        assertTrue(viewModel.uiState.value.allApps.any { it.packageName == "com.mojang.minecraftpe" })
    }

    // owner, 2026-10-05: an app that is never used can be hidden from the whole layout, from the drawer
    @Test
    fun `Hide Everywhere from the drawer hides the app and drops it from the drawer`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val browser = viewModel.uiState.value.allApps.first { it.packageName == "com.example.browser" }
        viewModel.openAppMenu(browser)
        assertTrue(AppMenuAction.HIDE_EVERYWHERE in viewModel.uiState.value.menuActions)

        coEvery { appCategories.hiddenEverywhere() } returns setOf("com.example.browser")
        viewModel.onMenuAction(AppMenuAction.HIDE_EVERYWHERE)
        testDispatcher.scheduler.advanceUntilIdle()

        io.mockk.coVerify { appCategories.setHidden("com.example.browser", true) }
        assertEquals("the menu goes once a row is chosen", null, viewModel.uiState.value.appMenu)
        assertFalse(viewModel.uiState.value.allApps.any { it.packageName == "com.example.browser" })
    }

    private fun drawerWithNothingUsed(usageAccess: Boolean): AppDrawerViewModel {
        coEvery { repository.getInstalledApps() } returns fakeApps().map { it.copy(lastUsedAt = 0L) }
        every { repository.hasUsageAccess() } returns usageAccess
        return AppDrawerViewModel(
            repository,
            mockk(relaxed = true),
            games,
            mockk(relaxed = true),

            mockk(relaxed = true),
            mockk(relaxed = true),
            appCategories,
        )
    }

    // owner, 2026-10-05: an empty section is not shown, and LT/RT step over it
    @Test
    fun `an empty Recently Used is neither shown nor stepped onto`() = runTest {
        viewModel = drawerWithNothingUsed(usageAccess = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(AppFilter.APPS, AppFilter.EMULATORS, AppFilter.GAMES), state.sections)
        assertEquals("the drawer opens on the first section that has apps", AppFilter.APPS, state.activeFilter)

        viewModel.handleGamepadAction(GamepadAction.PREV_CATEGORY)
        assertEquals(AppFilter.GAMES, viewModel.uiState.value.activeFilter)
        viewModel.handleGamepadAction(GamepadAction.NEXT_CATEGORY)
        assertEquals(AppFilter.APPS, viewModel.uiState.value.activeFilter)
    }

    @Test
    fun `without usage access Recently Used stays, since its page is where access is granted`() = runTest {
        viewModel = drawerWithNothingUsed(usageAccess = false)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AppFilter.entries, state.sections)
        assertEquals(AppFilter.RECENT, state.activeFilter)
        assertTrue("the section itself is empty", state.visibleApps.isEmpty())
    }

    @Test
    fun `LB and RB wrap around the tabs, like the Tabs hint and the settings tabs`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setFilter(AppFilter.entries.last())

        viewModel.handleGamepadAction(GamepadAction.NEXT_CATEGORY)
        assertEquals(AppFilter.entries.first(), viewModel.uiState.value.activeFilter)

        viewModel.handleGamepadAction(GamepadAction.PREV_CATEGORY)
        assertEquals(AppFilter.entries.last(), viewModel.uiState.value.activeFilter)
    }

    @Test
    fun `Add to Cross Bar from the controller hands the app to the cross bar, as a tap does`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setFilter(AppFilter.EMULATORS)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.handleGamepadAction(GamepadAction.OPEN_CONTEXT_MENU)
        testDispatcher.scheduler.advanceUntilIdle()
        val app = viewModel.uiState.value.menuApp!!.packageName
        assertEquals(AppMenuAction.ADD_TO_CROSS_BAR, viewModel.uiState.value.appMenu!!.rows[0].action)

        viewModel.handleGamepadAction(GamepadAction.SELECT)

        assertEquals(app, viewModel.uiState.value.pendingCrossBarAdd)
        viewModel.onCrossBarAddHandled()
        assertEquals(null, viewModel.uiState.value.pendingCrossBarAdd)
    }

    @Test
    fun `a refresh that hides the system chips takes the focus off them, so left and right move the wall again`() = runTest {
        val roms = listOf(
            com.echo.core.domain.model.Game(id = 1, title = "One", platformId = "psp"),
            com.echo.core.domain.model.Game(id = 2, title = "Two", platformId = "snes"),
        )
        every { games.observeAllGames() } returns kotlinx.coroutines.flow.flowOf(roms)
        val vm = drawerOver(fakeApps())
        testDispatcher.scheduler.advanceUntilIdle()
        vm.setFilter(AppFilter.GAMES)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue("the fixture must show the chip row", vm.uiState.value.showSystemChips)
        vm.handleGamepadAction(GamepadAction.NAVIGATE_UP)
        assertTrue(vm.uiState.value.chipFocus)

        every { games.observeAllGames() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        vm.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse("the chip row is hidden", vm.uiState.value.showSystemChips)
        assertFalse(vm.uiState.value.chipFocus)
    }

    @Test
    fun `back after opening uninstall guard rail closes the dialog not the drawer`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setFilter(AppFilter.EMULATORS)
        testDispatcher.scheduler.advanceUntilIdle()

        openUninstallPrompt()
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Dolphin", state.confirmUninstall?.label)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.handleGamepadAction(GamepadAction.BACK)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(null, state.confirmUninstall)
            assertEquals(null, state.menuApp)
            assertEquals(fakeApps().count { it.isEmulator }, state.visibleApps.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun openUninstallPrompt() {
        viewModel.handleGamepadAction(GamepadAction.OPEN_CONTEXT_MENU)
        testDispatcher.scheduler.advanceUntilIdle()
        val actions = viewModel.uiState.value.menuActions
        val target = actions.indexOf(AppMenuAction.UNINSTALL)
        assertTrue("the menu offered no Uninstall row: $actions", target >= 0)
        repeat(target) { viewModel.handleGamepadAction(GamepadAction.NAVIGATE_DOWN) }
        viewModel.handleGamepadAction(GamepadAction.SELECT)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `the uninstall prompt opens with the cursor on Cancel`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setFilter(AppFilter.EMULATORS)
        testDispatcher.scheduler.advanceUntilIdle()
        openUninstallPrompt()
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Dolphin", state.confirmUninstall?.label)
            assertFalse("prompt opened on the destructive button", state.uninstallConfirmFocused)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pressing confirm the instant the prompt opens does not uninstall anything`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        openUninstallPrompt()
        viewModel.handleGamepadAction(GamepadAction.SELECT)
        testDispatcher.scheduler.advanceUntilIdle()
        verify(exactly = 0) { repository.uninstallApp(any()) }
        viewModel.uiState.test {
            assertEquals(null, awaitItem().confirmUninstall)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `moving to Uninstall and confirming does uninstall`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setFilter(AppFilter.EMULATORS)
        testDispatcher.scheduler.advanceUntilIdle()
        openUninstallPrompt()
        viewModel.handleGamepadAction(GamepadAction.NAVIGATE_DOWN)
        viewModel.handleGamepadAction(GamepadAction.SELECT)
        testDispatcher.scheduler.advanceUntilIdle()
        verify(exactly = 1) { repository.uninstallApp("org.dolphinemu.dolphinemu") }
    }

    @Test
    fun `the cursor moves back off the destructive button`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        openUninstallPrompt()
        viewModel.handleGamepadAction(GamepadAction.NAVIGATE_DOWN)
        viewModel.handleGamepadAction(GamepadAction.NAVIGATE_UP)
        viewModel.handleGamepadAction(GamepadAction.SELECT)
        testDispatcher.scheduler.advanceUntilIdle()
        verify(exactly = 0) { repository.uninstallApp(any()) }
    }

    @Test
    fun `a reopened prompt starts on Cancel again, whatever the last answer was`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        openUninstallPrompt()
        viewModel.handleGamepadAction(GamepadAction.NAVIGATE_DOWN)
        viewModel.handleGamepadAction(GamepadAction.BACK)
        testDispatcher.scheduler.advanceUntilIdle()
        openUninstallPrompt()
        viewModel.uiState.test {
            assertFalse("cursor was remembered across prompts", awaitItem().uninstallConfirmFocused)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isLoading is false after initial load completes`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private val fakeDrawable: Drawable = mockk(relaxed = true)

    private fun alphabetApps(): List<InstalledApp> =
        ('A'..'J').flatMap { letter ->
            (0 until 26).map {
                InstalledApp(
                    packageName = "pkg.${letter.lowercaseChar()}$it",
                    label = "$letter app $it",
                    icon = fakeDrawable,
                    isEmulator = false,
                    isGame = false,
                )
            }
        }

    private fun drawerOver(apps: List<InstalledApp>): AppDrawerViewModel {
        coEvery { repository.getInstalledApps() } returns apps
        return AppDrawerViewModel(repository, mockk(relaxed = true), games, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), appCategories)
    }

    private fun pick(vm: AppDrawerViewModel, letter: Char) {
        val rung = vm.uiState.value.letterMenu.indexOf(letter)
        assertTrue("the strip must actually offer '$letter'", rung >= 0)
        vm.onLetterRailTouch(rung)
        vm.onLetterRailReleased()
    }

    @Test
    fun `picking a letter leaves only the apps that start with it`() = runTest {
        val vm = drawerOver(alphabetApps())
        testDispatcher.scheduler.advanceUntilIdle()
        vm.setFilter(AppFilter.APPS)
        testDispatcher.scheduler.advanceUntilIdle()

        val before = vm.uiState.value.visibleApps
        assertTrue(
            "the fixture must hold apps under other letters, or filtering proves nothing",
            before.any { !it.label.startsWith("C") },
        )

        pick(vm, 'C')
        testDispatcher.scheduler.advanceUntilIdle()

        val after = vm.uiState.value.visibleApps
        assertTrue("something must survive the filter", after.isNotEmpty())
        assertTrue(
            "an app under another letter stayed in the drawer: ${after.map { it.label }}",
            after.all { it.label.startsWith("C") },
        )
    }

    @Test
    fun `the strip keeps offering every letter while one of them is filtering`() = runTest {
        val vm = drawerOver(alphabetApps())
        testDispatcher.scheduler.advanceUntilIdle()
        vm.setFilter(AppFilter.APPS)
        testDispatcher.scheduler.advanceUntilIdle()
        val whole = vm.uiState.value.letterMenu

        pick(vm, 'C')
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "the strip reads off the filtered list, so picking C collapsed it to one rung",
            whole,
            vm.uiState.value.letterMenu,
        )
    }

    @Test
    fun `picking the letter that is already filtering clears it`() = runTest {
        val vm = drawerOver(alphabetApps())
        testDispatcher.scheduler.advanceUntilIdle()
        vm.setFilter(AppFilter.APPS)
        testDispatcher.scheduler.advanceUntilIdle()
        val whole = vm.uiState.value.visibleApps.size

        pick(vm, 'C')
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue("first pick must narrow the drawer", vm.uiState.value.visibleApps.size < whole)

        pick(vm, 'C')
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("picking C again is how you put the whole drawer back", null, vm.uiState.value.letterFilter)
        assertEquals(whole, vm.uiState.value.visibleApps.size)
    }

    @Test
    fun `a filtered tab shows only its own apps, with nothing from the rest of the drawer below`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        listOf(AppFilter.APPS, AppFilter.EMULATORS, AppFilter.GAMES, AppFilter.RECENT).forEach { filter ->
            viewModel.setFilter(filter)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(
                "$filter must show exactly the apps it matches",
                fakeApps().filter(filter::matches).map { it.packageName }.sorted(),
                viewModel.uiState.value.visibleApps.map { it.packageName }.sorted(),
            )
        }
    }

    @Test
    fun `back puts the whole drawer back instead of leaving it`() = runTest {
        val vm = drawerOver(alphabetApps())
        testDispatcher.scheduler.advanceUntilIdle()
        vm.setFilter(AppFilter.APPS)
        testDispatcher.scheduler.advanceUntilIdle()
        val whole = vm.uiState.value.visibleApps.size

        pick(vm, 'C')
        testDispatcher.scheduler.advanceUntilIdle()
        vm.clearLetterFilter()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, vm.uiState.value.letterFilter)
        assertEquals("back restores every app, not just the ones under C", whole, vm.uiState.value.visibleApps.size)
    }

    private fun fakeApps() = listOf(
        InstalledApp(packageName = "com.example.browser",          label = "Browser",   icon = fakeDrawable, isEmulator = false, isGame = false, lastUsedAt = 1_000L),
        InstalledApp(packageName = "org.dolphinemu.dolphinemu",    label = "Dolphin",   icon = fakeDrawable, isEmulator = true,  isGame = true),
        InstalledApp(packageName = "com.mojang.minecraftpe",       label = "Minecraft", icon = fakeDrawable, isEmulator = false, isGame = true, lastUsedAt = 2_000L),
        InstalledApp(packageName = "com.echo.launcher", label = "ECHO",      icon = fakeDrawable, isEmulator = false, isGame = false),
        InstalledApp(packageName = "org.ppsspp.ppsspp",           label = "PPSSPP",    icon = fakeDrawable, isEmulator = true,  isGame = false),
        InstalledApp(packageName = "com.retroarch",                label = "RetroArch", icon = fakeDrawable, isEmulator = true,  isGame = false),
    )
}
