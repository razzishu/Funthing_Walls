package com.`fun`.walls.data

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

data class PexelsResponse(val photos: List<PexelsPhoto>)
data class PexelsPhoto(val id: Int, val src: PexelsSrc, val photographer: String)
// FIX: Added 'portrait' and 'large' for High-Quality Grid Images
data class PexelsSrc(val original: String, val medium: String, val portrait: String, val large: String)

interface PexelsApi {
    @GET("v1/search")
    suspend fun searchWallpapers(
        @Header("Authorization") apiKey: String,
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1
    ): PexelsResponse

    @GET("v1/curated")
    suspend fun getCuratedWallpapers(
        @Header("Authorization") apiKey: String,
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1
    ): PexelsResponse
}