package id.rhiquest.mozzic.Fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import id.rhiquest.mozzic.R
import id.rhiquest.mozzic.Utils.DataUtils.SongItem
import id.rhiquest.mozzic.Utils.NetworkUtils.TextUtils
import id.rhiquest.mozzic.databinding.FragmentPlayBinding
import kotlinx.coroutines.launch


class PlayFragment : BaseFragment<FragmentPlayBinding, PlayViewModel>() {
    override val viewModel: PlayViewModel by activityViewModels()

    override val inflateBinding: (LayoutInflater, ViewGroup?, Boolean) -> FragmentPlayBinding
        get() = FragmentPlayBinding::inflate

    private var isUserSeeking = false
    private lateinit var lyricsAdapter: LyricsAdapter

    override fun initObserver() {
        // Observe current music changes
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentMusic.collect { song ->
                    if (song != null) {
                        showPlayingState(song)
                    } else {
                        showEmptyState()
                    }
                }
            }
        }

        // Observe play/pause state to update button icon
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isPlaying.collect { isPlaying ->
                    binding?.ivPlayPauseBtn?.setImageResource(
                        if (isPlaying) R.drawable.pause else R.drawable.play
                    )
                }
            }
        }

        // Observe current second → update SeekBar + time label + lyrics autoscroll
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentSecond.collect { second ->
                    if (!isUserSeeking) {
                        val total = viewModel.totalDuration.value
                        if (total > 0f) {
                            val progress = ((second / total) * 1000).toInt()
                            binding?.seekBarProgress?.progress = progress
                        }
                        binding?.tvCurrentTime?.text = formatTime(second)
                    }

                    // Autoscroll & Highlight lirik
                    val currentMs = (second * 1000).toLong()
                    val lyricList = viewModel.lyrics.value
                    if (lyricList.isNotEmpty() && viewModel.lyricsState.value == LyricsState.HasLyrics) {
                        val activeIndex = getActiveLineIndex(currentMs, lyricList)
                        if (activeIndex != -1 && activeIndex != lyricsAdapter.getActivePosition()) {
                            lyricsAdapter.setActivePosition(activeIndex)
                            binding?.rvLyrics?.smoothScrollToPosition(activeIndex)
                        }
                    }
                }
            }
        }

        // Observe total duration → update total time label
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.totalDuration.collect { duration ->
                    binding?.tvTotalTime?.text = formatTime(duration)
                }
            }
        }

        // Observe lyrics list updates
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.lyrics.collect { list ->
                    lyricsAdapter.updateList(list)
                }
            }
        }

        // Observe lyrics state to update UI status
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.lyricsState.collect { state ->
                    binding?.apply {
                        when (state) {
                            is LyricsState.Loading -> {
                                tvLyricsStatus.visibility = View.VISIBLE
                                tvLyricsStatus.text = "Memuat lirik dari LRCLIB..."
                                rvLyrics.visibility = View.GONE
                            }
                            is LyricsState.Instrumental -> {
                                tvLyricsStatus.visibility = View.VISIBLE
                                tvLyricsStatus.text = "Lagu Instrumental (Tanpa Lirik) 🎧"
                                rvLyrics.visibility = View.GONE
                            }
                            is LyricsState.NoLyrics -> {
                                tvLyricsStatus.visibility = View.VISIBLE
                                tvLyricsStatus.text = "Lirik tidak ditemukan di LRCLIB"
                                rvLyrics.visibility = View.GONE
                            }
                            is LyricsState.PlainLyrics -> {
                                tvLyricsStatus.visibility = View.GONE
                                rvLyrics.visibility = View.VISIBLE
                            }
                            is LyricsState.HasLyrics -> {
                                tvLyricsStatus.visibility = View.GONE
                                rvLyrics.visibility = View.VISIBLE
                            }
                            else -> {
                                tvLyricsStatus.visibility = View.VISIBLE
                                tvLyricsStatus.text = "Lirik tidak tersedia"
                                rvLyrics.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }
    }

    override fun initView() {
        // Inisialisasi adapter lirik
        lyricsAdapter = LyricsAdapter(emptyList())
        binding?.rvLyrics?.adapter = lyricsAdapter

        initButtons()
        initSeekBar()
        // Show initial state
        val currentSong = viewModel.currentMusic.value
        if (currentSong != null) {
            showPlayingState(currentSong)
        } else {
            showEmptyState()
        }
    }

    private fun getActiveLineIndex(currentMs: Long, lyrics: List<LyricLine>): Int {
        var activeIndex = -1
        for (i in lyrics.indices) {
            if (lyrics[i].timeMs <= currentMs) {
                activeIndex = i
            } else {
                break
            }
        }
        return activeIndex
    }

    private fun initSeekBar() {
        binding?.seekBarProgress?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val total = viewModel.totalDuration.value
                    if (total > 0f) {
                        val seekSecond = (progress / 1000f) * total
                        binding?.tvCurrentTime?.text = formatTime(seekSecond)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                val progress = seekBar?.progress ?: 0
                val total = viewModel.totalDuration.value
                if (total > 0f) {
                    val seekSecond = (progress / 1000f) * total
                    viewModel.seekTo(seekSecond)
                }
            }
        })
    }

    private fun initButtons() {
        binding?.apply {
            // Play/Pause
            btnPlayPause.setOnClickListener {
                viewModel.togglePlayPause()
            }

            // Favorite
            btnFavoritePlay.setOnClickListener {
                val currentSong = viewModel.currentMusic.value
                if (currentSong != null) {
                    viewModel.addToFavorites(currentSong)
                    Toast.makeText(context, "Ditambahkan ke favorit ❤️", Toast.LENGTH_SHORT).show()
                }
            }

            // Previous
            btnPrevious.setOnClickListener {
                viewModel.playPrevious()
            }

            // Next
            btnNext.setOnClickListener {
                viewModel.playNext()
            }

            // Stop
            btnStopPlay.setOnClickListener {
                viewModel.stopMusic()
            }
        }
    }

    private fun showPlayingState(song: SongItem) {
        binding?.apply {
            svPlayingState.isVisible = true
            llEmptyPlay.isVisible = false

            // Load thumbnail
            Glide.with(ivPlayThumbnail)
                .load(song.thumbanailUrl)
                .centerCrop()
                .into(ivPlayThumbnail)

                tvPlayTitle.text = song.title
                tvPlaySinger.text = song.singer

            // Reset progress
            seekBarProgress.progress = 0
            tvCurrentTime.text = "0:00"
            tvTotalTime.text = "0:00"
        }
    }

    private fun showEmptyState() {
        binding?.apply {
            svPlayingState.isVisible = false
            llEmptyPlay.isVisible = true
        }
    }

    private fun formatTime(seconds: Float): String {
        val totalSec = seconds.toInt()
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%d:%02d".format(min, sec)
    }
}