package id.rhiquest.mozzic.Fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import id.rhiquest.mozzic.Utils.DataUtils.SongItem
import id.rhiquest.mozzic.Utils.NetworkUtils.BaseResponse
import id.rhiquest.mozzic.databinding.FragmentFavoriteBinding

class FavoriteFragment : BaseFragment<FragmentFavoriteBinding, FavoriteViewModel>() {

    override val viewModel: FavoriteViewModel by viewModels()

    private val playViewModel: PlayViewModel by activityViewModels()

    override val inflateBinding: (LayoutInflater, ViewGroup?, Boolean) -> FragmentFavoriteBinding
        get() = FragmentFavoriteBinding::inflate

    private var adapter: FavoriteAdapter? = null

    override fun initObserver() {
        viewModel.favorites.observe(viewLifecycleOwner) {
            when (it) {
                is BaseResponse.Loading -> showLoading()
                is BaseResponse.Success -> showFavorites(it.data)
                is BaseResponse.Error -> showError(it.msg)
            }
        }
    }

    override fun initView() {
        viewModel.loadFavorites()
    }

    private fun showLoading() {
        binding?.apply {
            progressFavorite.isVisible = true
            rvFavorites.isVisible = false
            llEmptyState.isVisible = false
        }
    }

    private fun showFavorites(data: List<SongItem>?) {
        binding?.apply {
            progressFavorite.isVisible = false

            if (data.isNullOrEmpty()) {
                rvFavorites.isVisible = false
                llEmptyState.isVisible = true
            } else {
                llEmptyState.isVisible = false
                rvFavorites.isVisible = true
                rvFavorites.layoutManager =
                    LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

                if (adapter == null) {
                    adapter = FavoriteAdapter(
                        favoriteList = data,
                        onPlayClick = { songItem ->
                            playViewModel.playMusic(songItem)
                        },
                        onDeleteClick = { songItem ->
                            viewModel.deleteFavorite(songItem)
                            Toast.makeText(context, "Dihapus dari favorit", Toast.LENGTH_SHORT).show()
                        }
                    )
                    rvFavorites.adapter = adapter
                } else {
                    adapter?.updateList(data)
                }
            }
        }
    }

    private fun showError(msg: String?) {
        binding?.apply {
            progressFavorite.isVisible = false
            rvFavorites.isVisible = false
            llEmptyState.isVisible = true
            Toast.makeText(context, msg ?: "Terjadi kesalahan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadFavorites()
    }
}