package com.`fun`.walls.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew // FIX: AutoMirrored
import androidx.compose.material.icons.automirrored.rounded.Send // FIX: AutoMirrored
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: WallpaperViewModel) {
    val pKeyCurrent by viewModel.pexelsKeyFlow.collectAsState(initial = "")
    val wKeyCurrent by viewModel.wallhavenKeyFlow.collectAsState(initial = "")
    val pStatus by viewModel.pexelsStatus.collectAsState()
    val wStatus by viewModel.wallhavenStatus.collectAsState()

    val autoChangeEnabled by viewModel.autoChangeEnabledFlow.collectAsState(initial = false)
    val autoChangeInterval by viewModel.autoChangeIntervalFlow.collectAsState(initial = 60)
    val autoChangeTarget by viewModel.autoChangeTargetFlow.collectAsState(initial = 3)

    var pexelsInput by remember(pKeyCurrent) { mutableStateOf(pKeyCurrent ?: "") }
    var wallhavenInput by remember(wKeyCurrent) { mutableStateOf(wKeyCurrent ?: "") }
    var pexelsVisible by remember { mutableStateOf(false) }
    var wallhavenVisible by remember { mutableStateOf(false) }

    var showIntervalMenu by remember { mutableStateOf(false) }
    var showTargetMenu by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var customIntervalInput by remember { mutableStateOf("") }

    var showAbout by remember { mutableStateOf(false) }

    val uriHandler = LocalUriHandler.current
    val intervalOptions = listOf(15, 30, 60, 120, 180)

    val targetOptions = mapOf(3 to "Home & Lock Screens", 1 to "Home Screen Only", 2 to "Lock Screen Only")

    fun getIntervalText(minutes: Int): String = when (minutes) {
        15 -> "15 Minutes"
        30 -> "30 Minutes"
        60 -> "1 Hour"
        120 -> "2 Hours"
        180 -> "3 Hours"
        else -> "$minutes Minutes (Custom)"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp, start = 24.dp, end = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        AppAestheticHeader()

        Spacer(modifier = Modifier.height(32.dp))
        Text("Engine Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        // --- PEXELS API ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Pexels API Key", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.clickable { uriHandler.openUri("https://www.pexels.com/api/") }.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Get Key", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) // FIX
            }
        }
        OutlinedTextField(
            value = pexelsInput,
            onValueChange = { pexelsInput = it },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text("Paste Pexels Key (Required)") },
            visualTransformation = if (pexelsVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                    IconButton(onClick = { pexelsVisible = !pexelsVisible }) {
                        Icon(if (pexelsVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, "Toggle Visibility")
                    }
                    StatusIcon(pStatus)
                }
            }
        )
        Text("Required for the Explore feed. Sign up for a free Pexels account and generate a key.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, start = 4.dp))

        Spacer(modifier = Modifier.height(16.dp))

        // --- WALLHAVEN API ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Wallhaven API Key", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.clickable { uriHandler.openUri("https://wallhaven.cc/settings/account") }.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Get Key", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) // FIX
            }
        }
        OutlinedTextField(
            value = wallhavenInput,
            onValueChange = { wallhavenInput = it },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text("Paste Wallhaven Key (Optional)") },
            visualTransformation = if (wallhavenVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                    IconButton(onClick = { wallhavenVisible = !wallhavenVisible }) {
                        Icon(if (wallhavenVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, "Toggle Visibility")
                    }
                    StatusIcon(wStatus)
                }
            }
        )
        Text("Optional. Unlocks premium abstract and anime wallpapers. Sign in and copy key from Account Settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, start = 4.dp))

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.saveKeys(pexelsInput, wallhavenInput) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            enabled = pStatus != ApiStatus.Checking && wStatus != ApiStatus.Checking
        ) {
            if (pStatus == ApiStatus.Checking || wStatus == ApiStatus.Checking) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verifying Keys...")
            } else {
                Text("Save & Validate Keys")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))

        // --- AUTO-CHANGER SETTINGS ---
        Text("Explore Auto-Changer", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Automatically downloads and applies a new wallpaper from your active Explore feed. \n\nNote: This engine will safely pause itself if you apply a custom image from your Local Gallery.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Auto-Change", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Switch(
                checked = autoChangeEnabled,
                onCheckedChange = { viewModel.setAutoChangeSettings(enabled = it, intervalMinutes = autoChangeInterval) }
            )
        }

        if (autoChangeEnabled) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Change Interval", style = MaterialTheme.typography.labelLarge)
                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = getIntervalText(autoChangeInterval),
                        onValueChange = {}, readOnly = true, enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Select") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { showIntervalMenu = true })
                    DropdownMenu(
                        expanded = showIntervalMenu, onDismissRequest = { showIntervalMenu = false },
                        modifier = Modifier.fillMaxWidth(0.85f).background(MaterialTheme.colorScheme.surface)
                    ) {
                        intervalOptions.forEach { minutes ->
                            DropdownMenuItem(text = { Text(getIntervalText(minutes)) }, onClick = { viewModel.setAutoChangeSettings(enabled = true, intervalMinutes = minutes); showIntervalMenu = false })
                        }
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("Custom Interval...") }, onClick = { showIntervalMenu = false; showCustomDialog = true })
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Text("Target Screen", style = MaterialTheme.typography.labelLarge)
                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = targetOptions[autoChangeTarget] ?: "Home & Lock Screens",
                        onValueChange = {}, readOnly = true, enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Select") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { showTargetMenu = true })
                    DropdownMenu(
                        expanded = showTargetMenu, onDismissRequest = { showTargetMenu = false },
                        modifier = Modifier.fillMaxWidth(0.85f).background(MaterialTheme.colorScheme.surface)
                    ) {
                        targetOptions.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { viewModel.setAutoChangeTarget(key); showTargetMenu = false })
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        // --- COMMUNITY & SOURCE LINKS ---
        Text("Community & Support", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedCard(
                onClick = { uriHandler.openUri("https://github.com/razzishu/Funthing_Walls") },
                modifier = Modifier.weight(1f).height(80.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Code, contentDescription = "GitHub", modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Source Code", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedCard(
                onClick = { uriHandler.openUri("https://t.me/FunthingWalls") },
                modifier = Modifier.weight(1f).height(80.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFF0088cc).copy(alpha = 0.1f), contentColor = Color(0xFF0088cc))
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Telegram", modifier = Modifier.size(26.dp)) // FIX: AutoMirrored
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Telegram", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- ABOUT & CHANGELOG SECTION ---
        ElevatedCard(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable { showAbout = !showAbout },
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("About & Changelog", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }

                AnimatedVisibility(visible = showAbout) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Text("Version 2.0.3 - The Optimization Update", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("FunThingWalls is a next-generation Android Customization Studio built entirely in Jetpack Compose.", style = MaterialTheme.typography.bodyMedium)

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("What's New in 2.0.3:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("• Deep Battery Optimization: Hardware-accelerated rendering now adapts to Power Save mode and reduces overhead when idle.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                        Text("• Modern Blur Engine: Migrated from RenderScript to the highly efficient RenderEffect API for buttery smooth blurs.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                        Text("• FTW Branding: Redesigned the app logo with a sleek, minimalist 'FTW' look and neon glassmorphism.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                        Text("• UI Animation: Added a living 'breathing' effect to the logo on startup.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                        Text("• Stability Fixes: Resolved multiple code warnings and improved API key validation logic.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // --- FOOTER ---
        Text(
            text = "MADE WITH ♥ BY RAZZ",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(120.dp))
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Custom Interval") },
            text = {
                Column {
                    Text("Enter auto-change interval in minutes (Min: 15):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customIntervalInput,
                        onValueChange = { customIntervalInput = it.filter { char -> char.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val minutes = customIntervalInput.toIntOrNull()
                    if (minutes != null && minutes >= 15) {
                        viewModel.setAutoChangeSettings(enabled = true, intervalMinutes = minutes)
                        showCustomDialog = false
                        customIntervalInput = ""
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showCustomDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AppAestheticHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF141E30), Color(0xFF243B55))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE91E63).copy(alpha = 0.4f), Color.Transparent),
                    center = Offset(width * 0.8f, height * 0.2f),
                    radius = width * 0.5f
                ),
                radius = width * 0.5f,
                center = Offset(width * 0.8f, height * 0.2f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00BCD4).copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(width * 0.2f, height * 0.8f),
                    radius = width * 0.6f
                ),
                radius = width * 0.6f,
                center = Offset(width * 0.2f, height * 0.8f)
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = height * 0.6f,
                center = Offset(width * 0.5f, height * 0.5f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = height * 0.4f,
                center = Offset(width * 0.5f, height * 0.5f)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "FUN THING WALLS",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            )
            Text(
                text = "S T U D I O",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF6DD5FA),
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp
            )

            Surface(
                color = Color.Black.copy(alpha = 0.3f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(
                    text = "v2.0.3",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun StatusIcon(status: ApiStatus) {
    when (status) {
        ApiStatus.Valid -> Icon(Icons.Rounded.CheckCircle, "Valid", tint = Color(0xFF4CAF50))
        ApiStatus.Invalid -> Icon(Icons.Rounded.Error, "Invalid", tint = Color(0xFFF44336))
        ApiStatus.Blank -> Icon(Icons.Rounded.Info, "Blank", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        ApiStatus.Checking -> CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
    }
}