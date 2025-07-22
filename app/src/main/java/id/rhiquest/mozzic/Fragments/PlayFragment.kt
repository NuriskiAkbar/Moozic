package id.rhiquest.mozzic.Fragments

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import id.rhiquest.mozzic.R
import id.rhiquest.mozzic.Utils.DataUtils.SongItem
import id.rhiquest.mozzic.databinding.FragmentPlayBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch


class PlayFragment : BaseFragment<FragmentPlayBinding, PlayViewModel>() {
    override val viewModel: PlayViewModel by activityViewModels()
    private var youTubePlayerInstance: YouTubePlayer? = null

    override val inflateBinding: (LayoutInflater, ViewGroup?, Boolean) -> FragmentPlayBinding
        get() = FragmentPlayBinding::inflate

    private var lastVideoId: String? = null
    private var isFirstLoad = true

    override fun initObserver() {}

    override fun initView() {
        initYoutube()
    }

    private fun initYoutube() {
        val miniPlayerView = binding?.root?.findViewById<YouTubePlayerView>(R.id.main_youtube_player)
        if (miniPlayerView == null) {
            Log.e("YTView", "YouTubePlayerView not found!")
            return
        } else {
            Log.d("YTView", "YouTubePlayerView ditemukan")
        }
        miniPlayerView?.let { lifecycle.addObserver(it) }

        miniPlayerView?.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                youTubePlayerInstance = youTubePlayer
                Log.d("cekmasuk", "ini masuk kok")

                youTubePlayer.addListener(object : AbstractYouTubePlayerListener() {
                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                        viewModel.updateSecond(second)
                    }
                })

                lifecycleScope.launch {
                    viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        combine(
                            viewModel.currentMusic,
                            viewModel.currentSecond
                        ) { song, second ->
                            Pair(song, second)
                        }.collect { (song, second) ->
                            if (song != null && youTubePlayerInstance != null) {
                                val currentId = song.videoId

                                if (song.videoId != lastVideoId) {
                                    youTubePlayerInstance?.cueVideo(currentId, second)
                                    youTubePlayerInstance?.play()
                                    lastVideoId = currentId
                                    isFirstLoad = false
                                } else if (isFirstLoad) {
                                    youTubePlayerInstance?.seekTo(second)
                                    isFirstLoad = false
                                }
                            }
                        }
                    }
                }
            }
            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                Log.e("YTPlayerError", "Player error: $error")
            }

        })
    }

}