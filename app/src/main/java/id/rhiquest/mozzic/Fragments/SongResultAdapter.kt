package id.rhiquest.mozzic.Fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import id.rhiquest.mozzic.Utils.DataUtils.SongItem
import id.rhiquest.mozzic.databinding.ItemSearchBinding

class SongResultAdapter(
    private val songList: List<SongItem>?,
    private val onFavoriteIconClick: (SongItem) -> Unit,
    private val onPlayDirectIconClick: (SongItem) -> Unit,
    private val onPlaylistIconClick: (SongItem) -> Unit
): RecyclerView.Adapter<SongResultAdapter.SongResultViewHolder>() {

    inner class SongResultViewHolder(private val binding: ItemSearchBinding) : RecyclerView.ViewHolder(binding.root){
        fun bindData(songList: SongItem?, isLastPosition: Boolean = false){
            binding.tvTitleSong.text = songList?.title
            binding.tvSinger.text = songList?.singer
            Glide.with(binding.ivbanner)
                .load(songList?.thumbanailUrl)
                .centerCrop()
                .into(binding.ivbanner)
            binding.ivPlayDirect.setOnClickListener {
                onPlayDirectIconClick(songList!!)
            }

            binding.ivAddToFavorite.setOnClickListener {
                onFavoriteIconClick(songList!!)
            }

            binding.ivAddToPlaylist.setOnClickListener {
                onPlaylistIconClick(songList!!)
            }
            binding.llNotFoundSong.visibility = if (isLastPosition) View.VISIBLE else View.GONE
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongResultViewHolder {
        val view = ItemSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongResultViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: SongResultViewHolder,
        position: Int
    ) {
        val isLastPosition = position == itemCount - 1
        holder.bindData(songList?.get(position), isLastPosition)
    }

    override fun getItemCount(): Int = songList!!.size
}