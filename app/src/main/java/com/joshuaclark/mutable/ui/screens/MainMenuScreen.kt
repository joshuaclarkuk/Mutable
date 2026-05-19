package com.joshuaclark.mutable.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.joshuaclark.mutable.model.Song
import com.joshuaclark.mutable.model.SongViewModel
import com.joshuaclark.mutable.ui.theme.DeleteRed

@Composable
fun MainMenuScreen(onNavigateToSong: () -> Unit, onImportSong: () -> Unit, songViewModel: SongViewModel) {
    val isImporting by songViewModel.isImportingPublic.collectAsState()
    val importError by songViewModel.importErrorPublic.collectAsState()
    val songList by songViewModel.songListPublic.collectAsState()

    var songPendingDeletion by remember { mutableStateOf<com.joshuaclark.mutable.model.Song?>(null) }

    Box(Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.background).padding(16.dp).systemBarsPadding()) {
        Column {
            // Title behaviour
            Text(
                text = "Mutable",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                style = MaterialTheme.typography.headlineLarge
            )

            // Song list behaviour
            LazyColumn(Modifier.heightIn(max = (LocalConfiguration.current.screenHeightDp / 2).dp)) {
                if (isImporting) {
                    item {
                        Text(text = "Importing...", color = MaterialTheme.colorScheme.onBackground)
                    }
                } else {
                    if (songList.isEmpty()) {
                        item {
                            Text(text = "No songs to display", color = MaterialTheme.colorScheme.onBackground)
                        }
                    } else {
                        items(songList) { song ->
                            DisplaySongRow(
                                song = song,
                                onNavigateToSong = { songViewModel.selectSong(song); onNavigateToSong() },
                                onDeleteRequest = { songPendingDeletion = song })
                        }
                    }
                }
            }

            // Import error behaviour
            if (importError != null) {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        importError.toString(),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { songViewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            }
            else {
                // Import button behaviour
                DisplayImportSongButton(onImportSong, isImporting)
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
    }
}

@Composable
fun DisplayImportSongButton(onImportSong: () -> Unit, isImporting: Boolean) {
    Button(onClick = onImportSong, enabled = !isImporting) {
        Text("Import Song")
    }
}

@Composable
fun DisplaySongRow(song: com.joshuaclark.mutable.model.Song, onNavigateToSong: () -> Unit, onDeleteRequest: () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        Text(text = song.songTitle, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.clickable(onClick = onNavigateToSong).weight(1f))
        IconButton(onClick = { onDeleteRequest() }) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete song", tint = com.joshuaclark.mutable.ui.theme.DeleteRed)
        }
    }
}

@Composable
fun DisplayConfirmDeleteDialogue(pendingSong: com.joshuaclark.mutable.model.Song, onDeleteSong: () -> Unit, onCancelDeletion: () -> Unit, dialogueTitle: String, dialogueText: String, icon: ImageVector?) {
    AlertDialog(
        icon = { if (icon != null) { Icon(icon, contentDescription = "Example icon") } },
        title = { (Text(dialogueTitle)) },
        text = { Text(text = dialogueText) },
        onDismissRequest = onCancelDeletion,
        confirmButton = { TextButton(onClick = { onDeleteSong() }) { Text("Confirm")} },
        dismissButton = { TextButton(onClick = { onCancelDeletion() }) { Text("Cancel") }}
    )
}