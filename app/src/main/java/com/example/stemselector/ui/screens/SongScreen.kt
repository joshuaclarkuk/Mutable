package com.example.stemselector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.stemselector.model.SongViewModel
import com.example.stemselector.ui.theme.CyanDark
import com.example.stemselector.ui.theme.CyanPrimary
import com.example.stemselector.ui.theme.stemBackgroundColours
import com.example.stemselector.ui.theme.stemTextColours

const val roundedBorderAmount: Int = 20
const val borderPadding: Int = 2

@Composable
fun SongScreen(onNavigateToMainMenu: () -> Unit, songViewModel: SongViewModel) {
    val currentSong = songViewModel.currentSongPublic.collectAsState()
    val muteStates = songViewModel.muteStatesPublic.collectAsState()
    val playbackPosition by songViewModel.playbackPositionPublic.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            DrawTransportControls(onPlay = songViewModel::play, onPause = songViewModel::pause, onStop = songViewModel::stop)
        }
        if (currentSong.value != null) {
            DrawSongScrubber(currentPositionBytes =  playbackPosition, totalBytes = songViewModel.totalSongLengthInBytes, onScrubFinished = { bytes -> songViewModel.seekTo(bytes) })
            DrawStemColumn(currentSong.value!!.stemPaths, muteStates.value,  songViewModel::toggleMute)
        }
        DrawBackToMenuButton(onNavigateToMainMenu = {
            songViewModel.teardown()
            onNavigateToMainMenu()
        })
    }
}

@Composable
fun DrawStemColumn(stemPaths: List<String>, muteStates: Map<String, Boolean>, onToggle: (String) -> Unit) {
    val count = stemPaths.size
    val backgroundColours = List(count) { index -> stemBackgroundColours[index % stemBackgroundColours.size] }
    val textColours = List(count) { index -> stemTextColours[index % stemTextColours.size] }
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.verticalScroll(scrollState)) {
        for (index in stemPaths.indices) {
            val stemPath = stemPaths[index]
            val backgroundColour = backgroundColours[index]
            val textColour = textColours[index]
            val isMuted = muteStates[stemPath] ?: false
            val displayName = stemPath.substringAfterLast("/").substringBeforeLast(".")
            DrawStemButton(backgroundColour, textColour, isMuted, displayName, onClick = { onToggle(stemPath) })
        }
    }
}

@Composable
fun DrawStemButton(backgroundColour: Color, textColour: Color, isMuted: Boolean, displayName: String, onClick: () -> Unit) {
    val displayColourBackground = if (isMuted) backgroundColour.copy(alpha = 0.35f) else backgroundColour
    val displayColourText = if (isMuted) textColour.copy(alpha = 0.7f) else textColour
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = displayColourBackground, contentColor = displayColourText),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(displayName)
    }
}

@Composable
fun DrawTransportControls(onPlay: () -> Unit, onPause: () -> Unit, onStop: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(shape = RoundedCornerShape(roundedBorderAmount.dp)).background(CyanPrimary).padding(borderPadding.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Button(onClick = onPlay, colors = ButtonDefaults.buttonColors(contentColor = CyanDark)
        ){
            Text("Play")
        }
        Button(onClick = onPause, colors = ButtonDefaults.buttonColors(contentColor = CyanDark)
        ){
            Text("Pause")
        }
        Button(onClick = onStop, colors = ButtonDefaults.buttonColors(contentColor = CyanDark)
        ){
            Text("Stop")
        }
    }
}

@Composable
fun DrawSongScrubber(currentPositionBytes: Long, totalBytes: Long, onScrubFinished: (Long) -> Unit) {
    val fraction = if (totalBytes > 0) currentPositionBytes.toFloat() / totalBytes else 0f
    var sliderPosition by remember { mutableFloatStateOf(fraction) }
    var isDragging by remember { mutableStateOf(false) }

    Slider(
        value = if (isDragging) sliderPosition else fraction,
        onValueChange = { newFraction -> isDragging = true; sliderPosition = newFraction },
        onValueChangeFinished = { isDragging = false; val positionBytes = (sliderPosition * totalBytes).toLong(); onScrubFinished(positionBytes) },
        valueRange = 0f..1f,
        colors = SliderDefaults.colors(
            thumbColor = CyanPrimary,
            activeTrackColor = CyanPrimary
        )
    )
}

@Composable
fun DrawBackToMenuButton(onNavigateToMainMenu: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(shape = RoundedCornerShape(roundedBorderAmount.dp)).background(CyanPrimary).padding(borderPadding.dp), horizontalArrangement = Arrangement.End) {
        Button(onClick = onNavigateToMainMenu, colors = ButtonDefaults.buttonColors(contentColor = CyanDark)) {
            Text("Back to Main Menu")
        }
    }
}