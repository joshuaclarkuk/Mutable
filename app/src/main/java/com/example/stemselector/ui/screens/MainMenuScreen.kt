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
    val songList by songViewModel.songListPublic.collectAsState()
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = WindowInsets.systemBars.asPaddingValues()) {
        item { DisplayImportSongButton(onImportSong) }
        if (songList.isEmpty()) {
            item { Text("No songs to display")}
        }
        else {
            items(songList) { song ->
                Text(song.songTitle)
            }
        }
    }
}

@Composable
fun DisplayImportSongButton(onImportSong: () -> Unit) {
    Button(onClick = onImportSong) {
        Text("Import Song")
    }
}