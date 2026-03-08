package com.aesthetic.funthingwalls

import android.app.WallpaperManager
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FunthingWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // 1. Get the user's saved API key and their last searched vibe
        val prefs = applicationContext.getSharedPreferences("FunthingPrefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("API_KEY", "") ?: ""
        val savedVibe = prefs.getString("LAST_VIBE", "Nature") ?: "Nature"

        if (apiKey.isEmpty()) return Result.failure()

        // 2. Setup the internet connection
        val apiService = Retrofit.Builder()
            .baseUrl("https://api.pexels.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PexelsApiService::class.java)

        return try {
            // 3. Pick a random page so the user gets different wallpapers every time!
            val randomPage = (1..50).random()

            // 4. Fetch the Sister Pair
            val darkRes = apiService.searchWallpapers(apiKey, savedVibe, "black", 1, randomPage)
            val lightRes = apiService.searchWallpapers(apiKey, savedVibe, "white", 1, randomPage)

            if (darkRes.isSuccessful && lightRes.isSuccessful) {
                val darkUrl = darkRes.body()?.photos?.firstOrNull()?.src?.large2x
                val lightUrl = lightRes.body()?.photos?.firstOrNull()?.src?.large2x

                if (darkUrl != null && lightUrl != null) {
                    applyBackgroundWallpapers(darkUrl, lightUrl)
                    Result.success() // Tell Android the job is done!
                } else {
                    Result.retry()
                }
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun applyBackgroundWallpapers(darkUrl: String, lightUrl: String) {
        withContext(Dispatchers.IO) {
            val wm = WallpaperManager.getInstance(applicationContext)
            val loader = ImageLoader(applicationContext)

            // Download Dark
            val darkReq = ImageRequest.Builder(applicationContext).data(darkUrl).build()
            val darkResult = (loader.execute(darkReq) as? SuccessResult)?.drawable
            val darkBitmap = (darkResult as? BitmapDrawable)?.bitmap

            // Download Light
            val lightReq = ImageRequest.Builder(applicationContext).data(lightUrl).build()
            val lightResult = (loader.execute(lightReq) as? SuccessResult)?.drawable
            val lightBitmap = (lightResult as? BitmapDrawable)?.bitmap

            // Apply Both silently
            if (darkBitmap != null && lightBitmap != null) {
                wm.setBitmap(darkBitmap, null, true, WallpaperManager.FLAG_LOCK)
                wm.setBitmap(lightBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
            }
        }
    }
}