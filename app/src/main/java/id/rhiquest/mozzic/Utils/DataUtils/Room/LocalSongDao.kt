package id.rhiquest.mozzic.Utils.DataUtils.Room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocalSongDao {
    @Query("SELECT * FROM local_song_table")
    suspend fun getAllLocalSongs(): List<LocalSongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalSong(song: LocalSongEntity)

    @Query("DELETE FROM local_song_table WHERE uriLocalSong = :uri")
    suspend fun deleteLocalSong(uri: String)
}
