package id.rhiquest.mozzic.Fragments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import id.rhiquest.mozzic.Utils.DataUtils.SongItem
import id.rhiquest.mozzic.Utils.DataUtils.toSongItems
import id.rhiquest.mozzic.Utils.NetworkUtils.ApiClient
import id.rhiquest.mozzic.Utils.NetworkUtils.BaseResponse
import id.rhiquest.mozzic.Utils.NetworkUtils.SearchResponse
import kotlinx.coroutines.launch
import okhttp3.Dispatcher

class BrowseViewModel(application: Application): AndroidViewModel(application) {
    val api = ApiClient()
    val songResults : MutableLiveData<BaseResponse<List<SongItem>>> = MutableLiveData()

    fun songList(query: String){
        songResults.value = BaseResponse.Loading()
        viewModelScope.launch {
            try {
                val response = api.searchSong(query)
                if (response?.isSuccessful == true){
                    val result = response.body()?.items
                    if (!result.isNullOrEmpty()){
                        songResults.value = BaseResponse.Success(response.body()?.toSongItems())
                    } else {
                        songResults.value = BaseResponse.Error(response.message())
                    }
                } else {
                    songResults.value = BaseResponse.Error(response?.message())
                }
            } catch (e: Exception){
                songResults.value = BaseResponse.Error(e.message)
            }
        }
    }
}