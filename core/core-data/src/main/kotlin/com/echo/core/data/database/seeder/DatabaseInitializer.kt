package com.echo.core.data.database.seeder

import com.echo.core.domain.model.PlatformIds.ANDROID as ANDROID_PLATFORM_ID
import com.echo.core.domain.model.PlatformIds.WINDOWS as WINDOWS_PLATFORM_ID

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.echo.core.data.database.dao.PlatformDao
import com.echo.core.data.database.entity.MemoryCardEntity
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.data.repository.CategoryRepositoryImpl
import com.echo.core.data.repository.WindowsLibrarySetup
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val KEY_DB_SEEDED     = booleanPreferencesKey("db_seeded_v1")

private val KEY_LAST_PLAYED_PLACED = booleanPreferencesKey("last_played_placed_v1")

private val KEY_NETWORK_RENAMED = booleanPreferencesKey("network_renamed_v1")

private val KEY_SETTINGS_ON_PANEL = booleanPreferencesKey("settings_on_panel_v1")

private val KEY_ANDROID_CARD_SEEDED = booleanPreferencesKey("android_card_seeded_v1")

internal enum class AndroidCardSeed { CREATE_AND_MARK, MARK_ONLY, NOTHING }

internal fun androidCardSeedAction(alreadySeeded: Boolean, cardExists: Boolean): AndroidCardSeed =
    when {
        alreadySeeded -> AndroidCardSeed.NOTHING
        cardExists -> AndroidCardSeed.MARK_ONLY
        else -> AndroidCardSeed.CREATE_AND_MARK
    }

private const val LEGACY_CARD_SUFFIX = " Memory Card"
private val LEGACY_PC_NAMES = setOf("Windows Games", "Windows Memory Card")

/**
 * The name a system or card takes when it still has a default from before "Memory Card" left the
 * UI (owner, 2026-10-07). Null keeps it: a name the owner chose is never touched.
 */
internal fun renamedFromLegacy(platformId: String, name: String, platformName: String?): String? = when {
    platformId == WINDOWS_PLATFORM_ID && name in LEGACY_PC_NAMES -> WindowsLibrarySetup.DISPLAY_NAME
    platformName != null && name == platformName + LEGACY_CARD_SUFFIX -> platformName
    else -> null
}


@Singleton
class DatabaseInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val platformSeeder: PlatformSeeder,
    private val categoryRepository: CategoryRepositoryImpl,
    private val libraryConsolidation: LibraryConsolidation,
    private val memoryCardDao: com.echo.core.data.database.dao.MemoryCardDao,
    private val platformDao: PlatformDao,
) {
    suspend fun initialize() {
        platformSeeder.seed()
        seedMainDb()

        categoryRepository.reconcileBuiltInCategories()
        placeLastPlayed()
        renameNetworkColumn()
        moveSettingsToPanel()
        seedAndroidCard()

        libraryConsolidation.run()
        dropLegacyNames()
    }

    // Every start, not once: a restored backup can bring the old names back.
    private suspend fun dropLegacyNames() {
        platformDao.getById(WINDOWS_PLATFORM_ID)?.let { p ->
            renamedFromLegacy(p.id, p.name, null)?.let { platformDao.update(p.copy(name = it)) }
        }
        for (card in memoryCardDao.getAll()) {
            val platformName = platformDao.getById(card.platformId)?.name
            renamedFromLegacy(card.platformId, card.displayName, platformName)
                ?.let { memoryCardDao.setDisplayName(card.platformId, it) }
        }
    }

    private suspend fun seedMainDb() {
        val prefs = context.echoDataStore.data.first()
        if (prefs[KEY_DB_SEEDED] == true) {
            Timber.d("DB already seeded — skipping")
            return
        }

        Timber.i("First launch — seeding database")

        categoryRepository.seedBuiltInCategories()

        context.echoDataStore.edit { it[KEY_DB_SEEDED] = true }
        Timber.i("Database seed complete")
    }

    private suspend fun placeLastPlayed() {
        val prefs = context.echoDataStore.data.first()
        if (prefs[KEY_LAST_PLAYED_PLACED] == true) return
        categoryRepository.placeLastPlayedBeforeGames()
        context.echoDataStore.edit { it[KEY_LAST_PLAYED_PLACED] = true }
    }

    private suspend fun renameNetworkColumn() {
        val prefs = context.echoDataStore.data.first()
        if (prefs[KEY_NETWORK_RENAMED] == true) return
        categoryRepository.renameStaleOnlineColumn()
        context.echoDataStore.edit { it[KEY_NETWORK_RENAMED] = true }
    }

    private suspend fun moveSettingsToPanel() {
        val prefs = context.echoDataStore.data.first()
        if (prefs[KEY_SETTINGS_ON_PANEL] == true) return
        categoryRepository.setVisible(com.echo.core.domain.model.BuiltInCategory.SETTINGS, false)
        context.echoDataStore.edit { it[KEY_SETTINGS_ON_PANEL] = true }
    }

    private suspend fun seedAndroidCard() {
        val prefs = context.echoDataStore.data.first()
        val action = androidCardSeedAction(
            alreadySeeded = prefs[KEY_ANDROID_CARD_SEEDED] == true,
            cardExists = memoryCardDao.getById(ANDROID_PLATFORM_ID) != null,
        )
        if (action == AndroidCardSeed.NOTHING) return
        if (action == AndroidCardSeed.CREATE_AND_MARK) {
            memoryCardDao.upsert(
                MemoryCardEntity(
                    platformId  = ANDROID_PLATFORM_ID,
                    displayName = "Android",
                    enabled     = true,
                    sortOrder   = memoryCardDao.maxSortOrder() + 1,
                    gameCount   = 0,
                )
            )
            Timber.i("Android card seeded")
        }
        context.echoDataStore.edit { it[KEY_ANDROID_CARD_SEEDED] = true }
    }

}
