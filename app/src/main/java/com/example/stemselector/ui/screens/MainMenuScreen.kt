package com.example.stemselector.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.stemselector.model.Song
import com.example.stemselector.model.SongViewModel

@Composable
fun MainMenuScreen(onNavigateToSong: () -> Unit, onImportSong: () -> Unit, songViewModel: SongViewModel) {
    val isImporting by songViewModel.isImportingPublic.collectAsState()
    val importError by songViewModel.importErrorPublic.collectAsState()
    val songList by songViewModel.songListPublic.collectAsState()

    var songPendingDeletion by remember { mutableStateOf<Song?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = WindowInsets.systemBars.asPaddingValues()) {
        item { DisplayImportSongButton(onImportSong, isImporting) }
        if (importError != null) {
            item {
                Column {
                    Text(importError.toString())
                    Button(onClick = { songViewModel.clearError() }) {
                        Text("Dismiss")
                    }
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
        // Display successfully created song
        else {
            items(songList) { song ->
                DisplaySongRow(song = song,
                    onNavigateToSong = { songViewModel.selectSong(song); onNavigateToSong() },
                    onDeleteRequest = { songPendingDeletion = song })
            }
        }
    }

    // Display confirmation dialogue when delete is selected
    val songToDelete = songPendingDeletion
    if (songToDelete != null) {
        DisplayConfirmDeleteDialogue(songToDelete,
            onDeleteSong = { songViewModel.deleteSong(songToDelete); songPendingDeletion = null },
            onCancelDeletion = { songPendingDeletion = null },
            "Delete Song?",
            "Are you sure you wish to delete this song?",
            null)
    }
}

@Composable
fun DisplayImportSongButton(onImportSong: () -> Unit, isImporting: Boolean) {
    Button(onClick = onImportSong, enabled = !isImporting) {
        Text("Import Song")
    }
}

@Composable
fun DisplaySongRow(song: Song, onNavigateToSong: () -> Unit, onDeleteRequest: () -> Unit) {
    Row {
        Button(onClick = onNavigateToSong) {
            Text(song.songTitle)
        }
        Button(onClick = { onDeleteRequest() }) {
            Text("Delete")
        }
    }
}

@Composable
fun DisplayConfirmDeleteDialogue(pendingSong: Song, onDeleteSong: () -> Unit, onCancelDeletion: () -> Unit, dialogueTitle: String, dialogueText: String, icon: ImageVector?) {
    AlertDialog(
        icon = { if (icon != null) { Icon(icon, contentDescription = "Example icon") } },
        title = { (Text(dialogueTitle)) },
        text = { Text(text = dialogueText) },
        onDismissRequest = onCancelDeletion,
        confirmButton = { TextButton(onClick = { onDeleteSong() }) { Text("Confirm")} },
        dismissButton = { TextButton(onClick = { onCancelDeletion() }) { Text("Cancel") }}
    )
}