package com.example.stemselector.model

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stemselector.tools.SongImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.stemselector.tools.loadSongs
import com.example.stemselector.tools.saveSongs
import java.io.File


class SongViewModel(application: Application) : AndroidViewModel(application) {
    private val songImporter = SongImporter(application.applicationContext, viewModelScope)
    private val _importError = MutableStateFlow<String?>(null)
    private val _songList = MutableStateFlow<List<Song>>(emptyList())
    private val _currentSong = MutableStateFlow<Song?>(null)
    private val _muteStates = MutableStateFlow<Map<String, Boolean>>(emptyMap<String, Boolean>())
    private val _isImporting = MutableStateFlow<Boolean>(false)

    val songListPublic: StateFlow<List<Song>> = _songList.asStateFlow()
    val currentSongPublic: StateFlow<Song?> = _currentSong.asStateFlow()
    val muteStatesPublic = _muteStates.asStateFlow()
    val isImportingPublic = _isImporting.asStateFlow()
    val importErrorPublic = _importError.asStateFlow()

    init {
        _songList.value = loadSongs(getApplication<Application>())
    }

    fun importSong(uri: Uri) {
        // Clear existing error message if active
        _importError.value = null

        // Check if any song in _songList has the same title as the folder being imported
        for (song: Song in _songList.value) {
            if (song.sourceFolderUri == uri.toString()) {
                _importError.value = "Song already imported!"
                return
            }
        }

        _isImporting.value = true
        songImporter.importSong(uri, onError = { message ->
            _importError.value = message
            _isImporting.value = false
        }) { song ->
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

    fun deleteSong(song: Song) {
        for (stemPath in song.stemPaths) {
            val doc = File(stemPath)
            if (doc.exists()) {
                doc.delete()
            }
        }
        _songList.value -= song
        saveSongs(getApplication<Application>(), _songList.value)
    }

    fun toggleMute(stemPath: String) {
        val newMap = _muteStates.value.toMutableMap()
        newMap[stemPath] = !newMap[stemPath]!!
        _muteStates.value = newMap
    }

    fun clearError() {
        _importError.value = null
    }
}