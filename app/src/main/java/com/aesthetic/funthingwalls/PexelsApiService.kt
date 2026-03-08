package com.aesthetic.funthingwalls

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface PexelsApiService {

    // Normal Image Search
    @GET("v1/search")
    suspend fun searchWallpapers(
        @Header("Authorization") apiKey: String,
        @Query("query") query: String,
        @Query("color") color: String? = null,
        @Query("per_page") perPage: Int = 1,
        @Query("page") page: Int = 1
    ): Response<PexelsResponse>

    // Infinite Curated Images
    @GET("v1/curated")
    suspend fun getCuratedWallpapers(
        @Header("Authorization") apiKey: String,
        @Query("per_page") perPage: Int = 40,
        @Query("page") page: Int = 1
    ): Response<PexelsResponse>

    // NEW: Video Search!
    @GET("videos/search")
    suspend fun searchVideos(
        @Header("Authorization") apiKey: String,
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 15,
        @Query("page") page: Int = 1
    ): Response<PexelsVideoResponse>

    // NEW: Infinite Popular Videos
    @GET("videos/popular")
    suspend fun getPopularVideos(
        @Header("Authorization") apiKey: String,
        @Query("per_page") perPage: Int = 15,
        @Query("page") page: Int = 1
    ): Response<PexelsVideoResponse>
}