package com.`fun`.walls.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

class SettingsManager(private val context: Context) {
    companion object {
        val PEXELS_KEY = stringPreferencesKey("pexels_api_key")
        val WALLHAVEN_KEY = stringPreferencesKey("wallhaven_api_key")
        val AUTOMATION_MODE = stringPreferencesKey("automation_mode")
        val MUSIC_APP = stringPreferencesKey("music_app_target")
        val LAST_WALL_ID = intPreferencesKey("last_wall_id")
        val WEATHER_CONDITION = stringPreferencesKey("weather_condition")

        val WEATHER_TEMP = stringPreferencesKey("weather_temp")
        val WEATHER_LOC = stringPreferencesKey("weather_loc")

        val AUTO_CHANGE_ENABLED = booleanPreferencesKey("auto_change_enabled")
        val AUTO_CHANGE_INTERVAL = intPreferencesKey("auto_change_interval")
        val IS_EXPLORE_WALLPAPER_ACTIVE = booleanPreferencesKey("is_explore_wallpaper_active")
        val AUTO_CHANGE_QUERY = stringPreferencesKey("auto_change_query")

        // NEW: Stores the Target Screen (1 = Home, 2 = Lock, 3 = Both)
        val AUTO_CHANGE_TARGET = intPreferencesKey("auto_change_target")

        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    }

    val pexelsApiKey: Flow<String?> = context.dataStore.data.map { it[PEXELS_KEY] }
    val wallhavenApiKey: Flow<String?> = context.dataStore.data.map { it[WALLHAVEN_KEY] }
    val automationMode: Flow<String> = context.dataStore.data.map { it[AUTOMATION_MODE] ?: "None" }
    val musicAppFlow: Flow<String> = context.dataStore.data.map { it[MUSIC_APP] ?: "Any Music App" }
    val lastWallpaperId: Flow<Int> = context.dataStore.data.map { it[LAST_WALL_ID] ?: -1 }
    val weatherCondition: Flow<String> = context.dataStore.data.map { it[WEATHER_CONDITION] ?: "Clear" }

    val weatherTemp: Flow<String> = context.dataStore.data.map { it[WEATHER_TEMP] ?: "--°C" }
    val weatherLoc: Flow<String> = context.dataStore.data.map { it[WEATHER_LOC] ?: "Unknown" }

    val autoChangeEnabled: Flow<Boolean> = context.dataStore.data.map { it[AUTO_CHANGE_ENABLED] ?: false }
    val autoChangeInterval: Flow<Int> = context.dataStore.data.map { it[AUTO_CHANGE_INTERVAL] ?: 60 }
    val isExploreWallpaperActive: Flow<Boolean> = context.dataStore.data.map { it[IS_EXPLORE_WALLPAPER_ACTIVE] ?: false }
    val autoChangeQuery: Flow<String> = context.dataStore.data.map { it[AUTO_CHANGE_QUERY] ?: "Curated" }

    // NEW: Expose Target Screen
    val autoChangeTarget: Flow<Int> = context.dataStore.data.map { it[AUTO_CHANGE_TARGET] ?: 3 }

    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { it[HAS_COMPLETED_ONBOARDING] ?: false }

    suspend fun savePexelsKey(key: String) { context.dataStore.edit { it[PEXELS_KEY] = key } }
    suspend fun saveWallhavenKey(key: String) { context.dataStore.edit { it[WALLHAVEN_KEY] = key } }
    suspend fun saveAutomationMode(mode: String) { context.dataStore.edit { it[AUTOMATION_MODE] = mode } }
    suspend fun saveMusicApp(app: String) { context.dataStore.edit { it[MUSIC_APP] = app } }
    suspend fun saveLastWallpaperId(id: Int) { context.dataStore.edit { it[LAST_WALL_ID] = id } }
    suspend fun saveWeatherCondition(condition: String) { context.dataStore.edit { it[WEATHER_CONDITION] = condition } }

    suspend fun saveWeatherTemp(temp: String) { context.dataStore.edit { it[WEATHER_TEMP] = temp } }
    suspend fun saveWeatherLoc(loc: String) { context.dataStore.edit { it[WEATHER_LOC] = loc } }

    suspend fun saveAutoChangeEnabled(enabled: Boolean) { context.dataStore.edit { it[AUTO_CHANGE_ENABLED] = enabled } }
    suspend fun saveAutoChangeInterval(minutes: Int) { context.dataStore.edit { it[AUTO_CHANGE_INTERVAL] = minutes } }
    suspend fun saveIsExploreWallpaperActive(isActive: Boolean) { context.dataStore.edit { it[IS_EXPLORE_WALLPAPER_ACTIVE] = isActive } }
    suspend fun saveAutoChangeQuery(query: String) { context.dataStore.edit { it[AUTO_CHANGE_QUERY] = query } }

    // NEW: Save Target Screen
    suspend fun saveAutoChangeTarget(target: Int) { context.dataStore.edit { it[AUTO_CHANGE_TARGET] = target } }

    suspend fun saveHasCompletedOnboarding(completed: Boolean) { context.dataStore.edit { it[HAS_COMPLETED_ONBOARDING] = completed } }
}