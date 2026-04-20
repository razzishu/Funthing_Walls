package com.`fun`.walls.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun MainAppScreen(viewModel: WallpaperViewModel) {
    val navController = rememberNavController()
    val items = listOf(Screen.Home, Screen.Automate, Screen.Local, Screen.Settings)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = items.any { it.route == currentRoute }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Explore Feed
                composable(Screen.Home.route) {
                    HomeScreen(viewModel) { navController.navigate("preview") }
                }

                // 2. The Auto-Engine
                composable(Screen.Automate.route) {
                    AutomateScreen(viewModel)
                }

                // 3. FIX: THE NEW GALLERY SCREEN
                // We deleted the dummy text and wired it directly to our new GalleryScreen,
                // passing the exact same navigation command used by the Home screen.
                composable(Screen.Local.route) {
                    GalleryScreen(viewModel) { navController.navigate("preview") }
                }

                // 4. Settings
                composable(Screen.Settings.route) {
                    SettingsScreen(viewModel)
                }

                // 5. The Editor / Preview Overlay
                composable("preview") {
                    PreviewScreen(viewModel = viewModel, onBackClick = { navController.popBackStack() })
                }
            }

            // The Floating Bottom Nav Bar
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .height(64.dp)
                        .shadow(elevation = 20.dp, shape = RoundedCornerShape(50), spotColor = Color.Black.copy(alpha = 0.6f))
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                ) {
                    Row(
                        modifier = Modifier.fillMaxHeight().padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        val currentDestination = navBackStackEntry?.destination
                        items.forEach { screen ->
                            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                            IconButton(
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}