package com.example.stemselector.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.stemselector.model.SongViewModel

@Composable
fun SongScreen(onNavigateToMainMenu: () -> Unit, songViewModel: SongViewModel) {
    val currentSong = songViewModel.currentSongPublic.collectAsState()
    val muteStates = songViewModel.muteStatesPublic.collectAsState()
    val playbackPosition by songViewModel.playbackPositionPublic.collectAsState()

    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            DrawTransportControls(onPlay = songViewModel::play, onPause = songViewModel::pause, onStop = songViewModel::stop)
            DrawBackToMenuButton(onNavigateToMainMenu)
        }
        if (currentSong.value != null) {
            DrawSongScrubber(currentPositionBytes =  playbackPosition, totalBytes = songViewModel.totalSongLengthInBytes, onScrubFinished = { bytes -> songViewModel.seekTo(bytes) })
            DrawStemColumn(currentSong.value!!.stemPaths, muteStates.value,  songViewModel::toggleMute)
        }
    }
}

@Composable
fun DrawStemColumn(stemPaths: List<String>, muteStates: Map<String, Boolean>, onToggle: (String) -> Unit) {
    val count = stemPaths.size
    val colours = List(count) { index -> Color.hsv(index * (360f / count), 1f, 1f) }
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.verticalScroll(scrollState)) {
        for ((stemPath, colour) in stemPaths.zip(colours)) {
            val isMuted = muteStates[stemPath] ?: false
            val displayName = stemPath.substringAfterLast("/").substringBeforeLast(".")
            DrawStemButton(colour, isMuted, displayName, onClick = { onToggle(stemPath) })
        }
    }
}

@Composable
fun DrawStemButton(colour: Color, isMuted: Boolean, displayName: String, onClick: () -> Unit) {
    val displayColour = if (isMuted) Color.Gray else colour
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = displayColour),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(displayName)
    }
}

@Composable
fun DrawTransportControls(onPlay: () -> Unit, onPause: () -> Unit, onStop: () -> Unit) {
    Row {
        Button(onClick = onPlay) {
            Text("Play")
        }
        Button(onClick = onPause) {
            Text("Pause")
        }
        Button(onClick = onStop) {
            Text("Stop")
        }
    }
}

@Composable
fun DrawSongScrubber(currentPositionBytes: Long, totalBytes: Long, onScrubFinished: (Long) -> Unit) {
    var sliderPosition by remember { mutableFloatStateOf(currentPositionBytes.toFloat() / totalBytes) }
    var isDragging by remember { mutableStateOf(false) }

    Slider(
        value = if (isDragging) sliderPosition else (currentPositionBytes.toFloat() / totalBytes),
        onValueChange = { newFraction -> isDragging = true; sliderPosition = newFraction },
        onValueChangeFinished = { isDragging = false; val positionBytes = (sliderPosition * totalBytes).toLong(); onScrubFinished(positionBytes) },
        valueRange = 0f..1f
    )
}

@Composable
fun DrawBackToMenuButton(onNavigateToMainMenu: () -> Unit) {
    Button(onClick = onNavigateToMainMenu) {
        Text("Back to Main Menu")
    }
}