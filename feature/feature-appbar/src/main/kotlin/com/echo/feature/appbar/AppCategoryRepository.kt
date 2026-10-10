package com.echo.feature.appbar

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.echo.core.data.database.dao.AppOverrideDao
import com.echo.core.data.database.dao.CategoryDao
import com.echo.core.data.database.entity.AppOverrideEntity
import com.echo.core.data.database.entity.CategoryItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class CategorizedApp(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val pinned: Boolean,
    val isEmulator: Boolean,
)

private const val ITEM_TYPE_APP = "app"

@Singleton
class AppCategoryRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val installedAppRepository: InstalledAppRepository,
    private val classifier: AppClassifier,
    private val categoryDao: CategoryDao,
    private val appOverrideDao: AppOverrideDao,
    private val hiddenPlacementDao: com.echo.core.data.database.dao.HiddenPlacementDao,
    private val gameRepository: com.echo.core.domain.repository.GameRepository,
) {
    private val forgetScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    @Volatile private var cache: List<InstalledApp> = emptyList()

    // an install, update or removal empties the cache and tells every reader (owner, 2026-10-04: a new
    // app stayed out of search until a reboot, because the cache was filled once per process)
    private val packageChanges = MutableStateFlow(0)

    init {
        context.getSystemService(LauncherApps::class.java)?.registerCallback(
            object : LauncherApps.Callback() {
                override fun onPackageAdded(packageName: String?, user: UserHandle?) = onPackagesChanged()
                override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
                    onPackagesChanged()
                    // an update arrives as onPackageChanged, so this is an uninstall from this profile
                    if (packageName != null && user == android.os.Process.myUserHandle()) {
                        forgetScope.launch { runCatching { forgetUninstalled(packageName) }.onFailure { Timber.w(it, "Forgetting $packageName failed") } }
                    }
                }
                override fun onPackageChanged(packageName: String?, user: UserHandle?) = onPackagesChanged()
                override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = onPackagesChanged()
                override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = onPackagesChanged()
            },
            Handler(Looper.getMainLooper()),
        )
    }

    // owner, 2026-10-05: an app opened after ECHO started never reached Recent, because its last-used
    // time was read once with the list. Coming back to ECHO re-reads only the times, not the apps
    private val lastUsedChanges = MutableStateFlow(0)

    fun lastUsedChanges(): Flow<Int> = lastUsedChanges

    suspend fun refreshLastUsed() {
        if (cache.isEmpty()) return
        val lastUsed = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { installedAppRepository.loadLastUsedTimestamps() }
        cache = cache.map { it.copy(lastUsedAt = lastUsed[it.packageName] ?: 0L) }
        lastUsedChanges.update { it + 1 }
    }

    // owner, 2026-10-10: an uninstalled app leaves nothing behind: its name and hide override, its places in
    // columns, the spots it was hidden from, and its entry in the Android library (with its play history)
    internal suspend fun forgetUninstalled(pkg: String) {
        appOverrideDao.delete(pkg)
        categoryDao.removeAppFromAllCategories(pkg)
        hiddenPlacementDao.deleteAllForItem(com.echo.core.domain.model.HiddenPlacement.appKey(pkg))
        gameRepository.getAppEntry(pkg)?.let { gameRepository.delete(it.id) }
        Timber.i("Forgot uninstalled app $pkg")
    }

    internal fun onPackagesChanged() {
        cache = emptyList()
        packageChanges.update { it + 1 }
    }

    fun changes(): Flow<Unit> =
        combine(categoryDao.observeAppItems(), appOverrideDao.observeAll(), packageChanges) { _, _, _ -> }

    suspend fun ensureLoaded() {
        if (cache.isEmpty()) cache = installedAppRepository.getInstalledApps()
    }

    suspend fun refresh() {
        cache = installedAppRepository.getInstalledApps()
    }

    private suspend fun installedApps(): List<InstalledApp> {
        ensureLoaded()
        return cache
    }

    suspend fun allInstalledApps(): List<InstalledApp> = installedApps()

    // owner, 2026-10-05: Hide Everywhere hides an app from the whole system: the drawer, Recent and Search too
    suspend fun hiddenEverywhere(): Set<String> =
        appOverrideDao.getAll().filter { it.isHidden }.map { it.packageName }.toSet()

    suspend fun visibleInstalledApps(): List<InstalledApp> {
        val hidden = hiddenEverywhere()
        return installedApps().filterNot { it.packageName in hidden }
    }

    suspend fun packagesIn(categoryId: String): Set<String> {
        val explicit = categoryDao.getAppItems()
            .filter { it.categoryId == categoryId }
            .map { it.itemId }
            .toSet()
        val implicit = installedApps()
            .filter { app -> appOverrideDao.getByPackage(app.packageName)?.customized != true }
            .filter { categoryId in classifier.defaultCategories(it) }
            .map { it.packageName }
        return explicit + implicit
    }

    private fun appByPackage(pkg: String): InstalledApp? = cache.firstOrNull { it.packageName == pkg }

    suspend fun appsForCategory(categoryId: String): List<CategorizedApp> {
        val apps      = installedApps()
        val overrides = appOverrideDao.getAll().associateBy { it.packageName }
        val itemsByPkg = categoryDao.getAppItems().groupBy { it.itemId }

        return apps.mapNotNull { app ->
            val ov = overrides[app.packageName]
            if (ov?.isHidden == true) return@mapNotNull null

            val pinned: Boolean
            if (ov?.customized == true) {
                val row = itemsByPkg[app.packageName].orEmpty().firstOrNull { it.categoryId == categoryId }
                    ?: return@mapNotNull null
                pinned = row.pinned
            } else {
                if (categoryId !in classifier.defaultCategories(app)) return@mapNotNull null
                pinned = false
            }

            CategorizedApp(
                packageName = app.packageName,
                label       = ov?.customLabel?.takeIf { it.isNotBlank() } ?: app.label,
                icon        = app.icon ?: return@mapNotNull null,
                pinned      = pinned,
                isEmulator  = app.isEmulator,
            )
        }.sortedWith(compareByDescending<CategorizedApp> { it.pinned }.thenBy { it.label.lowercase() })
    }

    suspend fun moveToCategory(pkg: String, categoryId: String) {
        markCustomized(pkg)
        categoryDao.removeAppFromAllCategories(pkg)
        categoryDao.addItem(CategoryItemEntity(categoryId, pkg, ITEM_TYPE_APP))
        Timber.i("App $pkg moved to category $categoryId")
    }

    suspend fun addToCategory(pkg: String, categoryId: String) {
        materialize(pkg)
        categoryDao.addItem(CategoryItemEntity(categoryId, pkg, ITEM_TYPE_APP))
        Timber.i("App $pkg added to category $categoryId")
    }

    suspend fun removeFromCategory(pkg: String, categoryId: String) {
        materialize(pkg)
        categoryDao.removeItem(categoryId, pkg)
        Timber.i("App $pkg removed from category $categoryId")
    }

    suspend fun pinToCategory(pkg: String, categoryId: String) {
        materialize(pkg)
        if (categoryDao.getCategoriesForApp(pkg).none { it.categoryId == categoryId }) {
            categoryDao.addItem(CategoryItemEntity(categoryId, pkg, ITEM_TYPE_APP))
        }
        categoryDao.setItemPinned(categoryId, pkg, true)
    }

    suspend fun setHidden(pkg: String, hidden: Boolean) {
        val ov = appOverrideDao.getByPackage(pkg) ?: AppOverrideEntity(pkg)
        appOverrideDao.upsert(ov.copy(isHidden = hidden))
    }

    suspend fun rename(pkg: String, label: String?) {
        val ov = appOverrideDao.getByPackage(pkg) ?: AppOverrideEntity(pkg)
        appOverrideDao.upsert(ov.copy(customLabel = label?.trim()?.takeIf { it.isNotBlank() }))
    }

    fun launch(pkg: String) = installedAppRepository.launchApp(pkg)

    private suspend fun materialize(pkg: String) {
        val ov = appOverrideDao.getByPackage(pkg)
        if (ov?.customized == true) return
        val autoCats = appByPackage(pkg)?.let { classifier.defaultCategories(it) } ?: emptySet()
        autoCats.forEach { categoryDao.addItem(CategoryItemEntity(it, pkg, ITEM_TYPE_APP)) }
        markCustomized(pkg)
    }

    private suspend fun markCustomized(pkg: String) {
        val ov = appOverrideDao.getByPackage(pkg) ?: AppOverrideEntity(pkg)
        if (!ov.customized) appOverrideDao.upsert(ov.copy(customized = true))
    }
}
