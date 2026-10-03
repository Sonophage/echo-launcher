package com.psplauncher.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.psplauncher.core.data.database.entity.SteamNoAchievementsEntity
import com.psplauncher.core.data.database.entity.SteamOwnedGameEntity

@Dao
interface SteamOwnedGamesDao {

    @Query("SELECT * FROM steam_owned_games")
    suspend fun getAll(): List<SteamOwnedGameEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(games: List<SteamOwnedGameEntity>)

    @Transaction
    suspend fun replaceOwned(games: List<SteamOwnedGameEntity>) {
        val bookmarks = getAll().associate { it.appid to it.syncedPlaytimeMinutes }
        clearOwned()
        upsertAll(games.map { it.copy(syncedPlaytimeMinutes = bookmarks[it.appid]) })
    }

    @Query("DELETE FROM steam_owned_games")
    suspend fun clearOwned()

    @Query("UPDATE steam_owned_games SET synced_playtime_minutes = playtime_forever_minutes WHERE appid = :appid")
    suspend fun markSynced(appid: String)

    @Query("SELECT appid FROM steam_no_achievements")
    suspend fun noAchievementAppids(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun rememberNoAchievements(entity: SteamNoAchievementsEntity)
}
