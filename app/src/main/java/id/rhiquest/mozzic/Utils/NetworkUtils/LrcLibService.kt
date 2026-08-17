package id.rhiquest.mozzic.Utils.NetworkUtils

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class LrcResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("trackName") val trackName: String?,
    @SerializedName("artistName") val artistName: String?,
    @SerializedName("duration") val duration: Double?,
    @SerializedName("instrumental") val instrumental: Boolean?,
    @SerializedName("plainLyrics") val plainLyrics: String?,
    @SerializedName("syncedLyrics") val syncedLyrics: String?
)

class LrcLibService {
    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun fetchLyrics(artist: String, title: String): LrcResponse? = withContext(Dispatchers.IO) {
        val encodedArtist = java.net.URLEncoder.encode(artist, "UTF-8")
        val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
        val url = "https://lrclib.net/api/get?artist_name=$encodedArtist&track_name=$encodedTitle"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MoozicApp/1.0 (contact@moozicapp.com)")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body.string()
                    if (body.isNotEmpty()) {
                        return@withContext gson.fromJson(body, LrcResponse::class.java)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
