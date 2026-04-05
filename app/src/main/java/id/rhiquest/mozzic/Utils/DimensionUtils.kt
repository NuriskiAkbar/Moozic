package id.rhiquest.mozzic.Utils

import android.content.Context
import android.view.View

object DimensionUtils{
    fun Context.dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    fun View.dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}

