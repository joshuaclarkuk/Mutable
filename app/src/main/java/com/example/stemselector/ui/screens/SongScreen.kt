package com.example.stemselector.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.stemselector.model.SongViewModel
import com.example.stemselector.ui.components.StemButton

@Composable
fun SongScreen(onNavigateToMainMenu: () -> Unit, songViewModel: SongViewModel) {
    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        DrawBackToMenuButton(onNavigateToMainMenu)
        DrawStemColumn()
    }
}

@Composable
fun DrawStemColumn()
{
    val colours = List(32) {
            index -> Color.hsv(index * (360f / 32f),
        1f,
        1f)
    }
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.verticalScroll(scrollState))
    {
        for (color in colours) {
            StemButton(color)
        }
    }
}

@Composable
fun DrawBackToMenuButton(onNavigateToMainMenu: () -> Unit) {
    Button(onClick = onNavigateToMainMenu) {
        Text("Back to Main Menu")
    }
}