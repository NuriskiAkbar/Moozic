package id.rhiquest.mozzic.Fragments

import android.app.Dialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import id.rhiquest.mozzic.R
import id.rhiquest.mozzic.databinding.FragmentPlaylistBinding

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch

class PlaylistFragment : BaseFragment<FragmentPlaylistBinding, PlaylistViewModel>() {

    override val viewModel: PlaylistViewModel by activityViewModels()
    private var activeDialog: Dialog? = null
    private lateinit var playlistAdapter: PlaylistAdapter

    override val inflateBinding: (LayoutInflater, ViewGroup?, Boolean) -> FragmentPlaylistBinding
        get() = FragmentPlaylistBinding::inflate

    override fun initObserver() {
        // 1. Mengamati alur pembuatan playlist (loading, success, error)
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.createPlaylistResult.collect { result ->
                    val saveButton = activeDialog?.findViewById<Button>(R.id.btn_save_playlist)
                    when (result) {
                        is CreatePlaylistResult.Loading -> {
                            saveButton?.isEnabled = false
                            saveButton?.text = "Membuat..."
                        }
                        is CreatePlaylistResult.Success -> {
                            saveButton?.isEnabled = true
                            saveButton?.text = "Buat Playlist"
                            Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                            activeDialog?.dismiss()
                        }
                        is CreatePlaylistResult.Error -> {
                            saveButton?.isEnabled = true
                            saveButton?.text = "Buat Playlist"
                            Toast.makeText(requireContext(), result.errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        // 2. Mengamati StateFlow daftar playlist dari database local
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playlists.collect { list ->
                    playlistAdapter.updateList(list)
                    if (list.isEmpty()) {
                        binding?.llEmptyPlay?.visibility = View.VISIBLE
                        binding?.rvPlaylists?.visibility = View.GONE
                        binding?.fabAddPlaylist?.visibility = View.GONE
                    } else {
                        binding?.llEmptyPlay?.visibility = View.GONE
                        binding?.rvPlaylists?.visibility = View.VISIBLE
                        binding?.fabAddPlaylist?.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun initView() {
        // Inisialisasi adapter untuk list playlist
        playlistAdapter = PlaylistAdapter(
            playlistList = emptyList(),
            onItemClick = { playlist ->
                val bundle = Bundle().apply {
                    putInt("playlistId", playlist.id)
                    putString("playlistName", playlist.name)
                }
                findNavController().navigate(R.id.action_playlist_to_detail, bundle)
            },
            onDeleteClick = { playlist ->
                viewModel.deletePlaylist(playlist.id)
                Toast.makeText(requireContext(), "Playlist '${playlist.name}' dihapus", Toast.LENGTH_SHORT).show()
            }
        )

        // Hubungkan adapter ke RecyclerView
        binding?.rvPlaylists?.adapter = playlistAdapter

        // Click listener untuk menambah playlist
        binding?.btnAddPlaylist?.setOnClickListener {
            showDialog()
        }
        binding?.fabAddPlaylist?.setOnClickListener {
            showDialog()
        }
    }

    private fun showDialog(){
        val dialog = Dialog(requireContext())
        activeDialog = dialog
        dialog.setContentView(R.layout.add_new_playlist_dialog)

        dialog.setOnDismissListener {
            activeDialog = null
        }

        val editText = dialog.findViewById<EditText>(R.id.et_add_new_playlist_name)
        dialog.findViewById<Button>(R.id.btn_save_playlist).setOnClickListener {
            val newPlaylistName = editText.text.toString()
            viewModel.createPlaylist(newPlaylistName)
        }
        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}