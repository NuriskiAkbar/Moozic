package id.rhiquest.mozzic.Utils.DataUtils.Room

import id.rhiquest.mozzic.Utils.NetworkUtils.Id

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val uri: String
)
