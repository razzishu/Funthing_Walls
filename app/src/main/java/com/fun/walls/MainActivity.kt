package com.`fun`.walls

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.`fun`.walls.data.SettingsManager
import com.`fun`.walls.ui.MainAppScreen
import com.`fun`.walls.ui.WelcomeScreen
import com.`fun`.walls.ui.WallpaperViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: WallpaperViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val settingsManager = remember { SettingsManager(context) }
            val coroutineScope = rememberCoroutineScope()

            // Read the onboarding state from DataStore
            val hasCompletedOnboarding by settingsManager.hasCompletedOnboarding.collectAsState(initial = null)

            val colors = if (isSystemInDarkTheme()) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }

            MaterialTheme(colorScheme = colors) {
                // If it's null, DataStore is still reading from disk (shows blank background for ~50ms)
                when (hasCompletedOnboarding) {
                    null -> {
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    }
                    // If false, show the shiny new Welcome Screen
                    false -> {
                        WelcomeScreen(
                            onFinishOnboarding = {
                                coroutineScope.launch {
                                    settingsManager.saveHasCompletedOnboarding(true)
                                }
                            }
                        )
                    }
                    // If true, proceed directly to the app
                    else -> {
                        MainAppScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}