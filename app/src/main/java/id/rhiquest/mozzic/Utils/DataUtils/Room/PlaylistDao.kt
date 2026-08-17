package id.rhiquest.mozzic.Utils.DataUtils.Room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlist_table")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Query("""
        SELECT p.id, p.name, COUNT(ps.id) as songCount 
        FROM playlist_table p 
        LEFT JOIN playlist_song_table ps ON p.id = ps.playlistId 
        GROUP BY p.id
    """)
    suspend fun getPlaylistsWithSongCount(): List<PlaylistWithSongCount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlist_table WHERE id = :id")
    suspend fun deletePlaylist(id: Int)

    @Query("SELECT * FROM playlist_song_table WHERE playlistId = :playlistId")
    suspend fun getSongsForPlaylist(playlistId: Int): List<PlaylistSongEntity>

    @Query("DELETE FROM playlist_song_table WHERE id = :id")
    suspend fun deleteSongFromPlaylist(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongToPlaylist(playlistSong: PlaylistSongEntity)
}
