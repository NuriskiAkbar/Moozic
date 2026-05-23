package id.rhiquest.mozzic.Utils.DataUtils.Room

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `local_song_table` (`idLocalSong` INTEGER NOT NULL, `titleLocalSong` TEXT NOT NULL, `artistLocalSong` TEXT NOT NULL, `uriLocalSong` TEXT NOT NULL, PRIMARY KEY(`idLocalSong`))")
        }
    }
}
