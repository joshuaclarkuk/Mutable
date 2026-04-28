package com.example.stemselector.ui.screens

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.stemselector.model.SongViewModel

@Composable
fun MainMenuScreen(onNavigateToSong: () -> Unit, onImportSong: () -> Unit, songViewModel: SongViewModel) {
    val isImporting by songViewModel.isImportingPublic.collectAsState()
    val importError by songViewModel.importErrorPublic.collectAsState()
    val songList by songViewModel.songListPublic.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = WindowInsets.systemBars.asPaddingValues()) {
        item { DisplayImportSongButton(onImportSong, isImporting) }

        if (importError != null) {
            item {
                Text(importError.toString())
                Button(onClick = { songViewModel.clearError() }) {
                    Text("Dismiss")
                }
            }
        }

        if (songList.isEmpty()) {
            item {
                if (isImporting) {
                    Text("Importing...")
                } else {
                    Text("No songs to display")
                }
            }
        }
        else {
            items(songList) { song ->
                Button(onClick = {
                    songViewModel.selectSong(song)
                    onNavigateToSong()
                })  {
                    Text(song.songTitle)
                }
            }
        }
    }
}

@Composable
fun DisplayImportSongButton(onImportSong: () -> Unit, isImporting: Boolean) {
    Button(onClick = onImportSong, enabled = !isImporting) {
        Text("Import Song")
    }
}