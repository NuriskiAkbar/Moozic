package id.rhiquest.mozzic.Utils.NetworkUtils

import retrofit2.Response

class ApiClient {
    suspend fun searchSong(query: String): Response<SearchResponse>? {
        return ApiInterface.getApi()?.searchSong(query = query)
    }
}