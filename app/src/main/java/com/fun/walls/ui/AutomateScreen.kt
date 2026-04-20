package com.`fun`.walls.ui

import android.Manifest
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.`fun`.walls.workers.ChameleonWallpaperService
import com.`fun`.walls.workers.WeatherWallpaperService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomateScreen(viewModel: WallpaperViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val wallpaperManager = WallpaperManager.getInstance(context)

    val selectedMode by viewModel.automationModeFlow.collectAsState(initial = "None")
    val savedWallpaperId by viewModel.lastWallpaperIdFlow.collectAsState(initial = -1)
    val isBooting by viewModel.isEngineBooting.collectAsState()
    val applyStatus by viewModel.applyStatus.collectAsState()

    // NEW: Observe Auto-Changer state to manage engine conflicts
    val autoChangeEnabled by viewModel.autoChangeEnabledFlow.collectAsState(initial = false)
    val autoChangeInterval by viewModel.autoChangeIntervalFlow.collectAsState(initial = 60)

    var pendingMode by remember { mutableStateOf("") }

    val isChameleonActive = selectedMode == "Music"
    val isTimeActive = selectedMode == "Time"
    val isWeatherActive = selectedMode == "Weather"

    LaunchedEffect(applyStatus) {
        applyStatus?.let {
            if (it.isNotBlank()) {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearStatus()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isTimeActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isBooting) {
                    val currentId = wallpaperManager.getWallpaperId(WallpaperManager.FLAG_SYSTEM)
                    if (savedWallpaperId != -1 && currentId > 0 && currentId != savedWallpaperId) {
                        viewModel.setAutomationMode("None")
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val storagePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_EXTERNAL_STORAGE)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val launchChameleonIntent = {
        Toast.makeText(context, "⚠️ Please select 'Lock screen' on the next menu!", Toast.LENGTH_LONG).show()
        context.startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(context, ChameleonWallpaperService::class.java))
        })
    }

    val launchWeatherIntent = {
        Toast.makeText(context, "⚠️ Please select 'Home and Lock screen' on the next menu!", Toast.LENGTH_LONG).show()
        context.startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(context, WeatherWallpaperService::class.java))
        })
    }

    val storageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.containsValue(true)) {
            if (pendingMode == "Music") launchChameleonIntent()
            if (pendingMode == "Weather") launchWeatherIntent()
        } else {
            viewModel.setAutomationMode("None")
            Toast.makeText(context, "Storage permission is required.", Toast.LENGTH_SHORT).show()
        }
        pendingMode = ""
    }

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted && pendingMode.isNotEmpty()) {
            viewModel.setAutomationMode(pendingMode)
        } else {
            viewModel.setAutomationMode("None")
            Toast.makeText(context, "Location permission is required.", Toast.LENGTH_SHORT).show()
        }
        pendingMode = ""
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 48.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoMode, contentDescription = "Automate", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Auto-Engine", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }

            Text("Let the app adapt to your life.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

            // --- NEW: AUTO-CHANGER CONFLICT WARNING BANNER ---
            AnimatedVisibility(visible = autoChangeEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WarningAmber, contentDescription = "Warning", tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Explore Auto-Changer is currently ON.\nEnabling Time or Weather Sync will automatically disable it to prevent engine conflicts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- CHAMELEON GLASS ---
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFFE91E63).copy(alpha = 0.1f)).border(2.dp, if (isChameleonActive) Color(0xFFE91E63) else Color.Transparent, RoundedCornerShape(24.dp)).padding(20.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFE91E63).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.GraphicEq, null, tint = Color(0xFFE91E63), modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Chameleon Glass", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Switch(
                            checked = isChameleonActive,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    pendingMode = "Music"
                                    viewModel.setAutomationMode("Music")
                                    val hasPermission = storagePermissions.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
                                    if (hasPermission) launchChameleonIntent() else storageLauncher.launch(storagePermissions)
                                } else {
                                    viewModel.setAutomationMode("None")
                                    CoroutineScope(Dispatchers.IO).launch { try { wallpaperManager.clear(WallpaperManager.FLAG_LOCK) } catch (e: Exception) {} }
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFE91E63), checkedTrackColor = Color(0xFFE91E63).copy(alpha = 0.5f))
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("A hardware-accelerated Live Wallpaper. Seamlessly reads your Home Screen and adapts to a fluid audio visualizer when music plays.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // --- TIME BASED ---
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF2196F3).copy(alpha = 0.1f)).border(2.dp, if (isTimeActive) Color(0xFF2196F3) else Color.Transparent, RoundedCornerShape(24.dp)).padding(20.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF2196F3).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Schedule, null, tint = Color(0xFF2196F3), modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Time Sync", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Switch(
                            checked = isTimeActive,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    // FIX: Automatically disable Auto-Changer to guarantee TimeSync gets priority!
                                    if (autoChangeEnabled) {
                                        viewModel.setAutoChangeSettings(false, autoChangeInterval)
                                        Toast.makeText(context, "Explore Auto-Changer paused to prioritize Time Sync.", Toast.LENGTH_LONG).show()
                                    }
                                    pendingMode = "Time"
                                    locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                } else {
                                    viewModel.setAutomationMode("None")
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF2196F3), checkedTrackColor = Color(0xFF2196F3).copy(alpha = 0.5f))
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Updates both screens every hour of the day. Mathematically syncs color temperatures to the sun, applying a warm cinematic grade at sunset and dimming at night.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // --- WEATHER ENGINE ---
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF00BCD4).copy(alpha = 0.1f)).border(2.dp, if (isWeatherActive) Color(0xFF00BCD4) else Color.Transparent, RoundedCornerShape(24.dp)).padding(20.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF00BCD4).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.CloudQueue, null, tint = Color(0xFF00BCD4), modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Atmosphere Sync", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Switch(
                            checked = isWeatherActive,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    // FIX: Automatically disable Auto-Changer to guarantee WeatherSync gets priority!
                                    if (autoChangeEnabled) {
                                        viewModel.setAutoChangeSettings(false, autoChangeInterval)
                                        Toast.makeText(context, "Explore Auto-Changer paused to prioritize Weather Sync.", Toast.LENGTH_LONG).show()
                                    }
                                    pendingMode = "Weather"
                                    viewModel.setAutomationMode("Weather")
                                    val hasPermission = storagePermissions.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
                                    if (hasPermission) launchWeatherIntent() else storageLauncher.launch(storagePermissions)
                                } else {
                                    viewModel.setAutomationMode("None")
                                    CoroutineScope(Dispatchers.IO).launch { try { wallpaperManager.clear(WallpaperManager.FLAG_LOCK) } catch (e: Exception) {} }
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00BCD4), checkedTrackColor = Color(0xFF00BCD4).copy(alpha = 0.5f))
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("A hardware-accelerated Procedural Weather Engine. Generates 60FPS fluid rain, thunderstorms, snow, and dense fog based on real-world conditions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }

        if (isBooting) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Syncing API...", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
        }
    }
}