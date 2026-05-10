package id.rhiquest.mozzic.Utils.DataUtils.Room

interface DatabaseHelper {
    suspend fun getFavorites(): List<FavoriteEntity>
    suspend fun insertFavorite(favoriteEntity: FavoriteEntity)
    suspend fun deleteFavorite(idFavorite: String)

    suspend fun getLocalSongs(): List<LocalSongEntity>
    suspend fun insertLocalSong(song: LocalSongEntity)
    suspend fun deleteLocalSong(uri: String)
}