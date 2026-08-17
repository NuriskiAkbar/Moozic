package id.rhiquest.mozzic.Utils.DataUtils.Room

class DatabaseHelperImpl (
    private val db: LocalAppDatabase
): DatabaseHelper {
    override suspend fun getFavorites(): List<FavoriteEntity> = db.favoriteDao().getAllFavorite()

    override suspend fun insertFavorite(favoriteEntity: FavoriteEntity) =
        db.favoriteDao().insertFavoriteItem(favoriteEntity)

    override suspend fun deleteFavorite(idFavorite: String) = db.favoriteDao().deleteFavoriteItem(idFavorite)

    override suspend fun getLocalSongs(): List<LocalSongEntity> = db.localSongDao().getAllLocalSongs()

    override suspend fun insertLocalSong(song: LocalSongEntity) = db.localSongDao().insertLocalSong(song)

    override suspend fun deleteLocalSong(uri: String) = db.localSongDao().deleteLocalSong(uri)

    override suspend fun getAllPlaylists(): List<PlaylistEntity> = db.playlistDao().getAllPlaylists()

    override suspend fun getPlaylistsWithSongCount(): List<PlaylistWithSongCount> =
        db.playlistDao().getPlaylistsWithSongCount()

    override suspend fun insertPlaylist(playlist: PlaylistEntity): Long = db.playlistDao().insertPlaylist(playlist)

    override suspend fun deletePlaylist(id: Int) = db.playlistDao().deletePlaylist(id)

    override suspend fun getSongsForPlaylist(playlistId: Int): List<PlaylistSongEntity> =
        db.playlistDao().getSongsForPlaylist(playlistId)

    override suspend fun deleteSongFromPlaylist(id: Int) = db.playlistDao().deleteSongFromPlaylist(id)

    override suspend fun insertSongToPlaylist(playlistSong: PlaylistSongEntity) =
        db.playlistDao().insertSongToPlaylist(playlistSong)
}