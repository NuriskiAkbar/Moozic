package id.rhiquest.mozzic.Utils.DataUtils.Room

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

class LocalAudioDataSourceImpl(
    private val context: Context
): LocalAudioDataSource {
    override suspend fun getLocalSong(): List<LocalSongEntity> {
        val songs = mutableListOf<LocalSongEntity>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val cursor = context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val title = it.getString(titleCol)
                val artist = it.getString(artistCol)

                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                songs.add(
                    LocalSongEntity(
                        idLocalSong = id,
                        titleLocalSong = title,
                        artistLocalSong = artist,
                        uriLocalSong = uri.toString()
                    )
                )
            }
        }
        return songs
    }
}