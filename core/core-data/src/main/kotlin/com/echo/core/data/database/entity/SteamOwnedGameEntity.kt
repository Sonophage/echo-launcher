package com.echo.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "steam_owned_games")
data class SteamOwnedGameEntity(
    @PrimaryKey
    val appid: String,

    val name: String,

    @ColumnInfo(name = "playtime_forever_minutes")
    val playtimeForeverMinutes: Long,

    @ColumnInfo(name = "synced_playtime_minutes")
    val syncedPlaytimeMinutes: Long? = null,

    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Long,
)

@Entity(tableName = "steam_no_achievements")
data class SteamNoAchievementsEntity(
    @PrimaryKey
    val appid: String,

    @ColumnInfo(name = "checked_at")
    val checkedAt: Long,
)
