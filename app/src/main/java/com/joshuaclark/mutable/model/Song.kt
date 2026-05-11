package com.joshuaclark.mutable.model

import kotlinx.serialization.Serializable

@Serializable
data class Song(val songID: String, val songTitle: String, val sourceFolderUri: String, val stemPaths: List<String>)