package com.esfersoft.imageuploader.presentation.ui

import android.Manifest
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
// stickyHeader is a LazyListScope extension; no direct import required
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.esfersoft.imageuploader.domain.ImageEntity
import com.esfersoft.imageuploader.domain.ImageMetadataEntity
import com.esfersoft.imageuploader.domain.SelectionMode
import com.esfersoft.imageuploader.presentation.GalleryViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import kotlin.math.pow
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalPermissionsApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onChoosePhotos: (() -> Unit)? = null,
    onRefreshRequested: (() -> Unit)? = null
) {
    val images by viewModel.images.collectAsState()
    val mode by viewModel.selectionMode.collectAsState()
    val selected by viewModel.selectedUris.collectAsState()
    val metadata by viewModel.metadataMap.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val resume by viewModel.resumePrompt.collectAsState()
    val events = viewModel.events

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else Manifest.permission.READ_EXTERNAL_STORAGE

    val permissionState = rememberPermissionState(permission)
    val scope = rememberCoroutineScope()
    var asked by remember { mutableStateOf(false) }
    LaunchedEffect(permissionState.status.isGranted) {
        if (!permissionState.status.isGranted && !asked) {
            asked = true
            permissionState.launchPermissionRequest()
        } else if (permissionState.status.isGranted) {
            scope.launch { viewModel.refreshImages() }
        }
    }

    var cellTarget by remember { mutableStateOf(120f) }
    val cellSize by animateFloatAsState(targetValue = cellTarget, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "cellSizeAnim")
    Scaffold(
        topBar = {
            TopBar(
                mode = mode,
                onToggleMode = { viewModel.toggleSelectionMode() },
                onUpload = { viewModel.uploadSelectedWithRetry() },
                selectionCount = selected.size,
                resumeCount = resume.size,
                onResume = { viewModel.resumeSavedUploads() },
                onChoose = onChoosePhotos,
                onRefresh = {
                    if (onRefreshRequested != null) onRefreshRequested() else scope.launch { viewModel.refreshImages() }
                }
            )
        }
    ) { inner ->
    Column(modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 8.dp)) {

        val sections = remember(images) { buildMonthSections(images) }
        val listState = rememberLazyListState()
        Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val smooth = zoom.toFloat().pow(0.4f)
                        cellTarget = (cellTarget * smooth).coerceIn(80f, 240f)
                    }
                },
            state = listState
        ) {
            sections.forEach { section ->
                stickyHeader { MonthHeader(title = section.title) }
                item {
                    FlowRow(maxItemsInEachRow = Int.MAX_VALUE) {
                        section.items.forEach { img ->
                            val isSelected = selected.contains(img.uri)
                            MediaTile(
                                img = img,
                                isSelected = isSelected,
                                mode = mode,
                                cellSize = cellSize,
                                metadataCaption = metadata[img.uri]?.caption ?: "",
                                progress = progress[img.uri]?.progress ?: 0f,
                                onClick = { if (mode == SelectionMode.MULTI) viewModel.toggleSelect(img.uri) },
                                onLongClick = {
                                    viewModel.enterSelectionMode()
                                    viewModel.toggleSelect(img.uri)
                                },
                                onCaptionChange = { viewModel.setMetadata(img.uri, ImageMetadataEntity(caption = it)) }
                            )
                        }
                    }
                }
            }
        }
        // Floating month pill while scrolling
        val currentSectionTitle by remember(sections, listState.firstVisibleItemIndex) {
            mutableStateOf(
                if (sections.isEmpty()) "" else {
                    val i = listState.firstVisibleItemIndex
                    val sec = (i / 2).coerceIn(0, sections.lastIndex)
                    sections[sec].title
                }
            )
        }
        if (listState.isScrollInProgress && currentSectionTitle.isNotEmpty()) {
            MonthPill(currentSectionTitle, Modifier.align(Alignment.TopCenter).padding(top = 12.dp))
        }

        // Optional inline zoom slider for precision with gradient container
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .background(brush = Brush.verticalGradient(listOf(Color(0x660093E9), Color(
                    0xFF6A1B9A
                )
                )), shape = MaterialTheme.shapes.large)
        ) {
            ZoomSlider(value = cellTarget, onValueChange = { cellTarget = it }, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
        }

        // Draggable fast scroller thumb
        DraggableFastScroller(
            listState = listState,
            sectionCount = sections.size,
            onSnapToSection = { secIdx ->
                val headerIndex = (secIdx * 2).coerceAtMost(listState.layoutInfo.totalItemsCount - 1)
                scope.launch { listState.scrollToItem(headerIndex) }
            },
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(16.dp)
        )
        }
    }
    }

    // Toasts for upload events
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        events.collect { msg -> android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show() }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TopBar(
    mode: SelectionMode,
    onToggleMode: () -> Unit,
    onUpload: () -> Unit,
    selectionCount: Int,
    resumeCount: Int,
    onResume: () -> Unit,
    onChoose: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null
) {
    val isMulti = mode == SelectionMode.MULTI

    LargeTopAppBar(
        title = {
            Text(
                if (isMulti) "Selected: $selectionCount" else "Gallery",
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            if (resumeCount > 0) {
                AssistChip(onClick = onResume, label = { Text("Resume $resumeCount") })
            }
        },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (mode == SelectionMode.SINGLE) "Single" else "Multi",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(6.dp))
                Switch(
                    checked = mode == SelectionMode.MULTI,
                    onCheckedChange = { onToggleMode() }
                )
                Spacer(Modifier.width(8.dp))
                onChoose?.let {
                    IconButton(onClick = it) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Choose",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                onRefresh?.let {
                    IconButton(onClick = it) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Button(onClick = onUpload, enabled = selectionCount > 0) {
                    Text("Upload")
                }
            }
        },
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier.background(
            brush = Brush.verticalGradient(
                colors = if (isSystemInDarkTheme()) {
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                        Color.Black.copy(alpha = 0.6f)  // subtle dark fade
                    )
                } else {
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                        Color.White.copy(alpha = 0.7f)  // subtle light fade
                    )
                }
            )
        )
    )
}


