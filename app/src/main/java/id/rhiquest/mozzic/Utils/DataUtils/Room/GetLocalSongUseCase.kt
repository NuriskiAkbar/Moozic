package id.rhiquest.mozzic.Utils.DataUtils.Room

class GetLocalSongUseCase(
    private val repository: LocalSongRepository
) {
    suspend operator fun invoke(): List<Song> {
        return repository.getLocalSongs()
    }
}