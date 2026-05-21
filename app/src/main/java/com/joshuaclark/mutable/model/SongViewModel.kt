package com.joshuaclark.mutable.model

import android.R
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joshuaclark.mutable.engine.StemPlayer
import com.joshuaclark.mutable.tools.SongImporter
import com.joshuaclark.mutable.tools.loadSongs
import com.joshuaclark.mutable.tools.saveSongs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File


class SongViewModel(application: Application) : AndroidViewModel(application) {
    private val songImporter = SongImporter(
        application.applicationContext,
        viewModelScope
    )
    private val stemPlayer =
        StemPlayer(viewModelScope)

    private val _importError = MutableStateFlow<String?>(null)
    private val _songList = MutableStateFlow<List<Song>>(emptyList())
    private val _currentSong = MutableStateFlow<Song?>(null)
    private val _muteStates = MutableStateFlow<Map<String, Boolean>>(emptyMap<String, Boolean>())
    private val _playbackPosition = MutableStateFlow<Long>(0)
    private val _isLooping = MutableStateFlow<Boolean>(false)
    private val _isImporting = MutableStateFlow<Boolean>(false)

    val songListPublic: StateFlow<List<Song>> = _songList.asStateFlow()
    val currentSongPublic: StateFlow<Song?> = _currentSong.asStateFlow()
    val muteStatesPublic = _muteStates.asStateFlow()
    val playbackPositionPublic = _playbackPosition.asStateFlow()
    val isLoopingPublic = _isLooping.asStateFlow()
    val isImportingPublic = _isImporting.asStateFlow()
    val importErrorPublic = _importError.asStateFlow()

    // Pass-through variable that's updated whenever totalSongLengthInBytes is updated
    val totalSongLengthInBytes: Long
        get() = stemPlayer.totalSongLengthInBytes

    init {
        // Delete temporary folder to clear orphan stems
        val tempDirectory = File(getApplication<Application>().filesDir, "temp")
        if (tempDirectory.exists()) {
            tempDirectory.deleteRecursively()
        }

        // Delete empty song folders from previous deletions
        val songsFolder = File(getApplication<Application>().filesDir, "songs")
        if (songsFolder.exists()) {
            for (folder in songsFolder.listFiles() ?: emptyArray()) {
                if (folder.listFiles()?.isEmpty() == true) {
                    folder.delete()
                }
            }
        }

        // Load sample song
        songImporter.loadSampleSong { song ->
            _songList.value += song
            saveSongs(getApplication<Application>(), _songList.value)
        }

        // Load songs from JSON
        _songList.value =
            loadSongs(getApplication<Application>())
    }

    override fun onCleared() {
        stemPlayer.stop()
        stemPlayer.audioTrack?.release()
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
        saveSongs(
            getApplication<Application>(),
            _songList.value
        )
    }

    fun selectSong(song: Song) {
        stemPlayer.teardown()
        _playbackPosition.value = 0
        _currentSong.value = song
        _muteStates.value = song.stemPaths.associateWith { false }
        song.stemPaths.forEach { stemPlayer.setMuted(it, false) }
    }

    fun deleteSong(song: Song) {
        for (stemPath in song.stemPaths) {
            val doc = File(stemPath)
            if (doc.exists()) {
                doc.delete()
            }
        }

        // Remove from song list
        _songList.value -= song

        // Delete sample song
        if (song.sourceFolderUri == "bundled") {
            getApplication<Application>()
                .getSharedPreferences("mutable_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("sample_loaded", false).apply()
        }

        saveSongs(
            getApplication<Application>(),
            _songList.value
        )
    }

    fun toggleMute(stemPath: String) {
        val newMap = _muteStates.value.toMutableMap()
        newMap[stemPath] = !(newMap[stemPath] ?: false)
        _muteStates.value = newMap
        stemPlayer.setMuted(stemPath, newMap[stemPath]!!)
    }

    fun clearError() {
        _importError.value = null
    }

    fun play() {
        if (_currentSong.value != null) {
            stemPlayer.play(_currentSong.value!!.stemPaths)
            updatePlaybackPosition()
        }
    }

    fun pause() {
        stemPlayer.pause()
    }

    fun stop() {
        stemPlayer.stop()
        _playbackPosition.value = 0
    }

    fun toggleLoop() {
        val isLooping = !_isLooping.value
        _isLooping.value = isLooping
        stemPlayer.isLooping = isLooping
    }

    fun seekTo(positionBytes: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            stemPlayer.seekTo(positionBytes)
        }
    }

    fun updatePlaybackPosition() {
        viewModelScope.launch {
            while(true) {
                _playbackPosition.value = stemPlayer.currentPositionBytes
                delay(50)
            }
        }
    }

    fun teardown() {
        _isLooping.value = false
        stemPlayer.isLooping = false
        stemPlayer.teardown()
    }
}