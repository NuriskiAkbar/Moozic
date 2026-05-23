package id.rhiquest.mozzic.Utils.DataUtils.Room

interface LocalSongRepository {
    suspend fun getLocalSongs(): List<Song>
}