@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MediaTile(
    img: ImageEntity,
    isSelected: Boolean,
    mode: SelectionMode,
    cellSize: Float,
    metadataCaption: String,
    progress: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCaptionChange: (String) -> Unit
) {
    val aspect = remember(img.id) { when ((img.id % 3).toInt()) { 0 -> 1.8f; 1 -> 1.2f; else -> 1.0f } }
    Box(
        modifier = Modifier
            .padding(4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column {
            Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(), elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)) {
                Box(modifier = Modifier
                    .width(cellSize.dp)
                    .height((cellSize * aspect).dp)
                    .graphicsLayer { if (isSelected) { scaleX = 0.98f; scaleY = 0.98f } }
                ) {
                    AsyncImage(
                        model = img.uri,
                        contentDescription = img.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    val showCheck = mode == SelectionMode.MULTI
                    if (showCheck) {
                        val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                        Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = tint, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).alpha(if (isSelected) 1f else 0.6f))
                    }
                // Show progress bar only while uploading
                if (progress > 0f && progress < 1f) {
                    LinearProgressIndicator(progress = progress.coerceIn(0f, 1f), modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth())
                }
                }
            }
            if (isSelected) {
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = metadataCaption,
                    onValueChange = onCaptionChange,
                    modifier = Modifier.width(cellSize.dp),
                    placeholder = { Text("Caption") },
                    singleLine = true
                )
            }
        }
    }
}

private data class MonthKey(val year: Int, val month: Int, val title: String)

private fun monthKey(epochSeconds: Long): MonthKey {
    val zdt = Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault())
    val title = DateTimeFormatter.ofPattern("MMMM yyyy").format(zdt)
    return MonthKey(zdt.year, zdt.monthValue, title)
}

private data class MonthSection(val title: String, val items: List<ImageEntity>)

private fun buildMonthSections(images: List<ImageEntity>): List<MonthSection> {
    return images.groupBy { monthKey(it.dateAddedEpochSeconds) }
        .toSortedMap(compareByDescending<MonthKey> { it.year }.thenByDescending { it.month })
        .map { (k, v) -> MonthSection(k.title, v) }
}

@Composable
private fun MonthHeader(title: String) {
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), tonalElevation = 2.dp) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 12.dp))
    }
}

@Composable
private fun MonthPill(title: String, modifier: Modifier = Modifier) {
    Surface(shape = MaterialTheme.shapes.large, tonalElevation = 4.dp, modifier = modifier) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FastScroller(
    modifier: Modifier = Modifier,
    sectionCount: Int,
    onScrollToSection: (Int) -> Unit
) {
    if (sectionCount <= 1) return
    Column(modifier = modifier.padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        repeat(sectionCount) { idx ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .padding(vertical = 6.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f), shape = MaterialTheme.shapes.small)
                    .combinedClickable(onClick = { onScrollToSection(idx) }, onLongClick = { onScrollToSection(idx) })
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DraggableFastScroller(
    listState: androidx.compose.foundation.lazy.LazyListState,
    sectionCount: Int,
    onSnapToSection: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (sectionCount <= 1) return
    var dragY by remember { mutableStateOf(0f) }
    var containerHeight by remember { mutableStateOf(1f) }
    val thumbHeight = 48.dp
    Box(
        modifier = modifier
            .onGloballyPositioned { containerHeight = it.size.height.toFloat() }
    ) {
        // Track
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)))
        // Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(x = 0, y = (dragY - thumbHeight.toPx() / 2f).coerceAtLeast(0f).toInt()) }
                .size(width = 28.dp, height = thumbHeight)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), shape = MaterialTheme.shapes.large)
                .pointerInput(sectionCount, containerHeight) {
                    detectTransformGestures { centroid, _, _, _ ->
                        dragY = centroid.y
                        val ratio = (dragY / containerHeight).coerceIn(0f, 0.999f)
                        val index = (ratio * sectionCount).toInt()
                        onSnapToSection(index)
                    }
                }
        )
    }
}

@Composable
private fun ZoomSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    Surface(shape = MaterialTheme.shapes.large, tonalElevation = 2.dp, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text("Zoom", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.width(8.dp))
            Slider(value = value, onValueChange = onValueChange, valueRange = 80f..240f, modifier = Modifier.width(180.dp))
        }
    }
}

@Composable
private fun ResumeUploadsBanner(count: Int, onResume: () -> Unit) {
    Surface(color = Color(0xFFFFF3E0), tonalElevation = 2.dp, shadowElevation = 2.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Text("Pending uploads: $count", modifier = Modifier.weight(1f))
            Button(onClick = onResume) { Text("Resume") }
        }
    }
}


