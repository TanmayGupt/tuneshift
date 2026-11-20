package com.example.data

import com.example.domain.matching.TextNormalizer
import com.example.domain.model.MatchStatus
import com.example.domain.model.MusicPlatform
import com.example.domain.model.SourceTrack
import com.example.domain.model.TrackMatchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

data class SavedPlaylistSummary(
    val id: String,
    val name: String,
    val sourcePlatform: MusicPlatform,
    val destinationPlatform: MusicPlatform,
    val trackCount: Int,
    val updatedAt: Long
)

data class MigrationRecord(
    val id: String,
    val playlistName: String,
    val sourcePlatform: MusicPlatform,
    val destinationPlatform: MusicPlatform,
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

data class LoadedPlaylistBundle(
    val id: String,
    val name: String,
    val sourcePlatform: MusicPlatform,
    val destinationPlatform: MusicPlatform,
    val tracks: List<SourceTrack>,
    val matchResults: List<TrackMatchResult>
)

class TuneShiftRepository(private val dao: TuneShiftDao) {

    val savedPlaylists: Flow<List<SavedPlaylistSummary>> =
        dao.observeSavedPlaylists().map { list ->
            list.map {
                SavedPlaylistSummary(
                    id = it.id,
                    name = it.name,
                    sourcePlatform = MusicPlatform.fromId(it.sourcePlatformId),
                    destinationPlatform = MusicPlatform.fromId(it.destinationPlatformId),
                    trackCount = it.trackCount,
                    updatedAt = it.updatedAt
                )
            }
        }

    val migrationHistory: Flow<List<MigrationRecord>> =
        dao.observeMigrationHistory().map { list ->
            list.map {
                MigrationRecord(
                    id = it.id,
                    playlistName = it.playlistName,
                    sourcePlatform = MusicPlatform.fromId(it.sourcePlatformId),
                    destinationPlatform = MusicPlatform.fromId(it.destinationPlatformId),
                    totalTracks = it.totalTracks,
                    matchedCount = it.matchedCount,
                    ambiguousCount = it.ambiguousCount,
                    unmatchedCount = it.unmatchedCount,
                    skippedCount = it.skippedCount,
                    exportedCount = it.exportedCount,
                    exportFormat = it.exportFormat,
                    exportedFileName = it.exportedFileName,
                    executedAt = it.executedAt
                )
            }
        }

    suspend fun savePlaylistSession(
        playlistId: String,
        name: String,
        sourcePlatform: MusicPlatform,
        destinationPlatform: MusicPlatform,
        tracks: List<SourceTrack>,
        matchResults: List<TrackMatchResult>
    ) {
        val now = System.currentTimeMillis()
        val existing = dao.getPlaylistById(playlistId)
        val matchById = matchResults.associateBy { it.trackId }

        val playlistEntity = SavedPlaylistEntity(
            id = playlistId,
            name = name.ifBlank { "Untitled Playlist" },
            sourcePlatformId = sourcePlatform.id,
            destinationPlatformId = destinationPlatform.id,
            trackCount = tracks.size,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )

        val trackEntities = tracks.mapIndexed { idx, track ->
            val match = matchById[track.id]
            TrackItemEntity(
                id = "${playlistId}_${track.id}",
                playlistId = playlistId,
                position = idx + 1,
                rawTitle = track.rawTitle,
                rawArtist = track.rawArtist,
                album = track.album,
                durationMs = track.durationMs,
                isrc = track.isrc,
                externalId = track.externalId,
                notes = track.notes,
                isSelected = track.isSelected,
                isDuplicate = track.isDuplicate,
                duplicateOfPosition = track.duplicateOfPosition,
                matchedTitle = match?.matchedTitle,
                matchedArtist = match?.matchedArtist,
                matchedAlbum = match?.matchedAlbum,
                matchedDurationMs = match?.matchedDurationMs,
                matchStatus = match?.status?.name,
                confidence = match?.confidence,
                matchReason = match?.reason,
                autoAccepted = match?.autoAccepted ?: false,
                userCorrected = match?.userCorrected ?: false
            )
        }

        dao.savePlaylistWithTracks(playlistEntity, trackEntities)
    }

    suspend fun loadPlaylistSession(playlistId: String): LoadedPlaylistBundle? {
        val playlist = dao.getPlaylistById(playlistId) ?: return null
        val rows = dao.getTracksForPlaylist(playlistId)
        val tracks = rows.map { row ->
            val cleanTrackId = row.id.removePrefix("${playlistId}_")
            SourceTrack(
                id = cleanTrackId,
                position = row.position,
                rawTitle = row.rawTitle,
                rawArtist = row.rawArtist,
                album = row.album,
                durationMs = row.durationMs,
                isrc = row.isrc,
                externalId = row.externalId,
                notes = row.notes,
                isSelected = row.isSelected,
                isDuplicate = row.isDuplicate,
                duplicateOfPosition = row.duplicateOfPosition
            )
        }
        val matches = rows.mapIndexedNotNull { idx, row ->
            val status = MatchStatus.fromStored(row.matchStatus) ?: return@mapIndexedNotNull null
            val track = tracks[idx]
            val titleInfo = TextNormalizer.analyzeTitle(track.rawTitle)
            TrackMatchResult(
                trackId = track.id,
                sourceTrack = track,
                normalizedTitle = titleInfo.cleanTitle,
                normalizedArtist = TextNormalizer.normalizeArtist(track.rawArtist, titleInfo.extractedFeaturedArtists),
                matchedTitle = row.matchedTitle.orEmpty(),
                matchedArtist = row.matchedArtist.orEmpty(),
                matchedAlbum = row.matchedAlbum.orEmpty(),
                matchedDurationMs = row.matchedDurationMs,
                status = status,
                confidence = row.confidence ?: 0,
                reason = row.matchReason.orEmpty(),
                autoAccepted = row.autoAccepted,
                candidates = emptyList(),
                isDuplicate = track.isDuplicate,
                userCorrected = row.userCorrected
            )
        }

        return LoadedPlaylistBundle(
            id = playlist.id,
            name = playlist.name,
            sourcePlatform = MusicPlatform.fromId(playlist.sourcePlatformId),
            destinationPlatform = MusicPlatform.fromId(playlist.destinationPlatformId),
            tracks = tracks,
            matchResults = matches
        )
    }

    suspend fun deletePlaylist(playlistId: String) {
        dao.deletePlaylistById(playlistId)
    }

    suspend fun recordMigrationHistory(
        playlistName: String,
        sourcePlatform: MusicPlatform,
        destinationPlatform: MusicPlatform,
        totalTracks: Int,
        matchedCount: Int,
        ambiguousCount: Int,
        unmatchedCount: Int,
        skippedCount: Int,
        exportedCount: Int,
        exportFormat: String,
        exportedFileName: String
    ) {
        dao.insertMigrationHistory(
            MigrationHistoryEntity(
                id = UUID.randomUUID().toString(),
                playlistName = playlistName,
                sourcePlatformId = sourcePlatform.id,
                destinationPlatformId = destinationPlatform.id,
                totalTracks = totalTracks,
                matchedCount = matchedCount,
                ambiguousCount = ambiguousCount,
                unmatchedCount = unmatchedCount,
                skippedCount = skippedCount,
                exportedCount = exportedCount,
                exportFormat = exportFormat,
                exportedFileName = exportedFileName,
                executedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearAllLocalData() {
        dao.clearAllData()
    }
}
