package com.`fun`.walls.ui

import android.app.Application
import android.app.WallpaperManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.`fun`.walls.data.SettingsManager
import com.`fun`.walls.data.WallpaperRepository
import com.`fun`.walls.models.Wallpaper
import com.`fun`.walls.utils.WallpaperSetter
import com.`fun`.walls.workers.ContextualWorker
import com.`fun`.walls.workers.StorageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class CustomizationState(
    val scale: Float = 1f, val offsetX: Float = 0f, val offsetY: Float = 0f,
    val blur: Float = 0f, val grayscale: Float = 0f, val dimAmount: Float = 0f
)

enum class ApiStatus { Blank, Checking, Valid, Invalid }

class WallpaperViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WallpaperRepository()
    private val settingsManager = SettingsManager(application)

    private val _wallpapers = MutableStateFlow<List<Wallpaper>>(emptyList())
    val wallpapers: StateFlow<List<Wallpaper>> = _wallpapers
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _isEngineBooting = MutableStateFlow(false)
    val isEngineBooting: StateFlow<Boolean> = _isEngineBooting
    private val _selectedWallpaper = MutableStateFlow<Wallpaper?>(null)
    val selectedWallpaper: StateFlow<Wallpaper?> = _selectedWallpaper
    private val _applyStatus = MutableStateFlow<String?>(null)
    val applyStatus: StateFlow<String?> = _applyStatus

    val pexelsKeyFlow = settingsManager.pexelsApiKey
    val wallhavenKeyFlow = settingsManager.wallhavenApiKey
    val automationModeFlow = settingsManager.automationMode
    val musicAppFlow = settingsManager.musicAppFlow
    val lastWallpaperIdFlow = settingsManager.lastWallpaperId

    val autoChangeEnabledFlow = settingsManager.autoChangeEnabled
    val autoChangeIntervalFlow = settingsManager.autoChangeInterval

    // NEW: Auto Changer Target Flow
    val autoChangeTargetFlow = settingsManager.autoChangeTarget

    private val _pexelsStatus = MutableStateFlow(ApiStatus.Checking)
    val pexelsStatus: StateFlow<ApiStatus> = _pexelsStatus
    private val _wallhavenStatus = MutableStateFlow(ApiStatus.Checking)
    val wallhavenStatus: StateFlow<ApiStatus> = _wallhavenStatus

    private var currentPage = 1
    private var isFetching = false
    private val _activeCategory = MutableStateFlow("Curated")
    val activeCategory: StateFlow<String> = _activeCategory
    private var activeApiQuery = "Curated"
    private val _homeState = MutableStateFlow(CustomizationState())
    val homeState: StateFlow<CustomizationState> = _homeState
    private val _lockState = MutableStateFlow(CustomizationState())
    val lockState: StateFlow<CustomizationState> = _lockState

    private val _downloadedWallpapers = MutableStateFlow<List<Wallpaper>>(emptyList())
    val downloadedWallpapers: StateFlow<List<Wallpaper>> = _downloadedWallpapers
    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading

    init { validateKeysOnBoot(); fetchWallpapers() }

    private fun validateKeysOnBoot() {
        viewModelScope.launch {
            val pKey = settingsManager.pexelsApiKey.firstOrNull() ?: ""
            val wKey = settingsManager.wallhavenApiKey.firstOrNull() ?: ""
            _pexelsStatus.value = if (pKey.isBlank()) ApiStatus.Blank else { if (repository.validatePexelsKey(pKey)) ApiStatus.Valid else ApiStatus.Invalid }
            _wallhavenStatus.value = if (wKey.isBlank()) ApiStatus.Blank else { if (repository.validateWallhavenKey(wKey)) ApiStatus.Valid else ApiStatus.Invalid }
        }
    }

    fun setAutoChangeSettings(enabled: Boolean, intervalMinutes: Int) {
        viewModelScope.launch {
            settingsManager.saveAutoChangeEnabled(enabled)
            settingsManager.saveAutoChangeInterval(intervalMinutes)

            val workManager = WorkManager.getInstance(getApplication())
            if (enabled) {
                // --- NEW: STRICT BATTERY OPTIMIZATION ---
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true) // Won't run if battery is < 15%
                    .build()

                val safeInterval = maxOf(15, intervalMinutes).toLong()

                val req = PeriodicWorkRequestBuilder<ContextualWorker>(safeInterval, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    // NEW: Allows Android to delay the job slightly to batch network requests
                    .setBackoffCriteria(androidx.work.BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
                    .setInputData(workDataOf("worker_type" to "auto_changer"))
                    .addTag("explore_auto_changer")
                    .build()

                workManager.enqueueUniquePeriodicWork("explore_auto_changer", ExistingPeriodicWorkPolicy.UPDATE, req)
            } else {
                workManager.cancelUniqueWork("explore_auto_changer")
            }
        }
    }

    // NEW: Save the target screen preference
    fun setAutoChangeTarget(target: Int) {
        viewModelScope.launch {
            settingsManager.saveAutoChangeTarget(target)
        }
    }

    fun setAutomationMode(mode: String) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag("auto_wallpaper")
            settingsManager.saveAutomationMode(mode)

            if (mode == "Time" || mode == "Weather") {
                _isEngineBooting.value = true
                settingsManager.saveLastWallpaperId(-1)

                val instantReq = OneTimeWorkRequestBuilder<ContextualWorker>().setInputData(workDataOf("target_screen" to 3)).addTag("auto_wallpaper").build()
                workManager.enqueue(instantReq)

                viewModelScope.launch {
                    try {
                        val workInfo = workManager.getWorkInfoByIdFlow(instantReq.id).filterNotNull().first { it.state.isFinished }
                        _isEngineBooting.value = false
                        if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                            _applyStatus.value = if (mode == "Time") "Ambient Time Engine applied!" else "Live Weather Synced with Reality!"
                        } else {
                            _applyStatus.value = "Failed to fetch data. Check internet connection."
                        }
                    } catch (e: Exception) { _isEngineBooting.value = false }
                }

                if (mode == "Time") {
                    val timeReq = PeriodicWorkRequestBuilder<ContextualWorker>(1, TimeUnit.HOURS).setInputData(workDataOf("target_screen" to 3)).addTag("auto_wallpaper").build()
                    workManager.enqueueUniquePeriodicWork("auto_time", ExistingPeriodicWorkPolicy.UPDATE, timeReq)
                } else if (mode == "Weather") {
                    val weatherReq = PeriodicWorkRequestBuilder<ContextualWorker>(1, TimeUnit.HOURS).setInputData(workDataOf("target_screen" to 3)).addTag("auto_wallpaper").build()
                    workManager.enqueueUniquePeriodicWork("auto_weather", ExistingPeriodicWorkPolicy.UPDATE, weatherReq)
                }
            }
        }
    }

    fun setMusicApp(app: String) { viewModelScope.launch { settingsManager.saveMusicApp(app) } }

    fun selectWallpaper(wallpaper: Wallpaper) {
        if (_selectedWallpaper.value?.id != wallpaper.id) { _homeState.value = CustomizationState(); _lockState.value = CustomizationState() }
        _selectedWallpaper.value = wallpaper
    }

    fun updateHomeState(state: CustomizationState) { _homeState.value = state }
    fun updateLockState(state: CustomizationState) { _lockState.value = state }
    fun clearStatus() { _applyStatus.value = null }
    fun setCategoryAndFetch(category: String) { _activeCategory.value = category; activeApiQuery = category; fetchWallpapers(isLoadMore = false) }
    fun performCustomSearch(query: String) { _activeCategory.value = ""; activeApiQuery = query.trim(); fetchWallpapers(isLoadMore = false) }

    fun applyWallpaperSpecific(wallpaper: Wallpaper, screenType: Int, screenWidthPx: Float, screenHeightPx: Float) {
        viewModelScope.launch {
            _applyStatus.value = "Applying..."
            val context = getApplication<Application>()
            settingsManager.saveAutomationMode("None")
            settingsManager.saveLastWallpaperId(-1)
            WorkManager.getInstance(context).cancelAllWorkByTag("auto_wallpaper")

            val isExplore = wallpaper.source != "Gallery" && wallpaper.source != "Local"
            settingsManager.saveIsExploreWallpaperActive(isExplore)

            if (isExplore) {
                settingsManager.saveAutoChangeQuery(activeApiQuery)
            }

            val hState = _homeState.value; val lState = _lockState.value
            val success = if (screenType == 3) {
                val hOk = WallpaperSetter.applyCustomWallpaper(context, wallpaper.imageUrl, null, WallpaperManager.FLAG_SYSTEM, hState.scale, hState.offsetX, hState.offsetY, hState.blur, hState.grayscale, hState.dimAmount, screenWidthPx, screenHeightPx)
                val lOk = WallpaperSetter.applyCustomWallpaper(context, wallpaper.imageUrl, null, WallpaperManager.FLAG_LOCK, lState.scale, lState.offsetX, lState.offsetY, lState.blur, lState.grayscale, lState.dimAmount, screenWidthPx, screenHeightPx)
                hOk && lOk
            } else if (screenType == 1) {
                WallpaperSetter.applyCustomWallpaper(context, wallpaper.imageUrl, null, WallpaperManager.FLAG_SYSTEM, hState.scale, hState.offsetX, hState.offsetY, hState.blur, hState.grayscale, hState.dimAmount, screenWidthPx, screenHeightPx)
            } else {
                WallpaperSetter.applyCustomWallpaper(context, wallpaper.imageUrl, null, WallpaperManager.FLAG_LOCK, lState.scale, lState.offsetX, lState.offsetY, lState.blur, lState.grayscale, lState.dimAmount, screenWidthPx, screenHeightPx)
            }
            _applyStatus.value = if (success) "Success!" else "Failed."
        }
    }

    fun fetchWallpapers(isLoadMore: Boolean = false) {
        if (isFetching) return
        isFetching = true
        viewModelScope.launch {
            if (!isLoadMore) { _isLoading.value = true; _wallpapers.value = emptyList(); currentPage = if (_activeCategory.value.equals("Curated", ignoreCase = true)) (1..15).random() else 1 }
            else { currentPage++ }

            val pexelsKey = settingsManager.pexelsApiKey.firstOrNull() ?: ""; val wallhavenKey = settingsManager.wallhavenApiKey.firstOrNull() ?: ""
            val pexelsData = if (_pexelsStatus.value == ApiStatus.Valid) repository.getPexelsWallpapers(pexelsKey, activeApiQuery, currentPage) else emptyList()
            val wKeyToPass = if (_wallhavenStatus.value == ApiStatus.Valid || wallhavenKey.isBlank()) wallhavenKey.ifBlank { null } else null
            val wallhavenData = repository.getWallhavenWallpapers(wKeyToPass, activeApiQuery, currentPage)

            val combined = mutableListOf<Wallpaper>()
            val pIterator = pexelsData.iterator(); val wIterator = wallhavenData.iterator()
            while (pIterator.hasNext() || wIterator.hasNext()) {
                if (pIterator.hasNext()) combined.add(pIterator.next())
                if (wIterator.hasNext()) combined.add(wIterator.next())
            }

            val finalData = combined.shuffled()
            if (isLoadMore) { if (finalData.isNotEmpty()) _wallpapers.value = _wallpapers.value + finalData } else { _wallpapers.value = finalData }
            _isLoading.value = false; isFetching = false
        }
    }

    fun saveKeys(p: String, w: String) {
        viewModelScope.launch {
            _pexelsStatus.value = ApiStatus.Checking; _wallhavenStatus.value = ApiStatus.Checking
            val pValid = repository.validatePexelsKey(p); val wValid = repository.validateWallhavenKey(w)
            _pexelsStatus.value = if (p.isBlank()) ApiStatus.Blank else if (pValid) ApiStatus.Valid else ApiStatus.Invalid
            _wallhavenStatus.value = if (w.isBlank()) ApiStatus.Blank else if (wValid) ApiStatus.Valid else ApiStatus.Invalid
            if (pValid || p.isBlank()) settingsManager.savePexelsKey(p); if (wValid || w.isBlank()) settingsManager.saveWallhavenKey(w)
            fetchWallpapers()
        }
    }

    fun loadLocalGallery() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            _downloadedWallpapers.value = StorageHelper.getDownloadedWallpapers(context)
        }
    }

    fun downloadWallpaper(wallpaper: Wallpaper) {
        viewModelScope.launch {
            _isDownloading.value = true; _applyStatus.value = "Downloading..."
            val context = getApplication<Application>()
            val success = StorageHelper.downloadAndSaveWallpaper(context, wallpaper.imageUrl, wallpaper.id)
            _isDownloading.value = false
            if (success) { _applyStatus.value = "Saved to Pictures/FunThingWalls!"; loadLocalGallery() } else { _applyStatus.value = "Download failed." }
        }
    }

    fun selectLocalImageUri(uriString: String) {
        val localWall = Wallpaper(
            id = "custom_${System.currentTimeMillis()}",
            imageUrl = uriString,
            thumbnailUrl = uriString,
            source = "Gallery",
            credit = "Local File"
        )
        selectWallpaper(localWall)
    }
}