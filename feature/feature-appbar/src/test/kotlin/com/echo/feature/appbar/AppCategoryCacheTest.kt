package com.echo.feature.appbar

import android.content.Context
import android.content.pm.LauncherApps
import com.echo.core.data.database.dao.AppOverrideDao
import com.echo.core.data.database.dao.CategoryDao
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.async
import org.junit.Assert.assertEquals
import org.junit.Test

// owner, 2026-10-04: a newly installed app stayed out of search until a reboot
class AppCategoryCacheTest {
    private fun app(pkg: String) = InstalledApp(pkg, pkg, icon = null, isGame = false, isEmulator = false)

    private val installed = mockk<InstalledAppRepository>()
    private val repo = AppCategoryRepository(
        context = mockk<Context> { every { getSystemService(LauncherApps::class.java) } returns null },
        installedAppRepository = installed,
        classifier = mockk(relaxed = true),
        categoryDao = mockk<CategoryDao> { every { observeAppItems() } returns flowOf(emptyList()) },
        appOverrideDao = mockk<AppOverrideDao> { every { observeAll() } returns flowOf(emptyList()) },
    )

    @Test
    fun `an installed package shows up after the system reports it, without a restart`() = runTest {
        coEvery { installed.getInstalledApps() } returnsMany listOf(listOf(app("a")), listOf(app("a"), app("new")))
        assertEquals(listOf("a"), repo.allInstalledApps().map { it.packageName })
        assertEquals("read again from the cache until a package changes", listOf("a"), repo.allInstalledApps().map { it.packageName })

        repo.onPackagesChanged()
        assertEquals(listOf("a", "new"), repo.allInstalledApps().map { it.packageName })
    }

    @Test
    fun `a package change tells the crossbar's app columns to reload`() = runTest {
        val seen = async { repo.changes().take(2).toList() }
        repo.changes().first()
        repo.onPackagesChanged()
        assertEquals(2, seen.await().size)
    }
}
