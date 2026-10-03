package com.psplauncher.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "account_achievement_sets",
    primaryKeys = ["provider", "provider_game_id"],
)
data class AccountAchievementSetEntity(
    val provider: String,

    @ColumnInfo(name = "provider_game_id")
    val providerGameId: String,

    val title: String,

    @ColumnInfo(name = "icon_url")
    val iconUrl: String? = null,

    @ColumnInfo(name = "bronze_total")
    val bronzeTotal: Int = 0,
    @ColumnInfo(name = "silver_total")
    val silverTotal: Int = 0,
    @ColumnInfo(name = "gold_total")
    val goldTotal: Int = 0,

    @ColumnInfo(name = "bronze_earned")
    val bronzeEarned: Int = 0,
    @ColumnInfo(name = "silver_earned")
    val silverEarned: Int = 0,
    @ColumnInfo(name = "gold_earned")
    val goldEarned: Int = 0,

    val mastered: Boolean = false,

    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long? = null,
)
