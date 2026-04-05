package id.rhiquest.mozzic.Utils.DataUtils.Room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_table")
    suspend fun getAllFavorite(): List<FavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteItem(favoriteEntity: FavoriteEntity)

    @Query("DELETE FROM favorite_table WHERE idFavorite = :idFavorite")
    suspend fun deleteFavoriteItem(idFavorite: String)
}