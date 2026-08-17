package id.rhiquest.mozzic.Fragments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.rhiquest.mozzic.Utils.DataUtils.Room.DatabaseHelperImpl
import id.rhiquest.mozzic.Utils.DataUtils.Room.LocalAppDatabase
import id.rhiquest.mozzic.Utils.DataUtils.Room.PlaylistEntity
import id.rhiquest.mozzic.Utils.DataUtils.Room.PlaylistSongEntity
import id.rhiquest.mozzic.Utils.DataUtils.Room.PlaylistWithSongCount
import id.rhiquest.mozzic.Utils.DataUtils.SongItem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = DatabaseHelperImpl(LocalAppDatabase.getInstance(application))

    // Aliran State untuk menyimpan list playlist beserta jumlah lagu didalamnya
    private val _playlists = MutableStateFlow<List<PlaylistWithSongCount>>(emptyList())
    val playlists: StateFlow<List<PlaylistWithSongCount>> = _playlists.asStateFlow()

    // Aliran State untuk menyimpan daftar lagu di dalam playlist yang sedang dibuka
    private val _playlistSongs = MutableStateFlow<List<PlaylistSongEntity>>(emptyList())
    val playlistSongs: StateFlow<List<PlaylistSongEntity>> = _playlistSongs.asStateFlow()

    // Satu-satunya emitter: Channel untuk mengirim hasil/proses pembuatan playlist ke UI
    private val _createPlaylistResult = Channel<CreatePlaylistResult>()
    val createPlaylistResult = _createPlaylistResult.receiveAsFlow()

    init {
        fetchPlaylists()
    }

    fun fetchPlaylists() {
        viewModelScope.launch {
            try {
                val list = dbHelper.getPlaylistsWithSongCount()
                _playlists.value = list
            } catch (e: Exception) {
                // Penanganan error jika diperlukan
            }
        }
    }

    fun createPlaylist(playlistName: String) {
        if (playlistName.isBlank()) {
            viewModelScope.launch {
                _createPlaylistResult.send(CreatePlaylistResult.Error("Nama playlist tidak boleh kosong"))
            }
            return
        }

        viewModelScope.launch {
            _createPlaylistResult.send(CreatePlaylistResult.Loading)
            try {
                // Simpan ke database local
                dbHelper.insertPlaylist(PlaylistEntity(name = playlistName))
                
                _createPlaylistResult.send(CreatePlaylistResult.Success("Playlist '$playlistName' sukses dibuat"))
                fetchPlaylists() // Perbarui list playlist secara real-time
            } catch (e: Exception) {
                _createPlaylistResult.send(CreatePlaylistResult.Error(e.message ?: "Terjadi kesalahan"))
            }
        }
    }

    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch {
            try {
                dbHelper.deletePlaylist(playlistId)
                fetchPlaylists()
            } catch (e: Exception) {
                // Penanganan error
            }
        }
    }

    fun fetchSongsForPlaylist(playlistId: Int) {
        viewModelScope.launch {
            try {
                val list = dbHelper.getSongsForPlaylist(playlistId)
                _playlistSongs.value = list
            } catch (e: Exception) {
                // Penanganan error
            }
        }
    }

    fun deleteSongFromPlaylist(id: Int, playlistId: Int) {
        viewModelScope.launch {
            try {
                dbHelper.deleteSongFromPlaylist(id)
                fetchSongsForPlaylist(playlistId) // Refresh list lagu di detail
                fetchPlaylists() // Refresh jumlah lagu di daftar playlist
            } catch (e: Exception) {
                // Penanganan error
            }
        }
    }

    fun addSongToPlaylist(playlistId: Int, song: SongItem) {
        viewModelScope.launch {
            try {
                dbHelper.insertSongToPlaylist(
                    PlaylistSongEntity(
                        playlistId = playlistId,
                        songId = song.videoId,
                        title = song.title,
                        artist = song.singer,
                        image = song.thumbanailUrl
                    )
                )
                fetchPlaylists()
                fetchSongsForPlaylist(playlistId)
            } catch (e: Exception) {
                // Penanganan error
            }
        }
    }

    fun createPlaylistAndAddSong(playlistName: String, song: SongItem) {
        viewModelScope.launch {
            if (playlistName.isBlank()) {
                _createPlaylistResult.send(CreatePlaylistResult.Error("Nama playlist tidak boleh kosong"))
                return@launch
            }
            _createPlaylistResult.send(CreatePlaylistResult.Loading)
            try {
                val newPlaylistId = dbHelper.insertPlaylist(PlaylistEntity(name = playlistName))
                dbHelper.insertSongToPlaylist(
                    PlaylistSongEntity(
                        playlistId = newPlaylistId.toInt(),
                        songId = song.videoId,
                        title = song.title,
                        artist = song.singer,
                        image = song.thumbanailUrl
                    )
                )
                _createPlaylistResult.send(CreatePlaylistResult.Success("Playlist '$playlistName' dibuat dan lagu ditambahkan"))
                fetchPlaylists()
            } catch (e: Exception) {
                _createPlaylistResult.send(CreatePlaylistResult.Error(e.message ?: "Terjadi kesalahan"))
            }
        }
    }
}

// Sealed interface untuk menampung semua state & event dalam satu wadah
sealed interface CreatePlaylistResult {
    object Loading : CreatePlaylistResult
    data class Success(val message: String) : CreatePlaylistResult
    data class Error(val errorMessage: String) : CreatePlaylistResult
}