package id.rhiquest.mozzic

import android.annotation.SuppressLint
import com.bumptech.glide.Glide
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import id.rhiquest.mozzic.Fragments.PlayViewModel
import id.rhiquest.mozzic.Service.MusicService
import id.rhiquest.mozzic.Utils.DataUtils.SongItem
import id.rhiquest.mozzic.Utils.DimensionUtils.dpToPx
import id.rhiquest.mozzic.Utils.NetworkUtils.TextUtils
import id.rhiquest.mozzic.databinding.ActivityMainBinding
import id.rhiquest.mozzic.databinding.LayoutMiniPlayerBinding
import kotlinx.coroutines.launch
import kotlin.getValue
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.core.net.toUri

class MainActivity : BaseActivity<ActivityMainBinding>() {

    private val playViewModel: PlayViewModel by viewModels()
    private var youTubePlayerInstance: YouTubePlayer? = null
    private var miniPlayerYouTubeView: YouTubePlayerView? = null
    private var lastLoadedVideoId: String? = null
    private lateinit var navController: NavController

    // BroadcastReceiver for notification actions
    private val musicActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MusicService.ACTION_PLAY -> playViewModel.setPlaying(true)
                MusicService.ACTION_PAUSE -> playViewModel.setPlaying(false)
                MusicService.ACTION_STOP -> playViewModel.stopMusic()
                MusicService.ACTION_NEXT -> playViewModel.playNextFromFavorites()
                MusicService.ACTION_PREVIOUS -> playViewModel.playPreviousFromFavorites()
                MusicService.ACTION_SEEK_TO -> {
                    val seekPos = intent.getFloatExtra(MusicService.EXTRA_SEEK_POSITION, 0f)
                    playViewModel.seekTo(seekPos)
                }
            }
        }
    }

    override val inflateBinding: (LayoutInflater) -> ActivityMainBinding
        get() = ActivityMainBinding::inflate

    private val sharedPreferences by lazy {
        getContext().getSharedPreferences("spLogin", MODE_PRIVATE)
    }

    override fun getContext(): Context {
        return this
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun initView() {
        requestNotificationPermission()
        registerMusicReceiver()
        initNavbar()
        initYoutube()
        initMiniPlayerButtons()
        initSeekObserver()
    }

    private fun registerMusicReceiver() {
        val filter = IntentFilter().apply {
            addAction(MusicService.ACTION_PLAY)
            addAction(MusicService.ACTION_PAUSE)
            addAction(MusicService.ACTION_STOP)
            addAction(MusicService.ACTION_NEXT)
            addAction(MusicService.ACTION_PREVIOUS)
            addAction(MusicService.ACTION_SEEK_TO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(musicActionReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(musicActionReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        miniPlayerYouTubeView?.release()
        try { unregisterReceiver(musicActionReceiver) } catch (_: Exception) {}
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    private var isPlayerInitialized = false
    private var isLoadingNewVideo = false

    private fun initMiniPlayerButtons() {
        // Play/Pause button — just toggle state, observer handles YouTube
        viewBinding.miniPlayerPanel.ivPlayMiniPlayer.setOnClickListener {
            playViewModel.togglePlayPause()
        }

        // Close button
        viewBinding.miniPlayerPanel.ivCloseMiniPlayer.setOnClickListener {
            playViewModel.stopMusic()
        }

        // Observe play/pause state → control YouTube player + update icon + update notification
        lifecycleScope.launch {
            playViewModel.isPlaying.collect { isPlaying ->
                // Only control YouTube player after initial setup to avoid
                // interrupting playback when activity resumes from background
                if (isPlayerInitialized && !isLoadingNewVideo) {
                    if (isPlaying) {
                        youTubePlayerInstance?.play()
                    } else {
                        youTubePlayerInstance?.pause()
                    }
                }
                viewBinding.miniPlayerPanel.ivPlayMiniPlayer.setImageResource(
                    if (isPlaying) R.drawable.pause else R.drawable.play
                )
                // Update notification with current play state
                val music = playViewModel.currentMusic.value
                if (music != null) {
                    startMusicService(music, isPlaying)
                }
            }
        }

        // Observe currentMusic → hide mini player when stopped + manage service
        lifecycleScope.launch {
            playViewModel.currentMusic.collect { music ->
                if (music == null) {
                    viewBinding.miniPlayerPanel.root.isVisible = false
                    stopMusicService()
                }
            }
        }
    }

    private var lastNotifUpdateTime = 0L

    private fun initSeekObserver() {
        // Observe seek events from PlayFragment → apply to YouTube player
        lifecycleScope.launch {
            playViewModel.seekToEvent.collect { second ->
                youTubePlayerInstance?.seekTo(second)
            }
        }

        // Update notification progress periodically (~every 1 second)
        lifecycleScope.launch {
            playViewModel.currentSecond.collect { _ ->
                val now = System.currentTimeMillis()
                if (now - lastNotifUpdateTime >= 1000) {
                    lastNotifUpdateTime = now
                    val music = playViewModel.currentMusic.value
                    if (music != null && playViewModel.isPlaying.value) {
                        updateNotifProgress(music)
                    }
                }
            }
        }
    }

    private fun updateNotifProgress(song: SongItem) {
        val (singer, title) = TextUtils.parseSingerAndTitle(song.title)
        val intent = Intent(this, MusicService::class.java).apply {
            putExtra(MusicService.EXTRA_TITLE, title)
            putExtra(MusicService.EXTRA_SINGER, singer)
            putExtra(MusicService.EXTRA_IS_PLAYING, playViewModel.isPlaying.value)
            putExtra(MusicService.EXTRA_THUMBNAIL_URL, song.thumbanailUrl)
            putExtra(MusicService.EXTRA_CURRENT_SECOND, playViewModel.currentSecond.value)
            putExtra(MusicService.EXTRA_TOTAL_DURATION, playViewModel.totalDuration.value)
        }
        startService(intent)
    }

    private fun initYoutube() {
        val miniPlayerView = viewBinding.root.findViewById<YouTubePlayerView>(R.id.miniyoutubeplayer)
        miniPlayerYouTubeView = miniPlayerView
        miniPlayerView.enableBackgroundPlayback(true)

        miniPlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                youTubePlayerInstance = youTubePlayer
                isPlayerInitialized = true

                youTubePlayer.addListener(object : AbstractYouTubePlayerListener(){
                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                        playViewModel.updateSecond(second)
                    }

                    override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                        playViewModel.updateDuration(duration)
                    }

                    override fun onStateChange(
                        youTubePlayer: YouTubePlayer,
                        state: PlayerConstants.PlayerState
                    ) {
                        when (state) {
                            PlayerConstants.PlayerState.PLAYING -> {
                                isLoadingNewVideo = false
                                playViewModel.setPlaying(true)
                            }
                            PlayerConstants.PlayerState.PAUSED -> {
                                // Ignore PAUSED during video transition to prevent
                                // false pause when switching songs
                                if (!isLoadingNewVideo) {
                                    playViewModel.setPlaying(false)
                                }
                            }
                            PlayerConstants.PlayerState.ENDED -> {
                                // Auto-play next song from favorites when current song ends
                                playViewModel.playNextFromFavorites()
                            }
                            PlayerConstants.PlayerState.VIDEO_CUED, 
                            PlayerConstants.PlayerState.UNKNOWN -> {
                                // Video is ready to be played (via cueVideo) or in unknown state
                                isLoadingNewVideo = false
                            }
                            else -> {}
                        }
                    }
                })

                lifecycleScope.launch {
                    playViewModel.currentMusic.collect { music ->
                        if (music == null){
                            viewBinding.miniPlayerPanel.root.isVisible = false
                            lastLoadedVideoId = null
                        } else {
                            viewBinding.miniPlayerPanel.root.isVisible = true
                            // Only load if video ID changed (prevents restart on resume)
                            if (music.videoId != lastLoadedVideoId) {
                                isLoadingNewVideo = true
                                playMusic(youTubePlayer, music)
                                lastLoadedVideoId = music.videoId
                            }
                        }
                    }
                }
            }

            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                if (error == PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER) {
                    val intent = Intent(Intent.ACTION_VIEW,
                        "https://www.youtube.com/watch?v=M7lc1UVf-VE".toUri())
                    startActivity(intent)
                }
            }
        })
    }

    private fun playMusic(youTubePlayer: YouTubePlayer, music: SongItem?) {
        val shouldPlay = playViewModel.isPlaying.value
        music?.videoId?.let { videoId ->
            Log.d("cekmusik", videoId)
            if (shouldPlay) {
                youTubePlayer.loadVideo(videoId, 0f)  // auto-play
            } else {
                youTubePlayer.cueVideo(videoId, 0f)   // load only, no auto-play
            }
        }

        viewBinding.miniPlayerPanel.apply {
            tvTitleMiniPlayer.text = music?.title
            tvSingerMiniPlayer.text = music?.singer

            // Load thumbnail
            Glide.with(ivMiniThumbnail)
                .load(music?.thumbanailUrl)
                .centerCrop()
                .into(ivMiniThumbnail)
        }

        // Start foreground service for background playback
        if (music != null) {
            startMusicService(music, shouldPlay)
        }
    }

    private fun initNavbar() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        bottomNavigationView.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isPlayFragment = destination.id == R.id.play_fragment
            val hasMusic = playViewModel.currentMusic.value
            when {
                isPlayFragment -> viewBinding.miniPlayerPanel.root.isVisible = false
                hasMusic != null -> adjustViewsForMiniPlayer(hasMusic)
                else -> viewBinding.miniPlayerPanel.root.isVisible = false
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun adjustViewsForMiniPlayer(item: SongItem?) {
        viewBinding.miniPlayerPanel.apply {
            root.isVisible = true

            tvTitleMiniPlayer.setTextColor(Color.WHITE)
            tvSingerMiniPlayer.setTextColor(Color.parseColor("#99FFFFFF"))

            bindTitleAndSinger(item)

            // Load thumbnail
            if (item?.thumbanailUrl != null) {
                Glide.with(ivMiniThumbnail)
                    .load(item.thumbanailUrl)
                    .centerCrop()
                    .into(ivMiniThumbnail)
            }
        }
    }

    private fun LayoutMiniPlayerBinding.bindTitleAndSinger(item: SongItem?) {
        TextUtils.parseSingerAndTitle(item?.title).let { (singer, title) ->
            tvTitleMiniPlayer.text = title
            tvSingerMiniPlayer.text = singer
        }
    }

    private fun startMusicService(song: SongItem, isPlaying: Boolean) {
        val (singer, title) = TextUtils.parseSingerAndTitle(song.title)
        val intent = Intent(this, MusicService::class.java).apply {
            putExtra(MusicService.EXTRA_TITLE, title)
            putExtra(MusicService.EXTRA_SINGER, singer)
            putExtra(MusicService.EXTRA_IS_PLAYING, isPlaying)
            putExtra(MusicService.EXTRA_THUMBNAIL_URL, song.thumbanailUrl)
            putExtra(MusicService.EXTRA_CURRENT_SECOND, playViewModel.currentSecond.value)
            putExtra(MusicService.EXTRA_TOTAL_DURATION, playViewModel.totalDuration.value)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopMusicService() {
        val intent = Intent(this, MusicService::class.java).apply {
            action = MusicService.ACTION_STOP
        }
        startService(intent)
    }
}