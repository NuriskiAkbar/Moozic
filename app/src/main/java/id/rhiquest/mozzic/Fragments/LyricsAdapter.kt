package id.rhiquest.mozzic.Fragments

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import id.rhiquest.mozzic.R

class LyricsAdapter(
    private var lyricLines: List<LyricLine>
) : RecyclerView.Adapter<LyricsAdapter.LyricViewHolder>() {

    private var activePosition: Int = -1

    inner class LyricViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView) {
        fun bind(item: LyricLine, position: Int) {
            textView.text = item.text
            if (position == activePosition) {
                textView.setTextColor(ContextCompat.getColor(textView.context, R.color.accent))
                textView.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textView.alpha = 1.0f
            } else {
                textView.setTextColor(ContextCompat.getColor(textView.context, R.color.text_primary))
                textView.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textView.alpha = 0.5f
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LyricViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lyric_line, parent, false) as TextView
        return LyricViewHolder(view)
    }

    override fun onBindViewHolder(holder: LyricViewHolder, position: Int) {
        holder.bind(lyricLines[position], position)
    }

    override fun getItemCount(): Int = lyricLines.size

    fun updateList(newList: List<LyricLine>) {
        lyricLines = newList
        activePosition = -1
        notifyDataSetChanged()
    }

    fun getActivePosition(): Int = activePosition

    fun setActivePosition(position: Int) {
        if (activePosition != position) {
            val oldActive = activePosition
            activePosition = position
            notifyItemChanged(oldActive)
            notifyItemChanged(activePosition)
        }
    }
}
