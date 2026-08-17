package id.rhiquest.mozzic.Utils.DataUtils.Room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist_table")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name = "name")
    val name: String
)

data class PlaylistWithSongCount(
    val id: Int,
    val name: String,
    val songCount: Int
)
