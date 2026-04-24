package com.example.stemselector.ui.screens

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.stemselector.model.Song

val songList = emptyList<Song>()

@Composable
fun MainMenuScreen(onNavigateToSong: () -> Unit, onImportSong: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = WindowInsets.systemBars.asPaddingValues()) {
        item { DisplayImportSongButton(onImportSong) }
        item { DisplaySongList() }
    }
}

@Composable
fun DisplaySongList() {
    if (songList.isNotEmpty()) {
        // Display songs
    }
    else {
        Text("No songs to display")
    }
}

@Composable
fun DisplayImportSongButton(onImportSong: () -> Unit) {
    Button(onClick = onImportSong) {
        Text("Import Song")
    }
}