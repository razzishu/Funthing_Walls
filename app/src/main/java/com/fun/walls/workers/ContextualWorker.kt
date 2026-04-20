package com.`fun`.walls.workers

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.`fun`.walls.data.SettingsManager
import com.`fun`.walls.data.WallpaperRepository
import com.`fun`.walls.models.Wallpaper
import com.`fun`.walls.utils.WallpaperSetter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import kotlin.math.roundToInt

class ContextualWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val repository = WallpaperRepository()
    private val settingsManager = SettingsManager(context)

    override suspend fun doWork(): Result {
        val workerType = inputData.getString("worker_type")
        val pKey = settingsManager.pexelsApiKey.firstOrNull()?.ifBlank { null }
        val wKey = settingsManager.wallhavenApiKey.firstOrNull()?.ifBlank { null }

        try {
            if (workerType == "auto_changer") {
                val isEnabled = settingsManager.autoChangeEnabled.firstOrNull() ?: false
                val isExploreActive = settingsManager.isExploreWallpaperActive.firstOrNull() ?: false

                // ONLY runs if the user hasn't overridden it with a local gallery image
                if (isEnabled && isExploreActive) {
                    runExploreAutoChanger(pKey, wKey)
                } else {
                    Log.d("AutoChanger", "Skipped: AutoChanger is disabled or a Local Wallpaper is active.")
                }
                return Result.success()
            }

            val mode = settingsManager.automationMode.firstOrNull() ?: "None"
            if (mode == "None") return Result.success()

            when (mode) {
                "Time" -> runTimeBasedEngine(pKey, wKey)
                "Weather" -> runLiveWeatherSync()
            }
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private suspend fun runExploreAutoChanger(pKey: String?, wKey: String?) {
        val query = settingsManager.autoChangeQuery.firstOrNull() ?: "Curated"

        // NEW: Read the exact screen target setting (1 = Home, 2 = Lock, 3 = Both)
        val targetScreen = settingsManager.autoChangeTarget.firstOrNull() ?: 3

        Log.d("AutoChanger", "Fetching new wallpaper for category: $query, Target Screen: $targetScreen")

        val wallUrl = fetchBestWallpaper(query, pKey, wKey)

        if (wallUrl != null) {
            val w = context.resources.displayMetrics.widthPixels.toFloat()
            val h = context.resources.displayMetrics.heightPixels.toFloat()

            // Apply to Home Screen if 1 or 3
            if (targetScreen == 1 || targetScreen == 3) {
                WallpaperSetter.applyCustomWallpaper(context, wallUrl, null, WallpaperManager.FLAG_SYSTEM, 1f, 0f, 0f, 0f, 0f, 0f, w, h, "None", 1f, false, true)
                delay(1000)
            }

            // Apply to Lock Screen if 2 or 3
            if (targetScreen == 2 || targetScreen == 3) {
                WallpaperSetter.applyCustomWallpaper(context, wallUrl, null, WallpaperManager.FLAG_LOCK, 1f, 0f, 0f, 0f, 0f, 0f, w, h, "None", 1f, true, true)
            }
        }
    }

    private suspend fun fetchBestWallpaper(prompt: String, pKey: String?, wKey: String?): String? {
        val combinedWalls = mutableListOf<Wallpaper>()
        val randomPage = (1..20).random()

        if (pKey != null) {
            try { combinedWalls.addAll(repository.getPexelsWallpapers(pKey, prompt, randomPage)) } catch (_: Exception) { }
        }
        if (combinedWalls.isEmpty()) {
            try { combinedWalls.addAll(repository.getWallhavenWallpapers(wKey, prompt, randomPage)) } catch (_: Exception) { }
        }
        return if (combinedWalls.isNotEmpty()) combinedWalls.random().imageUrl else null
    }

    private suspend fun runLiveWeatherSync() {
        try {
            val lat = 11.0168
            val lon = 76.9558
            val locationName = "Coimbatore"

            val urlString = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val jsonObject = JSONObject(response)
                val currentWeather = jsonObject.getJSONObject("current_weather")

                val weatherCode = currentWeather.getInt("weathercode")
                val windSpeed = currentWeather.getDouble("windspeed")
                val temperature = currentWeather.getDouble("temperature").roundToInt()

                val visualState = when (weatherCode) {
                    0 -> "Clear"
                    1 -> "Partly Cloudy"
                    2 -> if (windSpeed > 20.0) "Windy" else "Cloudy"
                    3 -> "Overcast"
                    45, 48 -> "Fog"
                    51, 53, 55 -> "Drizzle"
                    56, 57 -> "Haze"
                    61, 63 -> "Rain"
                    65, 66, 67 -> "Heavy Rain"
                    71, 73 -> "Snow"
                    75, 77, 85, 86 -> "Blizzard"
                    80, 81, 82 -> "Heavy Rain"
                    95 -> "Thunderstorm"
                    96, 99 -> "Hail"
                    else -> "Clear"
                }

                settingsManager.saveWeatherCondition(visualState)
                settingsManager.saveWeatherTemp("$temperature°C")
                settingsManager.saveWeatherLoc(locationName)
            }
        } catch (e: Exception) { Log.e("WeatherAPI", "Error: ${e.message}") }
    }

    private suspend fun runTimeBasedEngine(pKey: String?, wKey: String?) {
        val wm = WallpaperManager.getInstance(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val currentId = wm.getWallpaperId(WallpaperManager.FLAG_SYSTEM)
            val savedId = settingsManager.lastWallpaperId.firstOrNull() ?: -1
            if (savedId != -1 && currentId > 0 && currentId != savedId) {
                if (settingsManager.automationMode.firstOrNull() == "Time") {
                    settingsManager.saveAutomationMode("None")
                    androidx.work.WorkManager.getInstance(context).cancelAllWorkByTag("auto_wallpaper")
                    return
                }
            }
        }
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val prompt = when (hour) {
            0 -> listOf("midnight space").random()
            6 -> listOf("sunrise landscape").random()
            12 -> listOf("high noon sky").random()
            18 -> listOf("sunset city").random()
            else -> listOf("minimal clean architecture").random()
        }
        val (colorTemp, dimAmount) = when (hour) {
            0, 1, 2, 3 -> Pair(0.8f, 0.5f)
            in 7..15 -> Pair(1.0f, 0.0f)
            18 -> Pair(1.3f, 0.1f)
            else -> Pair(0.8f, 0.4f)
        }

        val wallUrl = fetchBestWallpaper(prompt, pKey, wKey)

        if (wallUrl != null) {
            val w = context.resources.displayMetrics.widthPixels.toFloat(); val h = context.resources.displayMetrics.heightPixels.toFloat()
            WallpaperSetter.applyCustomWallpaper(context, wallUrl, null, WallpaperManager.FLAG_SYSTEM, 1f, 0f, 0f, 0f, 0f, dimAmount, w, h, "None", colorTemp, false, true)
            delay(1000)
            WallpaperSetter.applyCustomWallpaper(context, wallUrl, null, WallpaperManager.FLAG_LOCK, 1f, 0f, 0f, 0f, 0f, dimAmount, w, h, "None", colorTemp, true, true)
            delay(1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) settingsManager.saveLastWallpaperId(wm.getWallpaperId(WallpaperManager.FLAG_SYSTEM))
        }
    }
}