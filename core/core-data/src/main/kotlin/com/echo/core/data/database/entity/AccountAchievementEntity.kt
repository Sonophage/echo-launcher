package com.echo.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "account_achievements",
    primaryKeys = ["provider", "provider_game_id", "provider_achievement_id"],
)
data class AccountAchievementEntity(
    val provider: String,

    @ColumnInfo(name = "provider_game_id")
    val providerGameId: String,

    @ColumnInfo(name = "provider_achievement_id")
    val providerAchievementId: String,

    val title: String,
    val description: String,

    val tier: String,

    @ColumnInfo(name = "global_rarity")
    val globalRarity: Double,

    @ColumnInfo(name = "icon_url")
    val iconUrl: String? = null,

    @ColumnInfo(name = "is_hidden")
    val isHidden: Boolean = false,

    @ColumnInfo(name = "is_earned")
    val isEarned: Boolean = false,

    @ColumnInfo(name = "earned_at")
    val earnedAt: Long? = null,

    val points: Int? = null,
)
