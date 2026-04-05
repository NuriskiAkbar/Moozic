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
import kotlinx.coroutines.launch

class FavoriteViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = DatabaseHelperImpl(LocalAppDatabase.getInstance(application))

    val favorites: MutableLiveData<BaseResponse<List<SongItem>>> = MutableLiveData()

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

    private fun FavoriteEntity.toSongItem(): SongItem {
        return SongItem(
            singer = artistFavorite,
            title = titleFavorite,
            thumbanailUrl = imageFavorite,
            videoId = videoId
        )
    }
}
