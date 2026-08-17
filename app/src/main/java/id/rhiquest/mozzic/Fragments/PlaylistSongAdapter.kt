package id.rhiquest.mozzic.Fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import id.rhiquest.mozzic.R
import id.rhiquest.mozzic.Utils.DataUtils.Room.PlaylistSongEntity
import id.rhiquest.mozzic.databinding.ItemPlaylistSongBinding

class PlaylistSongAdapter(
    private var songList: List<PlaylistSongEntity>,
    private val onPlayClick: (PlaylistSongEntity) -> Unit,
    private val onDeleteClick: (PlaylistSongEntity) -> Unit
) : RecyclerView.Adapter<PlaylistSongAdapter.PlaylistSongViewHolder>() {

    inner class PlaylistSongViewHolder(private val binding: ItemPlaylistSongBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindData(item: PlaylistSongEntity) {
            binding.tvSongTitle.text = item.title
            binding.tvSongSinger.text = item.artist
            
            Glide.with(binding.ivSongThumbnail)
                .load(item.image)
                .error(R.drawable.icon_thumbnail_error)
                .fitCenter()
                .into(binding.ivSongThumbnail)

            binding.ivPlaySong.setOnClickListener {
                onPlayClick(item)
            }

            binding.ivDeleteSong.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistSongViewHolder {
        val binding = ItemPlaylistSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaylistSongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistSongViewHolder, position: Int) {
        holder.bindData(songList[position])
    }

    override fun getItemCount(): Int = songList.size

    fun updateList(newList: List<PlaylistSongEntity>) {
        songList = newList
        notifyDataSetChanged()
    }
}
