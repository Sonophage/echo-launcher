package com.echo.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.echo.core.data.database.entity.ProviderGameLinkEntity

@Dao
interface ProviderGameLinkDao {

    @Query("SELECT * FROM provider_game_links WHERE game_id = :gameId ORDER BY provider LIMIT 1")
    suspend fun getForGame(gameId: Long): ProviderGameLinkEntity?

    @Query("SELECT * FROM provider_game_links")
    suspend fun getAll(): List<ProviderGameLinkEntity>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM provider_game_links " +
            "WHERE provider = :provider AND provider_game_id = :providerGameId)"
    )
    suspend fun linkExistsFor(provider: String, providerGameId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(link: ProviderGameLinkEntity)

    @Query("DELETE FROM provider_game_links WHERE game_id = :gameId")
    suspend fun deleteForGame(gameId: Long)
}
