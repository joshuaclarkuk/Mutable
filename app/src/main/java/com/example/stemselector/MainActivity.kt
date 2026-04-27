package com.example.stemselector

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.stemselector.model.Song
import com.example.stemselector.model.SongViewModel
import com.example.stemselector.navigation.AppNavigation
import com.example.stemselector.ui.theme.StemSelectorTheme
import com.example.stemselector.tools.decodeAudioFile
import java.io.File

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
                        val selectedFolder: DocumentFile? = DocumentFile.fromTreeUri(this@MainActivity, uri)
                        if (selectedFolder != null) {
                            val stemPaths = mutableListOf<String>()
                            val fileList: Array<DocumentFile> = selectedFolder.listFiles()
                            for (file in fileList) {
                                val extension: String? = file.name?.split(".")?.last()?.lowercase()
                                if (extension == "wav" || extension == "ogg" || extension == "mp3" || extension == "flac") {
                                    val decodedAudio: ByteArray? = decodeAudioFile(this, file.uri)
                                    if (decodedAudio != null) {
                                        Log.d("MainActivity", "Decoded: ${file.name}, Size: ${decodedAudio.size} bytes")
                                        val outputFile = File(this@MainActivity.filesDir, "${file.name?.split(".")?.first()?.lowercase()}.pcm")
                                        outputFile.writeBytes(decodedAudio)
                                        stemPaths.add(outputFile.path)
                                    }
                                    else {
                                        Log.d("MainActivity", "Failed to decode ${file.name}")
                                    }
                                }
                                else {
                                    Log.d("MainActivity", "Skipped $extension file")
                                }
                            }
                            val songTitle: String = selectedFolder.name ?: "Unknown"
                            val song = Song(songTitle, stemPaths)
                            songViewModel.addSong(song)
                        }
                    }
                }
                AppNavigation(navHostController, songViewModel, { launcher.launch(null)})
            }
        }
    }
}