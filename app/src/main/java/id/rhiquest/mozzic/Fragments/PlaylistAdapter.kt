package id.rhiquest.mozzic.Fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import id.rhiquest.mozzic.Utils.DataUtils.Room.PlaylistWithSongCount
import id.rhiquest.mozzic.databinding.ItemPlaylistBinding

class PlaylistAdapter(
    private var playlistList: List<PlaylistWithSongCount>,
    private val onItemClick: (PlaylistWithSongCount) -> Unit,
    private val onDeleteClick: (PlaylistWithSongCount) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    inner class PlaylistViewHolder(private val binding: ItemPlaylistBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindData(item: PlaylistWithSongCount) {
            binding.tvPlaylistName.text = item.name
            binding.tvSongCount.text = "${item.songCount} Lagu"

            binding.root.setOnClickListener {
                onItemClick(item)
            }

            binding.ivDeletePlaylist.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bindData(playlistList[position])
    }

    override fun getItemCount(): Int = playlistList.size

    fun updateList(newList: List<PlaylistWithSongCount>) {
        playlistList = newList
        notifyDataSetChanged()
    }
}
