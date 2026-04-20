package com.`fun`.walls.data

import com.`fun`.walls.models.Wallpaper

class WallpaperRepository {
    private val pexelsApi = NetworkEngine.createRetrofit("https://api.pexels.com/").create(PexelsApi::class.java)
    private val wallhavenApi = NetworkEngine.createRetrofit("https://wallhaven.cc/").create(WallhavenApi::class.java)

    private fun getSmartQuery(query: String): String {
        return when (query.lowercase().trim()) {
            "amoled" -> "dark black"
            "nature" -> "landscape"
            "cars" -> "sports car"
            "space" -> "galaxy"
            else -> query
        }
    }

    // --- NEW: API KEY VALIDATORS ---
    suspend fun validatePexelsKey(apiKey: String): Boolean {
        if (apiKey.isBlank()) return false
        return try {
            // Ask for just 1 image. If it succeeds, the key is good!
            pexelsApi.getCuratedWallpapers(apiKey, perPage = 1, page = 1)
            true
        } catch (e: Exception) { false }
    }

    suspend fun validateWallhavenKey(apiKey: String): Boolean {
        if (apiKey.isBlank()) return false
        return try {
            wallhavenApi.searchWallpapers(apiKey = apiKey, page = 1)
            true
        } catch (e: Exception) { false }
    }

    suspend fun getPexelsWallpapers(apiKey: String, query: String, page: Int): List<Wallpaper> {
        return try {
            val cleanQuery = query.replace("#", "").trim()
            val smartQuery = getSmartQuery(cleanQuery)

            val response = if (cleanQuery.equals("Curated", ignoreCase = true)) {
                pexelsApi.getCuratedWallpapers(apiKey = apiKey, page = page)
            } else {
                pexelsApi.searchWallpapers(apiKey = apiKey, query = smartQuery, page = page)
            }

            response.photos.map { photo ->
                Wallpaper(
                    id = photo.id.toString(),
                    imageUrl = photo.src.original,
                    thumbnailUrl = photo.src.large,
                    source = "Pexels",
                    credit = "By ${photo.photographer} on Pexels"
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getWallhavenWallpapers(apiKey: String?, query: String, page: Int): List<Wallpaper> {
        return try {
            val isCurated = query.equals("Curated", ignoreCase = true)
            val smartQuery = if (isCurated) null else getSmartQuery(query.replace("#", ""))
            val smartSorting = if (isCurated) "toplist" else "relevance"

            val response = wallhavenApi.searchWallpapers(
                apiKey = apiKey,
                query = smartQuery,
                sorting = smartSorting,
                page = page
            )

            response.data.map { wall ->
                Wallpaper(
                    id = wall.id,
                    imageUrl = wall.path,
                    thumbnailUrl = wall.thumbs.large,
                    source = "Wallhaven",
                    credit = "Wallhaven Community"
                )
            }
        } catch (e: Exception) { emptyList() }
    }
}