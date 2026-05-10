package id.rhiquest.mozzic.Utils.DataUtils.Room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_song_table")
data class LocalSongEntity (
    @PrimaryKey @ColumnInfo(name = "idLocalSong") val idLocalSong: Long,
    @ColumnInfo(name = "titleLocalSong") val titleLocalSong: String,
    @ColumnInfo(name = "artistLocalSong") val artistLocalSong: String,
    @ColumnInfo(name = "uriLocalSong") val uriLocalSong: String,
)