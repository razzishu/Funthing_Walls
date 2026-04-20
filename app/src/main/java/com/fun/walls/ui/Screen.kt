package com.`fun`.walls.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Home : Screen("home", "Explore", Icons.Rounded.Dashboard, Icons.Outlined.Dashboard)
    // REPLACED MAGIC WITH AUTOMATE
    object Automate : Screen("automate", "Automate", Icons.Rounded.AutoMode, Icons.Outlined.AutoMode)
    object Local : Screen("local", "Device", Icons.Rounded.Image, Icons.Outlined.Image)
    object Settings : Screen("settings", "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings)
}