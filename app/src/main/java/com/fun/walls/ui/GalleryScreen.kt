package com.`fun`.walls.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.SecurityUpdateWarning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(viewModel: WallpaperViewModel, onNavigateToPreview: () -> Unit) {
    val context = LocalContext.current
    val downloadedWalls by viewModel.downloadedWallpapers.collectAsState()

    // --- FIX: SECURE STORAGE PERMISSION CHECKING ---
    val storagePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES) // Android 13+
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE) // Android 12 and below
    }

    var hasPermission by remember {
        mutableStateOf(storagePermissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED })
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        hasPermission = results.values.all { it }
        if (hasPermission) {
            viewModel.loadLocalGallery()
        }
    }

    // Auto-ask for permission when the screen opens
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.loadLocalGallery()
        } else {
            permissionLauncher.launch(storagePermissions)
        }
    }

    // Native Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.selectLocalImageUri(uri.toString())
            onNavigateToPreview()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = 48.dp)) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.FolderSpecial, contentDescription = "Gallery", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("My Studio", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }

        // FULL GALLERY BUTTON
        Button(
            onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = "Pick Image", modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Choose from Full Device Gallery", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        Text(
            text = "Downloaded Wallpapers",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        // --- GRID RENDERING WITH PERMISSION FALLBACK ---
        if (!hasPermission) {
            Box(modifier = Modifier.fillMaxSize().weight(1f).padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.SecurityUpdateWarning, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Storage Access Required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("We need permission to display the wallpapers you've downloaded to your device.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { permissionLauncher.launch(storagePermissions) }) {
                        Text("Grant Permission")
                    }
                }
            }
        } else if (downloadedWalls.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text("No downloaded wallpapers yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(downloadedWalls, key = { it.id }) { wall ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(0.6f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.DarkGray)
                            .clickable {
                                viewModel.selectWallpaper(wall)
                                onNavigateToPreview()
                            }
                    ) {
                        AsyncImage(
                            model = wall.imageUrl,
                            contentDescription = "Local Wallpaper",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}