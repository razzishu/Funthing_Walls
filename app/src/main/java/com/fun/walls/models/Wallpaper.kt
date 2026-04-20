package com.`fun`.walls.models

data class Wallpaper(
    val id: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val source: String,
    val credit: String // NEW: Holds the photographer or community name
)