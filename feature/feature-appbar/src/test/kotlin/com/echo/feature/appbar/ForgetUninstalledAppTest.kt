package com.echo.feature.appbar

import android.content.Context
import android.content.pm.LauncherApps
import com.echo.core.data.database.dao.AppOverrideDao
import com.echo.core.data.database.dao.CategoryDao
import com.echo.core.data.database.dao.HiddenPlacementDao
import com.echo.core.domain.model.Game
import com.echo.core.domain.repository.GameRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

// owner, 2026-10-10: an uninstalled app leaves nothing of itself behind
class ForgetUninstalledAppTest {
    private val categoryDao = mockk<CategoryDao>(relaxed = true) { every { observeAppItems() } returns flowOf(emptyList()) }
    private val appOverrideDao = mockk<AppOverrideDao>(relaxed = true) { every { observeAll() } returns flowOf(emptyList()) }
    private val hiddenPlacementDao = mockk<HiddenPlacementDao>(relaxed = true)
    private val gameRepository = mockk<GameRepository>(relaxed = true)
    private val repo = AppCategoryRepository(
        context = mockk<Context> { every { getSystemService(LauncherApps::class.java) } returns null },
        installedAppRepository = mockk(relaxed = true),
        classifier = mockk(relaxed = true),
        categoryDao = categoryDao,
        appOverrideDao = appOverrideDao,
        hiddenPlacementDao = hiddenPlacementDao,
        gameRepository = gameRepository,
    )

    @Test
    fun `forgetting an app clears its override, columns, hidden spots and library entry`() = runTest {
        coEvery { gameRepository.getAppEntry("com.gone") } returns mockk<Game> { every { id } returns 7L }

        repo.forgetUninstalled("com.gone")

        coVerify(exactly = 1) { appOverrideDao.delete("com.gone") }
        coVerify(exactly = 1) { categoryDao.removeAppFromAllCategories("com.gone") }
        coVerify(exactly = 1) { hiddenPlacementDao.deleteAllForItem("app:com.gone") }
        coVerify(exactly = 1) { gameRepository.delete(7L) }
    }
}
