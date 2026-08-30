package id.rhiquest.mozzic.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import id.rhiquest.mozzic.Utils.DataUtils.SongItem
import id.rhiquest.mozzic.databinding.FragmentPlaylistDetailBinding
import kotlinx.coroutines.launch

class PlaylistDetailFragment : BaseFragment<FragmentPlaylistDetailBinding, PlaylistViewModel>() {

    override val viewModel: PlaylistViewModel by activityViewModels()
    private val playViewModel: PlayViewModel by activityViewModels()

    private lateinit var songAdapter: PlaylistSongAdapter
    private var playlistId: Int = -1
    private var playlistName: String = ""

    override val inflateBinding: (LayoutInflater, ViewGroup?, Boolean) -> FragmentPlaylistDetailBinding
        get() = FragmentPlaylistDetailBinding::inflate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            playlistId = it.getInt("playlistId", -1)
            playlistName = it.getString("playlistName", "")
        }
    }

    override fun initObserver() {
        // Mengamati daftar lagu di playlist ini
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playlistSongs.collect { list ->
                    songAdapter.updateList(list)
                    if (list.isEmpty()) {
                        binding?.llEmptySongs?.visibility = View.VISIBLE
                        binding?.rvPlaylistSongs?.visibility = View.GONE
                    } else {
                        binding?.llEmptySongs?.visibility = View.GONE
                        binding?.rvPlaylistSongs?.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun initView() {
        // Set nama playlist di title header
        binding?.tvPlaylistTitle?.text = playlistName

        // Tombol back
        binding?.ivBack?.setOnClickListener {
            findNavController().navigateUp()
        }

        // Inisialisasi adapter
        songAdapter = PlaylistSongAdapter(
            songList = emptyList(),
            onPlayClick = { playlistSong ->
                // Map PlaylistSongEntity ke SongItem agar bisa diputar
                val songItem = SongItem(
                    singer = playlistSong.artist,
                    title = playlistSong.title,
                    thumbanailUrl = playlistSong.image,
                    videoId = playlistSong.songId
                )
                val queue = viewModel.playlistSongs.value.map {
                    SongItem(
                        singer = it.artist,
                        title = it.title,
                        thumbanailUrl = it.image,
                        videoId = it.songId
                    )
                }
                playViewModel.playMusic(songItem, queue, playlistId)
            },
            onDeleteClick = { playlistSong ->
                viewModel.deleteSongFromPlaylist(playlistSong.id, playlistId)
                Toast.makeText(requireContext(), "Lagu '${playlistSong.title}' dihapus dari playlist", Toast.LENGTH_SHORT).show()
            }
        )

        binding?.rvPlaylistSongs?.adapter = songAdapter

        // Ambil data lagu dari database untuk playlist ini
        if (playlistId != -1) {
            viewModel.fetchSongsForPlaylist(playlistId)
        }
    }
}
