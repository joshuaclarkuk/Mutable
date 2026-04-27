package com.example.stemselector.model

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SongViewModel (
) : ViewModel() {
    private val _songList = MutableStateFlow<List<Song>>(emptyList())
    private val _currentSong = MutableStateFlow<Song?>(null)

    val songListPublic: StateFlow<List<Song>> = _songList.asStateFlow()
    val currentSongPublic: StateFlow<Song?> = _currentSong.asStateFlow()

    fun addSong(song: Song) {
        _songList.value += song
    }

    fun selectSong(song: Song) {
        _currentSong.value = song
    }
}