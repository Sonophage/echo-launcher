package com.psplauncher.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "provider_game_links",
    primaryKeys = ["game_id", "provider"],
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["game_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ProviderGameLinkEntity(
    @ColumnInfo(name = "game_id")
    val gameId: Long,

    val provider: String,

    @ColumnInfo(name = "provider_game_id")
    val providerGameId: String,

    val source: String,

    @ColumnInfo(name = "resolved_at")
    val resolvedAt: Long,

    val ownership: String? = null,
)
