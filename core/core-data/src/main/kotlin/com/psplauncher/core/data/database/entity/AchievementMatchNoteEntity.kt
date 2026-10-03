package com.psplauncher.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "achievement_match_notes",
    primaryKeys = ["game_id"],
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["game_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class AchievementMatchNoteEntity(
    @ColumnInfo(name = "game_id")
    val gameId: Long,

    val reason: String,

    @ColumnInfo(name = "checked_at")
    val checkedAt: Long,
)
