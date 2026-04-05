package id.rhiquest.mozzic.Utils.NetworkUtils

import android.widget.TextView
import id.rhiquest.mozzic.Utils.DataUtils.SongItem

object TextUtils {
    fun getSingerTitle(item: SongItem?): Pair<String?, String?> =
        Pair(item?.singer, item?.title)

    fun parseSingerAndTitle(fullTitle: String?): Pair<String?, String?> {
        // Pecah berdasarkan " - "
        val parts = fullTitle?.split(" - ", limit = 2)
        val singer = parts?.getOrNull(0).orEmpty().trim()
        // Hapus isi dalam tanda kurung (contoh: (Official Lyric))
        val rawTitle = parts?.getOrNull(1).orEmpty().trim()
        val title = rawTitle.substringBefore("(").trim()
        return singer to title
    }
}