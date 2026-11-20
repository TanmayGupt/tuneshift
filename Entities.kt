package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "saved_playlists")
data class SavedPlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sourcePlatformId: String,
    val destinationPlatformId: String,
    val trackCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "track_items",
    foreignKeys = [
        ForeignKey(
            entity = SavedPlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["playlistId"])]
)
data class TrackItemEntity(
    @PrimaryKey val id: String,
    val playlistId: String,
    val position: Int,
    val rawTitle: String,
    val rawArtist: String,
    val album: String,
    val durationMs: Long?,
    val isrc: String,
    val externalId: String,
    val notes: String,
    val isSelected: Boolean,
    val isDuplicate: Boolean,
    val duplicateOfPosition: Int?,
    val matchedTitle: String?,
    val matchedArtist: String?,
    val matchedAlbum: String?,
    val matchedDurationMs: Long?,
    val matchStatus: String?,
    val confidence: Int?,
    val matchReason: String?,
    val autoAccepted: Boolean,
    val userCorrected: Boolean
)

@Entity(tableName = "migration_history")
data class MigrationHistoryEntity(
    @PrimaryKey val id: String,
    val playlistName: String,
    val sourcePlatformId: String,
    val destinationPlatformId: String,
    val totalTracks: Int,
    val matchedCount: Int,
    val ambiguousCount: Int,
    val unmatchedCount: Int,
    val skippedCount: Int,
    val exportedCount: Int,
    val exportFormat: String,
    val exportedFileName: String,
    val executedAt: Long
)
