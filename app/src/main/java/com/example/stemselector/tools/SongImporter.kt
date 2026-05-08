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
import java.util.UUID

class SongImporter(val context: Context, val coroutineScope: CoroutineScope) {

    fun importSong(uri: Uri, onError: (String) -> Unit, onComplete: (Song) -> Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val selectedFolder = DocumentFile.fromTreeUri(context, uri)
            if (selectedFolder == null) {
                onError("Couldn't open folder")
                return@launch
            }

            val songId = UUID.randomUUID().toString()
            val tempFolder = File(File(context.filesDir, "temp"), songId)
            tempFolder.mkdirs()

            val wroteAny = decodeStemsToTemp(selectedFolder, tempFolder)
            if (!wroteAny) {
                onError("No supported audio files found")
                return@launch
            }

            val finalFolder = moveTempToFinal(songId, tempFolder, onError) ?: return@launch

            val stemPaths = finalFolder.listFiles()?.map { it.path } ?: emptyList()
            val songTitle = selectedFolder.name ?: "Unknown"
            onComplete(Song(songId, songTitle, uri.toString(), stemPaths))
        }
    }

    private fun decodeStemsToTemp(selectedFolder: DocumentFile, tempFolder: File): Boolean {
        var wroteAny = false
        for (file in selectedFolder.listFiles()) {
            val extension = file.name?.split(".")?.last()?.lowercase()
            if (extension !in listOf("wav", "flac", "mp3", "ogg")) {
                Log.d("SongImporter", "Skipped $extension file")
                continue
            }
            val decodedAudio = decodeAudioFile(context, file.uri)
            if (decodedAudio == null) {
                Log.d("SongImporter", "Failed to decode ${file.name}")
                continue
            }
            // Extract stem name from file name and capitalise each first letter
            val stemName = file.name?.split(".")?.first()?.split(" ")?.joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercaseChar() }
            }
            File(tempFolder, "$stemName.pcm").writeBytes(decodedAudio)
            Log.d("SongImporter", "Decoded: ${file.name}, Size: ${decodedAudio.size} bytes")
            wroteAny = true
        }
        return wroteAny
    }

    private fun moveTempToFinal(songId: String, tempFolder: File, onError: (String) -> Unit): File? {
        val finalFolder = File(File(context.filesDir, "songs"), songId)
        finalFolder.parentFile?.mkdirs()
        val moved = tempFolder.renameTo(finalFolder)
        if (!moved || finalFolder.listFiles() == null) {
            onError("Failed to move files to permanent storage")
            return null
        }
        return finalFolder
    }
}