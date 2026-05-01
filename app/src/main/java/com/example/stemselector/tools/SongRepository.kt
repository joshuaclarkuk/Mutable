package com.example.stemselector.tools

import android.content.Context
import android.util.Log
import com.example.stemselector.model.Song
import kotlinx.serialization.json.Json
import java.io.File

fun loadSongs(context: Context) : List<Song> {
    val file = File(context.filesDir, "songs.json")
    if (!file.exists()) {
        return emptyList()
    }
    try {
        val jsonText = file.readText()
        return Json.decodeFromString<List<Song>>(jsonText)
    }
    catch (exception: Exception) {
        Log.d("SongRepository", "Load song error: ${exception}")
        return emptyList()
    }
}

fun saveSongs(context: Context, songList: List<Song>) {
    val jsonText = Json.encodeToString(songList)
    val file = File(context.filesDir, "songs.json")
    file.writeText(jsonText)
}