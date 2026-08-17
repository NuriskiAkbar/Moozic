package id.rhiquest.mozzic.Fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import id.rhiquest.mozzic.Utils.DataUtils.Room.PlaylistWithSongCount
import id.rhiquest.mozzic.databinding.ItemPlaylistBinding

class ChoosePlaylistAdapter(
    private var playlistList: List<PlaylistWithSongCount>,
    private val onItemClick: (PlaylistWithSongCount) -> Unit
) : RecyclerView.Adapter<ChoosePlaylistAdapter.ChoosePlaylistViewHolder>() {

    inner class ChoosePlaylistViewHolder(private val binding: ItemPlaylistBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindData(item: PlaylistWithSongCount) {
            binding.tvPlaylistName.text = item.name
            binding.tvSongCount.text = "${item.songCount} Lagu"
            binding.ivDeletePlaylist.visibility = View.GONE // Sembunyikan tombol delete di dialog pilihan

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChoosePlaylistViewHolder {
        val binding = ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChoosePlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChoosePlaylistViewHolder, position: Int) {
        holder.bindData(playlistList[position])
    }

    override fun getItemCount(): Int = playlistList.size

    fun updateList(newList: List<PlaylistWithSongCount>) {
        playlistList = newList
        notifyDataSetChanged()
    }
}
