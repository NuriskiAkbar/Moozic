package id.rhiquest.mozzic.Utils.DataUtils.Room

interface DatabaseHelper {
    suspend fun getFavorites(): List<FavoriteEntity>
    suspend fun insertFavorite(favoriteEntity: FavoriteEntity)
    suspend fun deleteFavorite(idFavorite: String)

    suspend fun getLocalSongs(): List<LocalSongEntity>
    suspend fun insertLocalSong(song: LocalSongEntity)
    suspend fun deleteLocalSong(uri: String)

    suspend fun getAllPlaylists(): List<PlaylistEntity>
    suspend fun getPlaylistsWithSongCount(): List<PlaylistWithSongCount>
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long
    suspend fun deletePlaylist(id: Int)

    suspend fun getSongsForPlaylist(playlistId: Int): List<PlaylistSongEntity>
    suspend fun deleteSongFromPlaylist(id: Int)
    suspend fun insertSongToPlaylist(playlistSong: PlaylistSongEntity)
}