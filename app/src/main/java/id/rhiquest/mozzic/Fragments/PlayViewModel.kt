package id.rhiquest.mozzic.Fragments

import android.util.Log
import androidx.lifecycle.ViewModel
import id.rhiquest.mozzic.Utils.DataUtils.SongItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PlayViewModel : ViewModel() {
    private val _currentMusic = MutableStateFlow<SongItem?>(null)
    val currentMusic : StateFlow<SongItem?> = _currentMusic

    private val _currentSecond = MutableStateFlow(0f)
    val currentSecond: StateFlow<Float> = _currentSecond

    fun playMusic(song: SongItem){
        _currentMusic.value = song
        _currentSecond.value = 0f
    }

    fun updateSecond(second: Float){
        Log.e("YTProgress", "ini sudah $second")
        _currentSecond.value = second
    }

}