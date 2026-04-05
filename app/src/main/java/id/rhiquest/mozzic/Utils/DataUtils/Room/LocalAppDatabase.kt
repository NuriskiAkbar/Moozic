package id.rhiquest.mozzic.Utils.DataUtils.Room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class
               ],
    version = 1,
    exportSchema = false
)
abstract class LocalAppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        const val DATABASE_NAME = "local_app_database"

        @Volatile
        private var INSTANCE: LocalAppDatabase? = null

        fun getInstance(context: Context): LocalAppDatabase {
           if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE = buildRoomDB(context)
                }
           }
            return INSTANCE!!
        }

        private fun buildRoomDB(context: Context) = Room.databaseBuilder(
            context.applicationContext,
            LocalAppDatabase::class.java,
            DATABASE_NAME
        ).build()
    }
}