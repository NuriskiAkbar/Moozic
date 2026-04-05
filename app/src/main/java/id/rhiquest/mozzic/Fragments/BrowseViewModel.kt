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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.regex.Pattern
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

    fun extractYoutubeVideoId(url: String): String? {
        if (!url.contains("youtube.com") && !url.contains("youtu.be")) return null
        val regex = Pattern.compile("(?<=v=|v/|vi=|vi/|youtu\\.be/|/embed/)([a-zA-Z0-9_-]{11})")
        val matcher = regex.matcher(url)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }

    fun parseYoutubeUrl(url: String, videoId: String) {
        songResults.value = BaseResponse.Loading()
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
                    val responseStr = URL(oembedUrl).readText()
                    val json = JSONObject(responseStr)
                    
                    SongItem(
                        singer = json.optString("author_name", "Unknown Artist"),
                        title = json.optString("title", "YouTube Video"),
                        thumbanailUrl = json.optString("thumbnail_url", "https://img.youtube.com/vi/$videoId/hqdefault.jpg"),
                        videoId = videoId
                    )
                }
                songResults.value = BaseResponse.Success(listOf(result))
            } catch (e: Exception) {
                // Fallback to standard search if HTTP request or parsing fails
                songList(url)
            }
        }
    }
}