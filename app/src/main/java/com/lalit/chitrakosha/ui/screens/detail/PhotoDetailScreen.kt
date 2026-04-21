package com.lalit.chitrakosha.ui.screens.detail

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import coil3.compose.AsyncImage
import com.lalit.chitrakosha.ui.screens.gallery.GalleryUiState
import com.lalit.chitrakosha.ui.screens.gallery.GalleryViewModel
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoDetailScreen(
    photoId: Long,
    viewModel: GalleryViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit,
    deleteLauncher: ActivityResultLauncher<IntentSenderRequest>? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val metadata by viewModel.metadata.collectAsState()
    var showMetadata by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    val images = if (uiState is GalleryUiState.Success) {
        (uiState as GalleryUiState.Success).images
    } else emptyList()

    val initialIndex = remember(photoId, images) {
        images.indexOfFirst { it.id == photoId }.coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(initialPage = initialIndex) { images.size }

    // Track current photo for top bar updates
    val currentPhoto = if (images.isNotEmpty() && pagerState.currentPage < images.size) {
        images[pagerState.currentPage]
    } else null

    // Load metadata when current photo changes
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { page ->
                if (page < images.size) {
                    viewModel.loadMetadata(images[page].uri)
                }
            }
    }

    // Metadata bottom sheet
    if (showMetadata) {
        ModalBottomSheet(
            onDismissRequest = { showMetadata = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Photo Details",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                currentPhoto?.let { photo ->
                    MetadataRow("File Name", photo.displayName)
                }

                if (metadata.isNotEmpty()) {
                    metadata[ExifInterface.TAG_DATETIME]?.let {
                        MetadataRow("Date Taken", it)
                    }
                    val width = metadata[ExifInterface.TAG_IMAGE_WIDTH]
                    val height = metadata[ExifInterface.TAG_IMAGE_LENGTH]
                    if (width != null && height != null) {
                        MetadataRow("Resolution", "${width} × ${height}")
                    }
                    metadata[ExifInterface.TAG_MAKE]?.let {
                        MetadataRow("Camera Make", it)
                    }
                    metadata[ExifInterface.TAG_MODEL]?.let {
                        MetadataRow("Camera Model", it)
                    }
                    metadata[ExifInterface.TAG_EXPOSURE_TIME]?.let {
                        MetadataRow("Exposure", "${it}s")
                    }
                    metadata[ExifInterface.TAG_F_NUMBER]?.let {
                        MetadataRow("Aperture", "f/$it")
                    }
                    metadata[ExifInterface.TAG_ISO_SPEED_RATINGS]?.let {
                        MetadataRow("ISO", it)
                    }
                    metadata[ExifInterface.TAG_FOCAL_LENGTH]?.let {
                        MetadataRow("Focal Length", "${it}mm")
                    }
                    val lat = metadata[ExifInterface.TAG_GPS_LATITUDE]
                    val lon = metadata[ExifInterface.TAG_GPS_LONGITUDE]
                    if (lat != null && lon != null) {
                        MetadataRow("Location", "$lat, $lon")
                    }
                } else {
                    Text(
                        text = "No EXIF metadata available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    with(sharedTransitionScope) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        currentPhoto?.let {
                            Text(
                                text = it.displayName,
                                maxLines = 1,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (currentPhoto != null) {
                            // Info
                            IconButton(onClick = { showMetadata = true }) {
                                Icon(Icons.Default.Info, contentDescription = "Info")
                            }
                            // Favorite
                            IconButton(onClick = {
                                viewModel.toggleFavorite(currentPhoto.id, currentPhoto.isFavorite)
                            }) {
                                Icon(
                                    imageVector = if (currentPhoto.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (currentPhoto.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            // Share
                            IconButton(onClick = {
                                viewModel.sharePhotos(context, listOf(currentPhoto.uri))
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                            // Delete
                            IconButton(onClick = {
                                if (deleteLauncher != null) {
                                    viewModel.deletePhotos(context, listOf(currentPhoto.uri), deleteLauncher)
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                if (images.isNotEmpty()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        key = { images[it].id }
                    ) { page ->
                        val photo = images[page]
                        val isInitialPage = page == initialIndex

                        if (isInitialPage) {
                            ZoomableAsyncImage(
                                model = photo.uri,
                                contentDescription = photo.displayName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .sharedElement(
                                        rememberSharedContentState(key = "photo-${photo.id}"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                            )
                        } else {
                            ZoomableAsyncImage(
                                model = photo.uri,
                                contentDescription = photo.displayName,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(120.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 6.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}