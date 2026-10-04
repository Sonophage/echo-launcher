package com.echo.core.data.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.echo.core.data.database.entity.AccountAchievementSetEntity
import kotlinx.coroutines.flow.Flow

data class AchievementSetRow(
    val provider: String,
    @ColumnInfo(name = "provider_game_id") val providerGameId: String,
    @ColumnInfo(name = "library_game_id") val libraryGameId: Long?,
    val title: String,
    @ColumnInfo(name = "icon_url") val iconUrl: String?,
    val total: Int,
    val unlocked: Int,
    val points: Int,
    @ColumnInfo(name = "earned_points") val earnedPoints: Int,
    val mastered: Boolean,
    @ColumnInfo(name = "last_synced_at") val lastSyncedAt: Long?,
    @ColumnInfo(name = "last_played_at") val lastPlayedAt: Long?,
    @ColumnInfo(name = "platform_id") val platformId: String? = null,
)

private const val SET_ROW_COLUMNS =
    "s.provider AS provider, s.provider_game_id AS provider_game_id, " +
        "COALESCE(g.user_title_override, g.scraped_title, g.title, s.title) AS title, " +
        "s.icon_url AS icon_url, s.mastered AS mastered, s.last_synced_at AS last_synced_at, " +
        "g.last_played_at AS last_played_at, g.platform_id AS platform_id, " +
        "(SELECT COUNT(*) FROM account_achievements a " +
        "WHERE a.provider = s.provider AND a.provider_game_id = s.provider_game_id) AS total, " +
        "(SELECT COUNT(*) FROM account_achievements a " +
        "WHERE a.provider = s.provider AND a.provider_game_id = s.provider_game_id AND a.is_earned = 1) AS unlocked, " +
        "(SELECT COALESCE(SUM(a.points), 0) FROM account_achievements a " +
        "WHERE a.provider = s.provider AND a.provider_game_id = s.provider_game_id) AS points, " +
        "(SELECT COALESCE(SUM(a.points), 0) FROM account_achievements a " +
        "WHERE a.provider = s.provider AND a.provider_game_id = s.provider_game_id AND a.is_earned = 1) AS earned_points"

@Dao
interface AccountAchievementSetDao {

    @Query(
        "SELECT * FROM account_achievement_sets " +
            "WHERE provider = :provider AND provider_game_id = :providerGameId LIMIT 1"
    )
    suspend fun getSet(provider: String, providerGameId: String): AccountAchievementSetEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: AccountAchievementSetEntity)

    @Query(
        "UPDATE account_achievement_sets SET icon_url = :iconUrl " +
            "WHERE provider = :provider AND provider_game_id = :providerGameId AND icon_url IS NULL"
    )
    suspend fun backfillIcon(provider: String, providerGameId: String, iconUrl: String)

    @Query("SELECT * FROM account_achievement_sets WHERE provider = :provider AND last_synced_at IS NULL")
    suspend fun getUnsyncedSets(provider: String): List<AccountAchievementSetEntity>

    @Query("SELECT * FROM account_achievement_sets")
    suspend fun getAllSets(): List<AccountAchievementSetEntity>

    @Query("DELETE FROM account_achievement_sets WHERE provider = :provider AND provider_game_id = :providerGameId")
    suspend fun deleteSet(provider: String, providerGameId: String)

    @Query(
        "SELECT " + SET_ROW_COLUMNS + ", l.game_id AS library_game_id " +
            "FROM provider_game_links l " +
            "JOIN account_achievement_sets s ON s.provider = l.provider AND s.provider_game_id = l.provider_game_id " +
            "JOIN games g ON g.id = l.game_id " +
            "WHERE l.game_id = :gameId AND s.last_synced_at IS NOT NULL " +
            "ORDER BY s.provider LIMIT 1"
    )
    fun observeSetForGame(gameId: Long): Flow<AchievementSetRow?>

    @Query(
        "SELECT " + SET_ROW_COLUMNS + ", MIN(l.game_id) AS library_game_id " +
            "FROM account_achievement_sets s " +
            "LEFT JOIN provider_game_links l ON l.provider = s.provider AND l.provider_game_id = s.provider_game_id " +
            "LEFT JOIN games g ON g.id = l.game_id " +
            "WHERE s.last_synced_at IS NOT NULL " +
            "GROUP BY s.provider, s.provider_game_id " +
            "ORDER BY last_played_at IS NULL, last_played_at DESC, title COLLATE NOCASE"
    )
    fun observeSets(): Flow<List<AchievementSetRow>>
}
