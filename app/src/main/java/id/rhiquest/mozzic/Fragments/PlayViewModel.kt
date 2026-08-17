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
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import id.rhiquest.mozzic.Utils.NetworkUtils.LrcLibService
import id.rhiquest.mozzic.Utils.NetworkUtils.LrcResponse

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
    private val lrcService = LrcLibService()

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    private val _lyricsState = MutableStateFlow<LyricsState>(LyricsState.Empty)
    val lyricsState: StateFlow<LyricsState> = _lyricsState.asStateFlow()

    private val _currentQueue = MutableStateFlow<List<SongItem>>(emptyList())
    val currentQueue: StateFlow<List<SongItem>> = _currentQueue.asStateFlow()

    fun playMusic(song: SongItem, queue: List<SongItem> = emptyList()) {
        _isPlaying.value = true
        _currentSecond.value = 0f
        _totalDuration.value = 0f
        _currentMusic.value = song
        if (queue.isNotEmpty()) {
            _currentQueue.value = queue
        } else {
            _currentQueue.value = listOf(song)
        }
        loadLyricsForSong(song)
    }

    /** Switch to a new song while preserving the given play state */
    private fun switchMusic(song: SongItem, shouldPlay: Boolean) {
        _isPlaying.value = shouldPlay
        _currentSecond.value = 0f
        _totalDuration.value = 0f
        _currentMusic.value = song
        loadLyricsForSong(song)
    }

    private fun loadLyricsForSong(song: SongItem) {
        viewModelScope.launch {
            _lyricsState.value = LyricsState.Loading
            _lyrics.value = emptyList()
            try {
                val response = lrcService.fetchLyrics(song.singer, song.title)
                if (response != null) {
                    if (response.instrumental == true) {
                        _lyricsState.value = LyricsState.Instrumental
                    } else if (!response.syncedLyrics.isNullOrBlank()) {
                        val parsed = parseLrc(response.syncedLyrics)
                        if (parsed.isNotEmpty()) {
                            _lyrics.value = parsed
                            _lyricsState.value = LyricsState.HasLyrics
                        } else if (!response.plainLyrics.isNullOrBlank()) {
                            val plainLines = response.plainLyrics.split("\n").map { LyricLine(0L, it) }
                            _lyrics.value = plainLines
                            _lyricsState.value = LyricsState.PlainLyrics
                        } else {
                            _lyricsState.value = LyricsState.NoLyrics
                        }
                    } else if (!response.plainLyrics.isNullOrBlank()) {
                        val plainLines = response.plainLyrics.split("\n").map { LyricLine(0L, it) }
                        _lyrics.value = plainLines
                        _lyricsState.value = LyricsState.PlainLyrics
                    } else {
                        _lyricsState.value = LyricsState.NoLyrics
                    }
                } else {
                    _lyricsState.value = LyricsState.NoLyrics
                }
            } catch (e: Exception) {
                _lyricsState.value = LyricsState.NoLyrics
            }
        }
    }

    private fun parseLrc(lrcText: String): List<LyricLine> {
        val lyricList = mutableListOf<LyricLine>()
        val lines = lrcText.split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (!trimmed.startsWith("[")) continue
            val closeBracketIndex = trimmed.indexOf("]")
            if (closeBracketIndex == -1) continue
            val timeString = trimmed.substring(1, closeBracketIndex)
            val lyricText = trimmed.substring(closeBracketIndex + 1).trim()
            
            val timeMs = parseTimeStringToMs(timeString)
            if (timeMs != null) {
                lyricList.add(LyricLine(timeMs, lyricText))
            }
        }
        return lyricList.sortedBy { it.timeMs }
    }

    private fun parseTimeStringToMs(timeString: String): Long? {
        val parts = timeString.split(":")
        if (parts.size < 2) return null
        try {
            val min = parts[0].toLong()
            val rest = parts[1]
            val dotIndex = rest.indexOf(".")
            val sec: Long
            val ms: Long
            if (dotIndex != -1) {
                sec = rest.substring(0, dotIndex).toLong()
                val msStr = rest.substring(dotIndex + 1)
                val parsedMs = msStr.toLong()
                ms = if (msStr.length == 2) {
                    parsedMs * 10
                } else if (msStr.length == 1) {
                    parsedMs * 100
                } else {
                    parsedMs
                }
            } else {
                sec = rest.toLong()
                ms = 0L
            }
            return (min * 60 + sec) * 1000 + ms
        } catch (e: Exception) {
            return null
        }
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

    fun playNext() {
        val queue = _currentQueue.value
        if (queue.isEmpty()) {
            playNextFromFavorites()
            return
        }
        val current = _currentMusic.value
        if (current == null) {
            playNextFromFavorites()
            return
        }
        val currentIndex = queue.indexOfFirst { it.videoId == current.videoId }
        if (currentIndex == -1) {
            playNextFromFavorites()
            return
        }
        val nextIndex = (currentIndex + 1) % queue.size
        val nextSong = queue[nextIndex]
        val wasPlaying = _isPlaying.value
        switchMusic(nextSong, wasPlaying)
    }

    fun playPrevious() {
        val queue = _currentQueue.value
        if (queue.isEmpty()) {
            playPreviousFromFavorites()
            return
        }
        val current = _currentMusic.value
        if (current == null) {
            playPreviousFromFavorites()
            return
        }
        val currentIndex = queue.indexOfFirst { it.videoId == current.videoId }
        if (currentIndex == -1) {
            playPreviousFromFavorites()
            return
        }
        val prevIndex = if (currentIndex <= 0) queue.size - 1 else currentIndex - 1
        val prevSong = queue[prevIndex]
        val wasPlaying = _isPlaying.value
        switchMusic(prevSong, wasPlaying)
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

data class LyricLine(
    val timeMs: Long,
    val text: String
)

sealed interface LyricsState {
    object Empty : LyricsState
    object Loading : LyricsState
    object Instrumental : LyricsState
    object NoLyrics : LyricsState
    object PlainLyrics : LyricsState
    object HasLyrics : LyricsState
}