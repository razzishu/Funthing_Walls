package com.`fun`.walls.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Start
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeScreen(onFinishOnboarding: () -> Unit) {

    val permissionsToRequest = remember {
        val perms = mutableListOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        perms.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        onFinishOnboarding()
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()) // Makes it safe for smaller screens
        ) {
            // --- 1. HERO HEADER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp) // Fixed height for scrollable layout
                    .clip(RoundedCornerShape(bottomStart = 48.dp, bottomEnd = 48.dp))
                    .background(Brush.linearGradient(colors = listOf(Color(0xFF141E30), Color(0xFF243B55))))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width; val height = size.height
                    drawCircle(
                        brush = Brush.radialGradient(colors = listOf(Color(0xFFE91E63).copy(alpha = 0.4f), Color.Transparent), center = Offset(width * 0.8f, height * 0.2f), radius = width * 0.5f),
                        radius = width * 0.5f, center = Offset(width * 0.8f, height * 0.2f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(colors = listOf(Color(0xFF00BCD4).copy(alpha = 0.3f), Color.Transparent), center = Offset(width * 0.2f, height * 0.8f), radius = width * 0.6f),
                        radius = width * 0.6f, center = Offset(width * 0.2f, height * 0.8f)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text("Welcome to", style = MaterialTheme.typography.titleLarge, color = Color.White.copy(alpha = 0.7f))
                    Text("FunThingWalls", style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Text("The Ultimate Customization Studio", style = MaterialTheme.typography.titleMedium, color = Color(0xFF6DD5FA), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }
            }

            // --- 2. FEATURES LIST ---
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                FeatureRow(
                    icon = Icons.Rounded.CloudQueue, color = Color(0xFF00BCD4),
                    title = "Atmosphere Engine",
                    description = "Procedural weather and cinematic time-syncing based on your physical location."
                )
                FeatureRow(
                    icon = Icons.Rounded.GraphicEq, color = Color(0xFFE91E63),
                    title = "Chameleon Glass",
                    description = "Hardware-accelerated audio visualization that reads and reacts to system music."
                )
                FeatureRow(
                    icon = Icons.Rounded.FolderSpecial, color = Color(0xFF4CAF50),
                    title = "Secure Local Studio",
                    description = "Manage downloaded 4K assets privately without invasive storage access."
                )

                // NEW: Explicit API Feature Callout
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    FeatureRow(
                        icon = Icons.Rounded.AutoAwesome, color = Color(0xFFFFC107),
                        title = "Endless Discovery",
                        description = "For the ultimate enhanced experience, easily link free Pexels and Wallhaven APIs in the settings to unlock millions of premium assets.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // --- 3. PERMISSIONS & ACTION ---
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "To power the live weather engine and save wallpapers locally, we'll need Location and Storage access on the next step.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Button(
                    onClick = { permissionLauncher.launch(permissionsToRequest) },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Grant Permissions & Enter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(Icons.Rounded.Start, contentDescription = null)
                }
                Spacer(modifier = Modifier.height(48.dp)) // Bottom padding
            }
        }
    }
}

@Composable
fun FeatureRow(icon: ImageVector, color: Color, title: String, description: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.size(56.dp).background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
        }
    }
}