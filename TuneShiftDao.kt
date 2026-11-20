package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TuneShiftDao {

    @Query("SELECT * FROM saved_playlists ORDER BY updatedAt DESC")
    fun observeSavedPlaylists(): Flow<List<SavedPlaylistEntity>>

    @Query("SELECT * FROM migration_history ORDER BY executedAt DESC")
    fun observeMigrationHistory(): Flow<List<MigrationHistoryEntity>>

    @Query("SELECT * FROM track_items WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getTracksForPlaylist(playlistId: String): List<TrackItemEntity>

    @Query("SELECT * FROM saved_playlists WHERE id = :playlistId LIMIT 1")
    suspend fun getPlaylistById(playlistId: String): SavedPlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylist(playlist: SavedPlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackItemEntity>)

    @Query("DELETE FROM track_items WHERE playlistId = :playlistId")
    suspend fun deleteTracksForPlaylist(playlistId: String)

    @Transaction
    suspend fun savePlaylistWithTracks(
        playlist: SavedPlaylistEntity,
        tracks: List<TrackItemEntity>
    ) {
        upsertPlaylist(playlist)
        deleteTracksForPlaylist(playlist.id)
        if (tracks.isNotEmpty()) {
            insertTracks(tracks)
        }
    }

    @Query("DELETE FROM saved_playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMigrationHistory(history: MigrationHistoryEntity)

    @Query("DELETE FROM migration_history WHERE id = :id")
    suspend fun deleteHistoryById(id: String)

    @Transaction
    suspend fun clearAllData() {
        deleteAllTracks()
        deleteAllPlaylists()
        deleteAllHistory()
    }

    @Query("DELETE FROM track_items")
    suspend fun deleteAllTracks()

    @Query("DELETE FROM saved_playlists")
    suspend fun deleteAllPlaylists()

    @Query("DELETE FROM migration_history")
    suspend fun deleteAllHistory()
}
