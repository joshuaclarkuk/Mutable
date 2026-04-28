package com.example.stemselector.tools

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.stemselector.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class SongImporter(val context: Context, val coroutineScope: CoroutineScope) {
    fun importSong(uri: Uri, onComplete: (Song) -> Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val selectedFolder: DocumentFile? = DocumentFile.fromTreeUri(context, uri)
            if (selectedFolder != null) {
                val stemPaths = mutableListOf<String>()
                val fileList: Array<DocumentFile> = selectedFolder.listFiles()
                for (file in fileList) {
                    val extension: String? = file.name?.split(".")?.last()?.lowercase()
                    if (extension == "wav" || extension == "ogg" || extension == "mp3" || extension == "flac") {
                        val decodedAudio: ByteArray? = decodeAudioFile(context, file.uri)
                        if (decodedAudio != null) {
                            Log.d(
                                "MainActivity",
                                "Decoded: ${file.name}, Size: ${decodedAudio.size} bytes"
                            )
                            val outputFile = File(
                                context.filesDir,
                                "${file.name?.split(".")?.first()?.lowercase()}.pcm"
                            )
                            outputFile.writeBytes(decodedAudio)
                            stemPaths.add(outputFile.path)
                        } else {
                            Log.d("SongImporter", "Failed to decode ${file.name}")
                        }
                    } else {
                        Log.d("SongImporter", "Skipped $extension file")
                    }
                }
                val songTitle: String = selectedFolder.name ?: "Unknown"
                val song = Song(songTitle, stemPaths)
                onComplete(song)
            }
        }
    }
}