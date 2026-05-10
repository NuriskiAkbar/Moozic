package id.rhiquest.mozzic.Utils.DataUtils.Room

interface LocalAudioDataSource {
    suspend fun getLocalSong(): List<LocalSongEntity>
}