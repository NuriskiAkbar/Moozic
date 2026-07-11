package id.rhiquest.mozzic.Fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import id.rhiquest.mozzic.R
import id.rhiquest.mozzic.Utils.DataUtils.SongItem
import id.rhiquest.mozzic.databinding.ItemFavoriteBinding

class FavoriteAdapter(
    private var favoriteList: List<SongItem>,
    private val onPlayClick: (SongItem) -> Unit,
    private val onDeleteClick: (SongItem) -> Unit
) : RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder>() {

    inner class FavoriteViewHolder(private val binding: ItemFavoriteBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindData(item: SongItem) {
            binding.tvFavoriteTitle.text = item.title
            binding.tvFavoriteSinger.text = item.singer
            Glide.with(binding.ivFavoriteThumbnail)
                .load(item.thumbanailUrl)
                .error(R.drawable.icon_thumbnail_error)
                .fitCenter()
                .into(binding.ivFavoriteThumbnail)

            binding.ivFavoritePlay.setOnClickListener {
                onPlayClick(item)
            }

            binding.ivFavoriteDelete.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val binding = ItemFavoriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FavoriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bindData(favoriteList[position])
    }

    override fun getItemCount(): Int = favoriteList.size

    fun updateList(newList: List<SongItem>) {
        favoriteList = newList
        notifyDataSetChanged()
    }
}
