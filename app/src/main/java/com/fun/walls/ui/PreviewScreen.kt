package com.`fun`.walls.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack // FIX: AutoMirrored import
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PreviewScreen(viewModel: WallpaperViewModel, onBackClick: () -> Unit) {
    val wallpaper by viewModel.selectedWallpaper.collectAsState()
    val applyStatus by viewModel.applyStatus.collectAsState()
    val homeState by viewModel.homeState.collectAsState()
    val lockState by viewModel.lockState.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 2 })
    val selectedTab = pagerState.currentPage

    var isEditMode by remember { mutableStateOf(false) }
    var showApplyDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val screenAspectRatio = screenWidthPx / screenHeightPx

    var boxWidthPx by remember { mutableFloatStateOf(0f) }
    var boxHeightPx by remember { mutableFloatStateOf(0f) }
    var imageWidthPx by remember { mutableFloatStateOf(0f) }
    var imageHeightPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(applyStatus) {
        applyStatus?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatus()
        }
    }

    if (wallpaper == null) {
        LaunchedEffect(Unit) { onBackClick() }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(if (isEditMode) "Editor" else "Preview") },
                    // FIX: Using AutoMirrored Icon
                    navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                    actions = { IconButton(onClick = { isEditMode = !isEditMode }) { Icon(if (isEditMode) Icons.Rounded.Close else Icons.Rounded.Tune, "Edit") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                if (!isEditMode) {
                    // FIX: Migrated from TabRow to SecondaryTabRow to comply with Material 3 guidelines
                    SecondaryTabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, divider = {}) {
                        Tab(selected = selectedTab == 0, onClick = { }) { Text("Home", modifier = Modifier.padding(12.dp)) }
                        Tab(selected = selectedTab == 1, onClick = { }) { Text("Lock", modifier = Modifier.padding(12.dp)) }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp)
                    .aspectRatio(screenAspectRatio)
                    .clip(RoundedCornerShape(32.dp))
                    .border(8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
                    .onGloballyPositioned { coords ->
                        boxWidthPx = coords.size.width.toFloat()
                        boxHeightPx = coords.size.height.toFloat()
                    }
                    .pointerInput(isEditMode, boxWidthPx, imageWidthPx, selectedTab, homeState, lockState) {
                        if (!isEditMode || boxWidthPx == 0f || imageWidthPx == 0f) return@pointerInput
                        detectTransformGestures { _, pan, zoom, _ ->
                            val currentState = if (selectedTab == 0) homeState else lockState
                            val newScale = (currentState.scale * zoom).coerceIn(1f, 5f)
                            val baseScale = maxOf(boxWidthPx / imageWidthPx, boxHeightPx / imageHeightPx)
                            val reqWidth = imageWidthPx * baseScale * newScale
                            val reqHeight = imageHeightPx * baseScale * newScale
                            val maxOffsetX = maxOf(0f, (reqWidth - boxWidthPx) / 2f)
                            val maxOffsetY = maxOf(0f, (reqHeight - boxHeightPx) / 2f)
                            val newX = (currentState.offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                            val newY = (currentState.offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            val newState = currentState.copy(scale = newScale, offsetX = newX, offsetY = newY)
                            if (selectedTab == 0) viewModel.updateHomeState(newState) else viewModel.updateLockState(newState)
                        }
                    }
            ) { page ->
                val pageState = if (page == 0) homeState else lockState
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

                    val dimScale = 1f - pageState.dimAmount
                    val grayscale = pageState.grayscale
                    val finalColorMatrix = ColorMatrix(floatArrayOf(
                        dimScale * (1f - grayscale) + (grayscale * 0.2126f * dimScale), dimScale * (grayscale * 0.7152f), dimScale * (grayscale * 0.0722f), 0f, 0f,
                        dimScale * (grayscale * 0.2126f), dimScale * (1f - grayscale) + (grayscale * 0.7152f * dimScale), dimScale * (grayscale * 0.0722f), 0f, 0f,
                        dimScale * (grayscale * 0.2126f), dimScale * (grayscale * 0.7152f), dimScale * (1f - grayscale) + (grayscale * 0.0722f * dimScale), 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))

                    SubcomposeAsyncImage(
                        model = wallpaper!!.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = pageState.scale
                                scaleY = pageState.scale
                                translationX = pageState.offsetX
                                translationY = pageState.offsetY
                            }
                            .blur(pageState.blur.dp),
                        colorFilter = ColorMatrixColorFilter(finalColorMatrix),
                        loading = {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        onSuccess = { state ->
                            imageWidthPx = state.painter.intrinsicSize.width
                            imageHeightPx = state.painter.intrinsicSize.height
                        }
                    )

                    if (page == 1) {
                        Text(
                            text = "12:00",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp)
                        )
                    }
                }
            }

            if (isEditMode) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
                    if (selectedTab == 0) {
                        Text("Home Screen Blur", style = MaterialTheme.typography.labelMedium)
                        ClaySlider(value = homeState.blur, onValueChange = { viewModel.updateHomeState(homeState.copy(blur = it)) }, valueRange = 0f..25f)
                    } else {
                        Text("Lock Screen Blur", style = MaterialTheme.typography.labelMedium)
                        ClaySlider(value = lockState.blur, onValueChange = { viewModel.updateLockState(lockState.copy(blur = it)) }, valueRange = 0f..25f)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Grayscale (B&W)", style = MaterialTheme.typography.labelMedium)
                        ClaySlider(value = lockState.grayscale, onValueChange = { viewModel.updateLockState(lockState.copy(grayscale = it)) }, valueRange = 0f..1f)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Dim Wallpaper", style = MaterialTheme.typography.labelMedium)
                        ClaySlider(value = lockState.dimAmount, onValueChange = { viewModel.updateLockState(lockState.copy(dimAmount = it)) }, valueRange = 0f..0.8f)
                    }
                }
            } else {
                Text(
                    text = wallpaper!!.credit,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 0.dp).padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.downloadWallpaper(wallpaper!!) },
                        enabled = !isDownloading,
                        modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }

                    Button(
                        onClick = { showApplyDialog = true },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(Icons.Rounded.Check, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply Wallpaper")
                    }
                }
            }
        }

        if (showApplyDialog) {
            AlertDialog(
                onDismissRequest = { showApplyDialog = false },
                title = { Text("Apply Final Image") },
                text = { Text("Apply your unique Home and Lock screen crops and effects?") },
                confirmButton = {
                    TextButton(onClick = {
                        val mult = screenWidthPx / boxWidthPx
                        viewModel.updateHomeState(homeState.copy(offsetX = homeState.offsetX * mult, offsetY = homeState.offsetY * mult))
                        viewModel.updateLockState(lockState.copy(offsetX = lockState.offsetX * mult, offsetY = lockState.offsetY * mult))
                        viewModel.applyWallpaperSpecific(wallpaper!!, 3, screenWidthPx, screenHeightPx)
                        showApplyDialog = false
                    }) { Text("Both") }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            val mult = screenWidthPx / boxWidthPx
                            viewModel.updateHomeState(homeState.copy(offsetX = homeState.offsetX * mult, offsetY = homeState.offsetY * mult))
                            viewModel.applyWallpaperSpecific(wallpaper!!, 1, screenWidthPx, screenHeightPx)
                            showApplyDialog = false
                        }) { Text("Home") }
                        TextButton(onClick = {
                            val mult = screenWidthPx / boxWidthPx
                            viewModel.updateLockState(lockState.copy(offsetX = lockState.offsetX * mult, offsetY = lockState.offsetY * mult))
                            viewModel.applyWallpaperSpecific(wallpaper!!, 2, screenWidthPx, screenHeightPx)
                            showApplyDialog = false
                        }) { Text("Lock") }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaySlider(value: Float, onValueChange: (Float) -> Unit, valueRange: ClosedFloatingPointRange<Float>) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        thumb = {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .shadow(12.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.height(16.dp).clip(RoundedCornerShape(50)),
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    )
}