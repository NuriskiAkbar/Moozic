package id.rhiquest.mozzic

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import id.rhiquest.mozzic.Fragments.PlayViewModel
import id.rhiquest.mozzic.Utils.DataUtils.SongItem
import id.rhiquest.mozzic.databinding.ActivityMainBinding
import id.rhiquest.mozzic.databinding.LayoutMiniPlayerBinding
import kotlinx.coroutines.launch
import kotlin.getValue

class MainActivity : BaseActivity<ActivityMainBinding>() {


    private val playViewModel: PlayViewModel by viewModels()
    private var youTubePlayerInstance: YouTubePlayer? = null

    override val inflateBinding: (LayoutInflater) -> ActivityMainBinding
        get() = ActivityMainBinding::inflate

    private val sharedPreferences by lazy {
        getContext().getSharedPreferences("spLogin", MODE_PRIVATE)
    }

    override fun getContext(): Context {
        return this
    }

    override fun initView() {
        initNavbar()
        initYoutube()
    }

    private fun initYoutube() {
        val miniPlayerView = viewBinding.root.findViewById<YouTubePlayerView>(R.id.miniyoutubeplayer)
        lifecycle.addObserver(miniPlayerView)

        miniPlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                youTubePlayerInstance = youTubePlayer

                youTubePlayer.addListener(object : AbstractYouTubePlayerListener(){
                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                        val rounded = second.toInt()
                        if (rounded % 5 == 0){
                            playViewModel.updateSecond(second)
                        }
                    }
                })

                lifecycleScope.launch {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        playViewModel.currentMusic.collect { music ->
                            if (music == null){
                                viewBinding.miniPlayerPanel.root.isVisible = false
                            } else {
                                viewBinding.miniPlayerPanel.root.isVisible = true
                                playMusic(youTubePlayer, music)
                            }
                        }
                    }
                }
            }

            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                Log.e("YTView", "YouTubePlayer error: $error")
            }
        })
    }

    private fun playMusic(youTubePlayer: YouTubePlayer, music: SongItem?) {
        music?.videoId?.let { videoId ->
            youTubePlayer.loadVideo(videoId, 0f)
        }

        viewBinding.miniPlayerPanel.apply {
         tvTitleMiniPlayer.text = music?.title
         tvSingerMiniPlayer.text = music?.singer
        }
    }

    private fun initNavbar() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        bottomNavigationView.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isPlayFragment = destination.id == R.id.play_fragment
            val hasMusic = playViewModel.currentMusic.value != null
            if (!isPlayFragment && hasMusic){
                viewBinding.miniPlayerPanel.apply {
                    
                }
            }
        }
    }
}