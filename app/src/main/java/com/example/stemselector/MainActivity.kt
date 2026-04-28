package com.example.stemselector

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.stemselector.model.SongViewModel
import com.example.stemselector.navigation.AppNavigation
import com.example.stemselector.ui.theme.StemSelectorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StemSelectorTheme {
                val navHostController: NavHostController = rememberNavController()
                val songViewModel: SongViewModel = viewModel()

                val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocumentTree()) { uri ->
                    Log.d("MainActivity", "Folder URI: $uri")
                    if (uri != null) {
                        songViewModel.importSong(uri)
                    }
                }
                AppNavigation(navHostController, songViewModel, { launcher.launch(null)})
            }
        }
    }
}