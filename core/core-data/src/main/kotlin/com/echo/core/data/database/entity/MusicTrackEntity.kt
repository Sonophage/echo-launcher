package com.echo.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import com.echo.core.domain.model.MusicTrack

@Serializable
@Entity(
    tableName = "music_tracks",
    foreignKeys = [
        ForeignKey(
            entity = MusicFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folder_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],

    indices = [Index("folder_id"), Index("last_played_at")],
)
data class MusicTrackEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "folder_id")
    val folderId: String,

    val uri: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,

    @ColumnInfo(name = "album_artist")
    val albumArtist: String? = null,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long? = null,

    @ColumnInfo(name = "mime_type")
    val mimeType: String? = null,

    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long? = null,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long? = null,

    @ColumnInfo(name = "track_number")
    val trackNumber: Int? = null,

    @ColumnInfo(name = "relative_path")
    val relativePath: String? = null,

    @ColumnInfo(name = "art_uri")
    val artUri: String? = null,

    @ColumnInfo(name = "last_played_at")
    val lastPlayedAt: Long? = null,

    // the file's genre tag; "" once read and empty, null until the scanner has read it (owner, 2026-10-08)
    val genre: String? = null,

    // a genre the owner set, kept over the tag and over rescans
    @ColumnInfo(name = "genre_override")
    val genreOverride: String? = null,
)

fun MusicTrackEntity.toDomain() = MusicTrack(
    id           = id,
    folderId     = folderId,
    uri          = uri,
    displayName  = displayName,
    title        = title,
    artist       = artist,
    album        = album,
    albumArtist  = albumArtist,
    durationMs   = durationMs,
    mimeType     = mimeType,
    sizeBytes    = sizeBytes,
    lastModified = lastModified,
    trackNumber  = trackNumber,
    relativePath = relativePath,
    artUri       = artUri,
    lastPlayedAt = lastPlayedAt,
    genre        = genre,
    genreOverride = genreOverride,
)

fun MusicTrack.toEntity() = MusicTrackEntity(
    id           = id,
    folderId     = folderId,
    uri          = uri,
    displayName  = displayName,
    title        = title,
    artist       = artist,
    album        = album,
    albumArtist  = albumArtist,
    durationMs   = durationMs,
    mimeType     = mimeType,
    sizeBytes    = sizeBytes,
    lastModified = lastModified,
    trackNumber  = trackNumber,
    relativePath = relativePath,
    artUri       = artUri,
    lastPlayedAt = lastPlayedAt,
    genre        = genre,
    genreOverride = genreOverride,
)
