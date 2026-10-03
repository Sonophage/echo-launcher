package com.psplauncher.core.data.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.psplauncher.core.data.database.entity.AccountAchievementEntity
import kotlinx.coroutines.flow.Flow

data class AchievementTotalsRow(
    val unlocked: Int,
    val total: Int,
    @ColumnInfo(name = "ra_points") val raPoints: Int,
)

@Dao
interface AccountAchievementDao {

    @Query(
        "SELECT * FROM account_achievements " +
            "WHERE provider = :provider AND provider_game_id = :providerGameId"
    )
    suspend fun getForSet(provider: String, providerGameId: String): List<AccountAchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AccountAchievementEntity>)

    @Query(
        "DELETE FROM account_achievements " +
            "WHERE provider = :provider AND provider_game_id = :providerGameId"
    )
    suspend fun deleteForSet(provider: String, providerGameId: String)

    @Query(
        "SELECT * FROM account_achievements " +
            "WHERE provider = :provider AND provider_game_id = :providerGameId " +
            "ORDER BY is_earned DESC, earned_at DESC, title ASC"
    )
    fun observeForSet(provider: String, providerGameId: String): Flow<List<AccountAchievementEntity>>

    @Query(
        "SELECT COALESCE(SUM(is_earned), 0) AS unlocked, COUNT(*) AS total, " +
            "COALESCE(SUM(CASE WHEN is_earned = 1 AND provider = :pointsProvider THEN points ELSE 0 END), 0) AS ra_points " +
            "FROM account_achievements"
    )
    fun observeTotals(pointsProvider: String): Flow<AchievementTotalsRow>
}
