package com.aesthetic.funthingwalls

// --- IMAGE MODELS ---
data class PexelsResponse(
    val photos: List<Photo>
)

data class Photo(
    val src: PhotoSrc
)

data class PhotoSrc(
    val original: String,
    val large2x: String
)

// --- VIDEO MODELS ---
data class PexelsVideoResponse(
    val videos: List<PexelsVideo>
)

data class PexelsVideo(
    val image: String, // NEW: The thumbnail image!
    val video_files: List<VideoFile>
)

data class VideoFile(
    val link: String,
    val quality: String
)