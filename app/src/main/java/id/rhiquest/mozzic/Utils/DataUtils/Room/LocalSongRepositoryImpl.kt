package id.rhiquest.mozzic.Utils.DataUtils.Room

class LocalSongRepositoryImpl(
    private val localAudioDataSource: LocalAudioDataSource
): LocalSongRepository {
    override suspend fun getLocalSongs(): List<Song> {
        return localAudioDataSource.getLocalSong().map {
            Song(
                id = it.idLocalSong,
                title = it.titleLocalSong,
                artist = it.artistLocalSong,
                uri = it.uriLocalSong
            )
        }
    }
}