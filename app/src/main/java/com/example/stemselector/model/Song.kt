package com.example.stemselector.model

import kotlinx.serialization.Serializable

@Serializable
data class Song(val songTitle: String, val stemPaths: List<String>)