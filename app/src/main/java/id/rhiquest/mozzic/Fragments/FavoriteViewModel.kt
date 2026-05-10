package id.rhiquest.mozzic.Fragments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import id.rhiquest.mozzic.Utils.DataUtils.Room.DatabaseHelperImpl
import id.rhiquest.mozzic.Utils.DataUtils.Room.FavoriteEntity
import id.rhiquest.mozzic.Utils.DataUtils.Room.LocalAppDatabase
import id.rhiquest.mozzic.Utils.DataUtils.SongItem
import id.rhiquest.mozzic.Utils.NetworkUtils.BaseResponse
import id.rhiquest.mozzic.Utils.DataUtils.Room.LocalSongEntity
import kotlinx.coroutines.launch

class FavoriteViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = DatabaseHelperImpl(LocalAppDatabase.getInstance(application))

    val favorites: MutableLiveData<BaseResponse<List<SongItem>>> = MutableLiveData()
    val localSongs: MutableLiveData<BaseResponse<List<SongItem>>> = MutableLiveData()

    fun loadFavorites() {
        favorites.value = BaseResponse.Loading()
        viewModelScope.launch {
            try {
                val entities = dbHelper.getFavorites()
                val songItems = entities.map { it.toSongItem() }
                favorites.value = BaseResponse.Success(songItems)
            } catch (e: Exception) {
                favorites.value = BaseResponse.Error(e.message)
            }
        }
    }

    fun deleteFavorite(songItem: SongItem) {
        viewModelScope.launch {
            try {
                dbHelper.deleteFavorite(songItem.videoId)
                loadFavorites()
            } catch (e: Exception) {
                favorites.value = BaseResponse.Error(e.message)
            }
        }
    }

    fun loadLocalSongs() {
        localSongs.value = BaseResponse.Loading()
        viewModelScope.launch {
            try {
                val entities = dbHelper.getLocalSongs()
                val songItems = entities.map { 
                    SongItem(
                        singer = it.artistLocalSong,
                        title = it.titleLocalSong,
                        thumbanailUrl = "",
                        videoId = it.uriLocalSong
                    )
                }
                localSongs.value = BaseResponse.Success(songItems)
            } catch (e: Exception) {
                localSongs.value = BaseResponse.Error(e.message)
            }
        }
    }

    fun addLocalSong(uri: String, title: String, artist: String) {
        viewModelScope.launch {
            try {
                val song = LocalSongEntity(
                    idLocalSong = System.currentTimeMillis() + uri.hashCode(), // Generate somewhat unique ID
                    titleLocalSong = title,
                    artistLocalSong = artist,
                    uriLocalSong = uri
                )
                dbHelper.insertLocalSong(song)
                loadLocalSongs()
            } catch (e: Exception) {
                localSongs.value = BaseResponse.Error(e.message)
            }
        }
    }


    private fun FavoriteEntity.toSongItem(): SongItem {
        return SongItem(
            singer = artistFavorite,
            title = titleFavorite,
            thumbanailUrl = imageFavorite,
            videoId = videoId
        )
    }
}
