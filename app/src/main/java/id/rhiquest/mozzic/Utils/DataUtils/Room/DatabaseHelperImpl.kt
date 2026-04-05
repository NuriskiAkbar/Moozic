package id.rhiquest.mozzic.Utils.DataUtils.Room

class DatabaseHelperImpl (
    private val db: LocalAppDatabase
): DatabaseHelper {
    override suspend fun getFavorites(): List<FavoriteEntity> = db.favoriteDao().getAllFavorite()


    override suspend fun insertFavorite(favoriteEntity: FavoriteEntity) =
        db.favoriteDao().insertFavoriteItem(favoriteEntity)

    override suspend fun deleteFavorite(idFavorite: String) = db.favoriteDao().deleteFavoriteItem(idFavorite)

}