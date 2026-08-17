package id.rhiquest.mozzic.Fragments

import android.app.Dialog
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import id.rhiquest.mozzic.R
import id.rhiquest.mozzic.Utils.DataUtils.SongItem
import id.rhiquest.mozzic.Utils.DataUtils.Room.PlaylistWithSongCount
import id.rhiquest.mozzic.Utils.NetworkUtils.BaseResponse
import id.rhiquest.mozzic.databinding.FragmentBrowseBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BrowseFragment: BaseFragment<FragmentBrowseBinding, BrowseViewModel>() {

    override val viewModel: BrowseViewModel by activityViewModels()

    private val playViewModel: PlayViewModel by activityViewModels()
    private val playlistViewModel: PlaylistViewModel by activityViewModels()

    private var playlistsList: List<PlaylistWithSongCount> = emptyList()
    private var activeChooseDialogAdapter: ChoosePlaylistAdapter? = null

    override val inflateBinding: (LayoutInflater, ViewGroup?, Boolean) -> FragmentBrowseBinding
        get() = FragmentBrowseBinding::inflate

    override fun initObserver() {
        viewModel.songResults.observe(this) {
            when(it){
                is BaseResponse.Loading -> {
                    showLoading()
                }
                is BaseResponse.Success -> {
                    showResults(it.data)
                }
                is BaseResponse.Error -> {
                    showError()
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                playlistViewModel.playlists.collect { list ->
                    playlistsList = list
                    activeChooseDialogAdapter?.updateList(list)
                }
            }
        }
    }

    private fun showLoading() {
        binding?.apply {
            progressSonglist.isVisible = true
            rvSongresult.isVisible = false
            animationView.isVisible = false
            tvSearchHint.isVisible = false
        }
    }

    private fun showResults(data: List<SongItem>?) {
        binding?.apply {
            progressSonglist.isVisible = false
            animationView.isVisible = false
            tvSearchHint.isVisible = false
            rvSongresult.apply {
                isVisible = true
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                adapter = SongResultAdapter(
                    data,
                    onPlayDirectIconClick = { songItem ->
                        playViewModel.playMusic(songItem, data ?: emptyList())
                    },
                    onFavoriteIconClick = { songItem ->
                        playViewModel.addToFavorites(songItem)
                        Toast.makeText(context, "Ditambahkan ke favorit", Toast.LENGTH_SHORT).show()
                    },
                    onPlaylistIconClick = { songItem ->
                        showChoosePlaylistDialog(songItem)
                    })
            }
        }
    }

    private fun showChoosePlaylistDialog(song: SongItem) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_choose_playlist)

        val llCreateOption = dialog.findViewById<View>(R.id.ll_create_new_playlist_option)
        val rvPlaylistsChoice = dialog.findViewById<RecyclerView>(R.id.rv_playlists_choice)

        // Set up adapter
        val chooseAdapter = ChoosePlaylistAdapter(playlistsList) { playlist ->
            playlistViewModel.addSongToPlaylist(playlist.id, song)
            Toast.makeText(requireContext(), "Lagu ditambahkan ke playlist ${playlist.name}", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        activeChooseDialogAdapter = chooseAdapter
        rvPlaylistsChoice.adapter = chooseAdapter

        dialog.setOnDismissListener {
            activeChooseDialogAdapter = null
        }

        // Click on Create New Playlist
        llCreateOption.setOnClickListener {
            dialog.dismiss()
            showCreatePlaylistAndAddDialog(song)
        }

        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun showCreatePlaylistAndAddDialog(song: SongItem) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.add_new_playlist_dialog)

        val editText = dialog.findViewById<EditText>(R.id.et_add_new_playlist_name)
        dialog.findViewById<Button>(R.id.btn_save_playlist).setOnClickListener {
            val newPlaylistName = editText.text.toString()
            if (newPlaylistName.isBlank()) {
                Toast.makeText(requireContext(), "Nama playlist tidak boleh kosong", Toast.LENGTH_SHORT).show()
            } else {
                playlistViewModel.createPlaylistAndAddSong(newPlaylistName, song)
                Toast.makeText(requireContext(), "Playlist '$newPlaylistName' dibuat & lagu ditambahkan", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun showError(){
        binding.apply {

        }
    }

    override fun initView() {
        binding?.etSearchLagu?.addTextChangedListener(object : TextWatcher {
            private var searchJob: Job? = null

            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                if (s.isNullOrEmpty()) {
                    // Show Lottie animation and hint text when search is cleared
                    binding?.apply {
                        animationView.isVisible = true
                        tvSearchHint.isVisible = true
                        rvSongresult.isVisible = false
                        progressSonglist.isVisible = false
                    }
                    return
                }
                searchJob = lifecycleScope.launch {
                    val query = s.toString()
                    val videoId = viewModel.extractYoutubeVideoId(query)
                    
                    if (videoId != null) {
                        // Bypass delay for direct youtube link
                        viewModel.parseYoutubeUrl(query, videoId)
                    } else {
                        // Standard debounce delay for text search
                        delay(3000L)
                        viewModel.songList(query)
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

}