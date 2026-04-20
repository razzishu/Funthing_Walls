package com.`fun`.walls.data

import retrofit2.http.GET
import retrofit2.http.Query

data class WallhavenResponse(val data: List<WallhavenWallpaper>)
data class WallhavenWallpaper(val id: String, val path: String, val thumbs: WallhavenThumbs)
data class WallhavenThumbs(val small: String, val large: String)

interface WallhavenApi {
    @GET("api/v1/search")
    suspend fun searchWallpapers(
        @Query("apikey") apiKey: String?,
        @Query("q") query: String? = null,
        // FIX: Re-added the default value so the compiler stops panicking
        @Query("sorting") sorting: String = "toplist",
        @Query("purity") purity: String = "100",
        @Query("page") page: Int = 1
    ): WallhavenResponse
}