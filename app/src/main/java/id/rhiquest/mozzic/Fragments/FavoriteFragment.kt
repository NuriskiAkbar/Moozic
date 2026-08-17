package id.rhiquest.mozzic.Fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import id.rhiquest.mozzic.Utils.DataUtils.SongItem
import id.rhiquest.mozzic.Utils.NetworkUtils.BaseResponse
import id.rhiquest.mozzic.databinding.FragmentFavoriteBinding
import id.rhiquest.mozzic.R

class FavoriteFragment : BaseFragment<FragmentFavoriteBinding, FavoriteViewModel>() {

    override val viewModel: FavoriteViewModel by viewModels()

    private val playViewModel: PlayViewModel by activityViewModels()

    override val inflateBinding: (LayoutInflater, ViewGroup?, Boolean) -> FragmentFavoriteBinding
        get() = FragmentFavoriteBinding::inflate

    private var adapter: FavoriteAdapter? = null
    private var localAdapter: FavoriteAdapter? = null

    private val openMultipleDocumentsLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            handleSelectedAudio(uri)
        }
    }

    override fun initObserver() {
        viewModel.favorites.observe(viewLifecycleOwner) {
            when (it) {
                is BaseResponse.Loading -> showLoading()
                is BaseResponse.Success -> showFavorites(it.data)
                is BaseResponse.Error -> showError(it.msg)
            }
        }
        
        viewModel.localSongs.observe(viewLifecycleOwner) {
            when (it) {
                is BaseResponse.Loading -> showLocalLoading()
                is BaseResponse.Success -> showLocalSongs(it.data)
                is BaseResponse.Error -> showLocalError(it.msg)
            }
        }
    }

    override fun initView() {
        setupHeaders()
        viewModel.loadFavorites()
        viewModel.loadLocalSongs()
        
        binding?.btnAddLocalSong?.setOnClickListener {
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        openMultipleDocumentsLauncher.launch(arrayOf("audio/*"))
    }

    private fun handleSelectedAudio(uri: Uri) {
        try {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(requireContext(), uri)
            
            val title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Unknown Title"
            val artist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            
            viewModel.addLocalSong(uri.toString(), title, artist)
            retriever.release()
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal memuat lagu: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupHeaders() {
        binding?.apply {
            clOnlineHeader.setOnClickListener {
                val isCurrentlyVisible = flOnlineContent.isVisible
                flOnlineContent.isVisible = !isCurrentlyVisible
                ivOnlineArrow.setImageResource(
                    if (!isCurrentlyVisible) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
                )
            }

            clOfflineHeader.setOnClickListener {
                val isCurrentlyVisible = flOfflineContent.isVisible
                flOfflineContent.isVisible = !isCurrentlyVisible
                ivOfflineArrow.setImageResource(
                    if (!isCurrentlyVisible) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
                )
            }
        }
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
                            playViewModel.playMusic(songItem, data)
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

    private fun showLocalLoading() {
        binding?.apply {
            progressLocal.isVisible = true
            rvLocalSongs.isVisible = false
            llEmptyLocal.isVisible = false
        }
    }

    private fun showLocalSongs(data: List<SongItem>?) {
        binding?.apply {
            progressLocal.isVisible = false

            if (data.isNullOrEmpty()) {
                rvLocalSongs.isVisible = false
                llEmptyLocal.isVisible = true
                Toast.makeText(context, "Tidak ada lagu lokal ditemukan di perangkat ini", Toast.LENGTH_SHORT).show()
            } else {
                llEmptyLocal.isVisible = false
                rvLocalSongs.isVisible = true
                rvLocalSongs.layoutManager =
                    LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

                if (localAdapter == null) {
                    localAdapter = FavoriteAdapter(
                        favoriteList = data,
                        onPlayClick = { songItem ->
                            playViewModel.playMusic(songItem, data)
                        },
                        onDeleteClick = { songItem ->
                            Toast.makeText(context, "Hanya dapat menghapus lagu favorit", Toast.LENGTH_SHORT).show()
                        }
                    )
                    rvLocalSongs.adapter = localAdapter
                } else {
                    localAdapter?.updateList(data)
                }
            }
        }
    }

    private fun showLocalError(msg: String?) {
        binding?.apply {
            progressLocal.isVisible = false
            rvLocalSongs.isVisible = false
            llEmptyLocal.isVisible = true
            Toast.makeText(context, msg ?: "Terjadi kesalahan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadFavorites()
    }
}