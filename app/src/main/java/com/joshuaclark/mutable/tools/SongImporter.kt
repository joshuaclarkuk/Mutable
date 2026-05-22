package com.joshuaclark.mutable.tools

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.joshuaclark.mutable.model.Song
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

            val wroteAny = decodeStemsToTemp(selectedFolder, tempFolder, onError)
            if (!wroteAny) {
                onError("No supported audio files found")
                return@launch
            }

            val finalFolder = moveTempToFinal(songId, tempFolder, onError) ?: return@launch

            // Sort list of stems alphabetically
            val stemPaths = finalFolder.listFiles()?.sortedBy { it.name.lowercase() } ?.map { it.path } ?: emptyList()

            val songTitle = selectedFolder.name ?: "Unknown"
            onComplete(
                Song(
                    songId,
                    songTitle,
                    uri.toString(),
                    stemPaths
                )
            )
        }
    }

    fun loadSampleSong(onComplete: (Song) -> Unit) {
        val prefs = context.getSharedPreferences("mutable_prefs", Context.MODE_PRIVATE)
        val isLoaded = prefs.getBoolean("sample_loaded", false)
        if (isLoaded) return

        coroutineScope.launch(Dispatchers.IO) {
            val songId = UUID.randomUUID().toString()
            val tempFolder = File(File(context.filesDir, "temp"), songId)
            tempFolder.mkdirs()

            // Copy FLACs from assets to temp folder
            val assetFiles = context.assets.list("sample_song") ?: return@launch
            for (filename in assetFiles) {
                val outFile = File(tempFolder, filename)
                context.assets.open("sample_song/$filename").use { inputStream ->
                    outFile.writeBytes(inputStream.readBytes())
                }
            }

            // Decode each FLAC to PCM, then delete the FLAC copy
            var wroteAny = false
            for (file in tempFolder.listFiles() ?: emptyArray()) {
                val decodedAudio = decodeAudioFile(context, Uri.fromFile(file)) ?: continue
                val stemName = file.nameWithoutExtension
                    .split(" ")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
                File(tempFolder, "$stemName.pcm").writeBytes(decodedAudio)
                file.delete() // Delete the FLAC copy to free space
                wroteAny = true
            }

            if (!wroteAny) return@launch

            // Move temp to final
            val finalFolder = moveTempToFinal(songId, tempFolder) { } ?: return@launch

            val stemPaths = finalFolder.listFiles()?.sortedBy { it.name.lowercase() } ?.map { it.path } ?: emptyList()

            val song = Song(
                songId,
                "Adam Fielding - The Destroyer (Demo)",
                "bundled",
                stemPaths
            )

            prefs.edit().putBoolean("sample_loaded", true).apply()

            // Return song via callback
            onComplete(song)
        }
    }

    private fun decodeStemsToTemp(selectedFolder: DocumentFile, tempFolder: File, onError: (String) -> Unit): Boolean {
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

        // Get all newly decoded pcm files and check they are all the same length
        val pcmFiles = tempFolder.listFiles { file -> file.name.endsWith(".pcm") }
        if (pcmFiles != null && pcmFiles.size > 1) {
            val expectedSize = pcmFiles.first().length()
            for (file in pcmFiles.drop(1)) {
                if (file.length() != expectedSize) {
                    onError("Stems must be the same length")
                    return false
                }
            }
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