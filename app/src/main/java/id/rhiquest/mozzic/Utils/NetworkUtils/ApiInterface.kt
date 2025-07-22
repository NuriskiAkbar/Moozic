package id.rhiquest.mozzic.Utils.NetworkUtils

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {
    @GET("search")
    suspend fun searchSong(
        @Query("part") part: String = "snippet",
        @Query("type") type: String = "video",
        @Query("q") query: String,
        @Query("key") apiKey: String = UrlConstant.API_TOKEN,
        @Query("maxResults") maxResults: Int = 20
    ): Response<SearchResponse>

    companion object{
        fun getApi(): ApiInterface? {
            return RetrofitInterface.getInstance().create(ApiInterface::class.java)
        }
    }

}