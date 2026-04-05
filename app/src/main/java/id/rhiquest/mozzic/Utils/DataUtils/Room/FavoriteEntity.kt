package id.rhiquest.mozzic.Utils.DataUtils.Room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorite_table")
data class FavoriteEntity (
    @PrimaryKey @ColumnInfo(name = "idFavorite") val idFavorite: String,
    @ColumnInfo(name = "titleFavorite") val titleFavorite: String,
    @ColumnInfo(name = "artistFavorite") val artistFavorite: String,
    @ColumnInfo(name = "albumFavorite") val albumFavorite: String,
    @ColumnInfo(name = "durationFavorite") val durationFavorite: String,
    @ColumnInfo(name = "imageFavorite") val imageFavorite: String,
    @ColumnInfo(name = "videoId") val videoId: String,
)