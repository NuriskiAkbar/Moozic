package id.rhiquest.mozzic.Fragments

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import id.rhiquest.mozzic.Utils.DataUtils.Room.DatabaseHelperImpl
import id.rhiquest.mozzic.Utils.DataUtils.Room.FavoriteEntity
import id.rhiquest.mozzic.Utils.DataUtils.Room.LocalAppDatabase
import id.rhiquest.mozzic.Utils.DataUtils.SongItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class PlayViewModel(application: Application) : AndroidViewModel(application) {
    private val _currentMusic = MutableStateFlow<SongItem?>(null)
    val currentMusic: StateFlow<SongItem?> = _currentMusic

    private val _currentSecond = MutableStateFlow(0f)
    val currentSecond: StateFlow<Float> = _currentSecond

    private val _totalDuration = MutableStateFlow(0f)
    val totalDuration: StateFlow<Float> = _totalDuration

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    // One-time event for seek commands (Fragment → Activity → YouTubePlayer)
    private val _seekToEvent = MutableSharedFlow<Float>()
    val seekToEvent: SharedFlow<Float> = _seekToEvent

    private val dbHelper = DatabaseHelperImpl(LocalAppDatabase.getInstance(application))

    fun playMusic(song: SongItem) {
        _isPlaying.value = true
        _currentSecond.value = 0f
        _totalDuration.value = 0f
        _currentMusic.value = song
    }

    /** Switch to a new song while preserving the given play state */
    private fun switchMusic(song: SongItem, shouldPlay: Boolean) {
        _isPlaying.value = shouldPlay
        _currentSecond.value = 0f
        _totalDuration.value = 0f
        _currentMusic.value = song
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun stopMusic() {
        _isPlaying.value = false
        _currentSecond.value = 0f
        _totalDuration.value = 0f
        _currentMusic.value = null
    }

    fun updateSecond(second: Float) {
        _currentSecond.value = second
    }

    fun updateDuration(duration: Float) {
        _totalDuration.value = duration
    }

    fun seekTo(second: Float) {
        viewModelScope.launch {
            _seekToEvent.emit(second)
        }
    }

    fun playNextFromFavorites() {
        viewModelScope.launch {
            try {
                val favorites = dbHelper.getFavorites()
                if (favorites.isEmpty()) return@launch

                val wasPlaying = _isPlaying.value
                val currentVideoId = _currentMusic.value?.videoId
                val currentIndex = favorites.indexOfFirst { it.videoId == currentVideoId }

                val nextIndex = if (currentIndex < 0 || currentIndex >= favorites.size - 1) {
                    0
                } else {
                    currentIndex + 1
                }

                val nextFav = favorites[nextIndex]
                switchMusic(
                    SongItem(
                        singer = nextFav.artistFavorite,
                        title = nextFav.titleFavorite,
                        thumbanailUrl = nextFav.imageFavorite,
                        videoId = nextFav.videoId
                    ),
                    wasPlaying
                )
            } catch (e: Exception) {
                Log.e("PlayViewModel", "Error playing next: ${e.message}")
            }
        }
    }

    fun playPreviousFromFavorites() {
        viewModelScope.launch {
            try {
                val favorites = dbHelper.getFavorites()
                if (favorites.isEmpty()) return@launch

                val wasPlaying = _isPlaying.value
                val currentVideoId = _currentMusic.value?.videoId
                val currentIndex = favorites.indexOfFirst { it.videoId == currentVideoId }

                val prevIndex = if (currentIndex <= 0) {
                    favorites.size - 1
                } else {
                    currentIndex - 1
                }

                val prevFav = favorites[prevIndex]
                switchMusic(
                    SongItem(
                        singer = prevFav.artistFavorite,
                        title = prevFav.titleFavorite,
                        thumbanailUrl = prevFav.imageFavorite,
                        videoId = prevFav.videoId
                    ),
                    wasPlaying
                )
            } catch (e: Exception) {
                Log.e("PlayViewModel", "Error playing previous: ${e.message}")
            }
        }
    }

    fun addToFavorites(song: SongItem) {
        viewModelScope.launch {
            try {
                val entity = FavoriteEntity(
                    idFavorite = song.videoId,
                    titleFavorite = song.title,
                    artistFavorite = song.singer,
                    albumFavorite = "",
                    durationFavorite = "",
                    imageFavorite = song.thumbanailUrl,
                    videoId = song.videoId
                )
                dbHelper.insertFavorite(entity)
                Log.d("PlayViewModel", "Added to favorites: ${song.title}")
            } catch (e: Exception) {
                Log.e("PlayViewModel", "Error adding to favorites: ${e.message}")
            }
        }
    }
}