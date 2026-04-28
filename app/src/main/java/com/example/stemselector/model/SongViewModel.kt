package com.example.stemselector.model

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stemselector.tools.SongImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.stemselector.tools.loadSongs
import com.example.stemselector.tools.saveSongs


class SongViewModel(application: Application) : AndroidViewModel(application) {
    private val songImporter = SongImporter(application.applicationContext, viewModelScope)
    private val _songList = MutableStateFlow<List<Song>>(emptyList())
    private val _currentSong = MutableStateFlow<Song?>(null)
    private val _muteStates = MutableStateFlow<Map<String, Boolean>>(emptyMap<String, Boolean>())
    private val _isImporting = MutableStateFlow<Boolean>(false)

    val songListPublic: StateFlow<List<Song>> = _songList.asStateFlow()
    val currentSongPublic: StateFlow<Song?> = _currentSong.asStateFlow()
    val muteStatesPublic = _muteStates.asStateFlow()
    val isImportingPublic = _isImporting.asStateFlow()

    init {
        _songList.value = loadSongs(getApplication<Application>())
    }

    fun importSong(uri: Uri) {
        _isImporting.value = true
        songImporter.importSong(uri) { song ->
            addSong(song)
            _isImporting.value = false
        }
    }

    fun addSong(song: Song) {
        _songList.value += song
        saveSongs(getApplication<Application>(), _songList.value)
    }

    fun selectSong(song: Song) {
        _currentSong.value = song

        _muteStates.value = song.stemPaths.associateWith { false }
    }

    fun toggleMute(stemPath: String) {
        val newMap = _muteStates.value.toMutableMap()
        newMap[stemPath] = !newMap[stemPath]!!
        _muteStates.value = newMap
    }
}