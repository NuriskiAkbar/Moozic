package id.rhiquest.mozzic.Fragments

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import id.rhiquest.mozzic.Utils.DataUtils.SongItem
import id.rhiquest.mozzic.Utils.NetworkUtils.BaseResponse
import id.rhiquest.mozzic.databinding.FragmentBrowseBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BrowseFragment: BaseFragment<FragmentBrowseBinding, BrowseViewModel>() {

    private val searchJob: Job? = null

    override val viewModel: BrowseViewModel by viewModels()

    private val playViewModel: PlayViewModel by activityViewModels()

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
    }

    private fun showLoading() {
        binding?.apply {
            progressSonglist.isVisible = true
            rvSongresult.isVisible = false
        }
    }

    private fun showResults(data: List<SongItem>?) {
        binding?.apply {
            progressSonglist.isVisible = false
            rvSongresult.apply {
                isVisible = true
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                adapter = SongResultAdapter(
                    data,
                    onPlayDirectIconClick = { songItem ->
                        playViewModel.playMusic(songItem)
                    },
                    onFavoriteIconClick = { songItem ->
                        Toast.makeText(requireContext(), "${songItem.title}", Toast.LENGTH_SHORT).show()
                    })
            }
        }
    }

    private fun showError(){
        binding.apply {

        }
    }

    override fun initView() {
        binding?.etLokasi?.addTextChangedListener(object : TextWatcher {
            private var searchJob: Job? = null

            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300L)
                    viewModel.songList(s.toString())
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

}