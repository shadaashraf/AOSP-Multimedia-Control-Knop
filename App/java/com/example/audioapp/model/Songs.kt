package com.example.audioapp.model

import java.io.Serializable

//data class Song(val name: String, val path: String)
data class Songs(
    val title: String,
    val path: String,
    var duration: String = "",
    var albumArtPath: String? = null
): Serializable