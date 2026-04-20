package com.`fun`.walls.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.`fun`.walls.models.Wallpaper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: WallpaperViewModel, onNavigateToPreview: () -> Unit) {
    val wallpapers by viewModel.wallpapers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val activeCategory by viewModel.activeCategory.collectAsState()

    val pStatus by viewModel.pexelsStatus.collectAsState()
    val wStatus by viewModel.wallhavenStatus.collectAsState()

    var searchInput by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val categories = listOf("Curated", "Abstract", "Amoled", "Nature", "Anime", "Cars", "Minimal", "Space")
    val gridState = rememberLazyGridState()

    // FIX: Wrapped derivedStateOf inside a remember block to prevent composition leaks
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem > 0 && lastVisibleItem >= totalItems - 6
        }
    }

    LaunchedEffect(isAtBottom) {
        if (isAtBottom && !isLoading) viewModel.fetchWallpapers(isLoadMore = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = 48.dp)) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Explore", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            val (text, color) = when {
                pStatus == ApiStatus.Valid && wStatus == ApiStatus.Valid -> "Premium Mix" to Color(0xFF4CAF50)
                pStatus == ApiStatus.Invalid || wStatus == ApiStatus.Invalid -> "API Error" to Color(0xFFF44336)
                pStatus == ApiStatus.Valid || wStatus == ApiStatus.Valid -> "Premium Mode" to Color(0xFF4CAF50)
                else -> "Free Mode" to Color(0xFF2196F3)
            }

            Surface(
                color = color.copy(alpha = 0.2f),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
                }
            }
        }

        OutlinedTextField(
            value = searchInput,
            onValueChange = { searchInput = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Search tags (e.g. #cyberpunk)") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                    if (searchInput.isNotBlank()) viewModel.performCustomSearch(searchInput)
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent
            )
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = activeCategory.equals(cat, ignoreCase = true),
                    onClick = {
                        focusManager.clearFocus()
                        searchInput = ""
                        viewModel.setCategoryAndFetch(cat)
                    },
                    label = { Text(cat) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading && wallpapers.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(wallpapers) { wallpaper ->
                        WallpaperCard(wallpaper) {
                            viewModel.selectWallpaper(wallpaper)
                            onNavigateToPreview()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WallpaperCard(wallpaper: Wallpaper, onClick: () -> Unit) {
    AsyncImage(
        model = wallpaper.thumbnailUrl,
        contentDescription = "Wallpaper",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.6f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    )
}