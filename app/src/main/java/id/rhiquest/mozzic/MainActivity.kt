package id.rhiquest.mozzic

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import id.rhiquest.mozzic.Fragments.PlayViewModel
import id.rhiquest.mozzic.Service.MusicService
import id.rhiquest.mozzic.Utils.DataUtils.SongItem
import id.rhiquest.mozzic.Utils.NetworkUtils.TextUtils
import id.rhiquest.mozzic.databinding.ActivityMainBinding
import id.rhiquest.mozzic.databinding.LayoutMiniPlayerBinding
import kotlinx.coroutines.launch

class MainActivity : BaseActivity<ActivityMainBinding>() {

    private val playViewModel: PlayViewModel by viewModels()
    private var youTubePlayerInstance: YouTubePlayer? = null
    private var miniPlayerYouTubeView: YouTubePlayerView? = null
    private var lastLoadedVideoId: String? = null
    private lateinit var navController: NavController
    private var exoPlayerInstance: ExoPlayer? = null
    private var isPlayingLocal: Boolean = false

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
        initExoPlayer()
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
                if (isPlayingLocal) {
                    if (isPlaying) {
                        exoPlayerInstance?.play()
                    } else {
                        exoPlayerInstance?.pause()
                    }
                } else {
                    // Only control YouTube player after initial setup to avoid
                    // interrupting playback when activity resumes from background
                    if (isPlayerInitialized && !isLoadingNewVideo) {
                        if (isPlaying) {
                            youTubePlayerInstance?.play()
                        } else {
                            youTubePlayerInstance?.pause()
                        }
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
        val intent = Intent(this, MusicService::class.java).apply {
            putExtra(MusicService.EXTRA_TITLE, song.title)
            putExtra(MusicService.EXTRA_SINGER, song.singer)
            putExtra(MusicService.EXTRA_IS_PLAYING, playViewModel.isPlaying.value)
            putExtra(MusicService.EXTRA_THUMBNAIL_URL, song.thumbanailUrl)
            putExtra(MusicService.EXTRA_CURRENT_SECOND, playViewModel.currentSecond.value)
            putExtra(MusicService.EXTRA_TOTAL_DURATION, playViewModel.totalDuration.value)
        }
        startService(intent)
    }

    private fun initExoPlayer() {
        exoPlayerInstance = ExoPlayer.Builder(this).build()
        exoPlayerInstance?.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                super.onPlayWhenReadyChanged(playWhenReady, reason)
                playViewModel.setPlaying(playWhenReady)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    playViewModel.playNextFromFavorites()
                }
            }
        })

        lifecycleScope.launch {
            while (true) {
                if (isPlayingLocal && exoPlayerInstance?.isPlaying == true) {
                    val duration = (exoPlayerInstance?.duration ?: 0L) / 1000f
                    if (duration > 0) {
                        playViewModel.updateDuration(duration)
                    }
                    val currentPos = (exoPlayerInstance?.currentPosition ?: 0L) / 1000F
                    playViewModel.updateSecond(currentPos)
                }
                kotlinx.coroutines.delay(1000)
            }
        }
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
            if (videoId.contains("content", ignoreCase = true)){
                playSongFromLocal(videoId)
            } else {
                playSongFromYoutube(youTubePlayer, videoId, shouldPlay)
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

    private fun playSongFromLocal(videoId: String?) {
        isPlayingLocal = true
        youTubePlayerInstance?.pause()
        val mediaItem = MediaItem.fromUri(
            videoId!!.toUri()
        )

        exoPlayerInstance?.setMediaItem(mediaItem)
        exoPlayerInstance?.prepare()
        exoPlayerInstance?.play()
    }

    private fun playSongFromYoutube(
        youTubePlayer: YouTubePlayer,
        videoId: String,
        shouldPlay: Boolean
    ){
        isPlayingLocal = false
        exoPlayerInstance?.pause()
        if (shouldPlay) {
            youTubePlayer.loadVideo(videoId, 0f)  // auto-play
        } else {
            youTubePlayer.cueVideo(videoId, 0f)   // load only, no auto-play
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
            tvTitleMiniPlayer.text = item?.title
            tvSingerMiniPlayer.text = item?.singer
    }

    private fun startMusicService(song: SongItem, isPlaying: Boolean) {
        val intent = Intent(this, MusicService::class.java).apply {
            putExtra(MusicService.EXTRA_TITLE, song.title)
            putExtra(MusicService.EXTRA_SINGER, song.singer)
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