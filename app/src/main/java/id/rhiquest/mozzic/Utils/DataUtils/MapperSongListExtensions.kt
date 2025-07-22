package id.rhiquest.mozzic.Utils.DataUtils

import id.rhiquest.mozzic.Utils.NetworkUtils.ItemsItem
import id.rhiquest.mozzic.Utils.NetworkUtils.SearchResponse
import kotlin.math.sin


    fun SearchResponse.toSongItems(): List<SongItem> {
        return this.items?.mapNotNull { item ->
            val singer = item?.snippet?.channelTitle
            val songTitle = item?.snippet?.title
            val thumbnails = item?.snippet?.thumbnails?.medium?.url
            val videoId = item?.id?.videoId
            if (singer != null && songTitle != null && thumbnails != null){
                SongItem(title = songTitle, singer = singer, thumbanailUrl = thumbnails, videoId = videoId.toString())
            } else null
        } ?: emptyList()
    }