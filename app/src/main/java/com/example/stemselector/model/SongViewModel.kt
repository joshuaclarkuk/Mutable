package com.example.stemselector.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.stemselector.tools.loadSongs
import com.example.stemselector.tools.saveSongs


class SongViewModel(application: Application) : AndroidViewModel(application) {
    private val _songList = MutableStateFlow<List<Song>>(emptyList())
    private val _currentSong = MutableStateFlow<Song?>(null)

    val songListPublic: StateFlow<List<Song>> = _songList.asStateFlow()
    val currentSongPublic: StateFlow<Song?> = _currentSong.asStateFlow()

    init {
        _songList.value = loadSongs(getApplication<Application>())
    }

    fun addSong(song: Song) {
        _songList.value += song
        saveSongs(getApplication<Application>(), _songList.value)
    }

    fun selectSong(song: Song) {
        _currentSong.value = song
    }
}