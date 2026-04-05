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

    override val viewModel: BrowseViewModel by activityViewModels()

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
                        playViewModel.playMusic(songItem)
                    },
                    onFavoriteIconClick = { songItem ->
                        playViewModel.addToFavorites(songItem)
                        Toast.makeText(context, "Ditambahkan ke favorit", Toast.LENGTH_SHORT).show()
                    })
            }
        }
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