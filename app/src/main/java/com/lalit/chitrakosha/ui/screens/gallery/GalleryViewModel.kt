package com.lalit.chitrakosha.ui.screens.gallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lalit.chitrakosha.data.Album
import com.lalit.chitrakosha.data.MediaItem
import com.lalit.chitrakosha.data.MediaRepository
import com.lalit.chitrakosha.data.local.FavoriteDao
import com.lalit.chitrakosha.data.local.FavoriteEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GalleryViewModel(
    private val repository: MediaRepository,
    private val favoriteDao: FavoriteDao
) : ViewModel() {

    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _hasLoaded = MutableStateFlow(false)

    val uiState: StateFlow<GalleryUiState> = combine(
        _mediaItems,
        _hasLoaded,
        favoriteDao.getAllFavoriteIds()
    ) { items, hasLoaded, favoriteIds ->
        if (!hasLoaded) {
            GalleryUiState.Loading
        } else if (items.isEmpty()) {
            GalleryUiState.Empty
        } else {
            val favoriteIdSet = favoriteIds.toSet()
            val itemsWithFavorites = items.map { it.copy(isFavorite = favoriteIdSet.contains(it.id)) }
            GalleryUiState.Success(itemsWithFavorites)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GalleryUiState.Loading)

    private val _albumSearchQuery = MutableStateFlow("")
    val albumSearchQuery = _albumSearchQuery.asStateFlow()

    private val _selectedAlbumId = MutableStateFlow<Long?>(null)
    val selectedAlbumId = _selectedAlbumId.asStateFlow()

    // --- Selection State ---
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    val isSelectionMode: StateFlow<Boolean> = combine(_selectedIds) { ids ->
        ids.first().isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleSelection(id: Long) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (contains(id)) remove(id) else add(id)
        }
    }

    fun selectAll() {
        val currentState = uiState.value
        if (currentState is GalleryUiState.Success) {
            _selectedIds.value = currentState.images.map { it.id }.toSet()
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    // --- Filtered Gallery State ---
    val filteredGalleryState: StateFlow<GalleryUiState> = combine(uiState, _selectedAlbumId) { state, albumId ->
        if (state is GalleryUiState.Success && albumId != null) {
            val filtered = state.images.filter { it.bucketId == albumId }
            if (filtered.isEmpty()) GalleryUiState.Empty else GalleryUiState.Success(filtered)
        } else {
            state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GalleryUiState.Loading)

    val albumsState: StateFlow<AlbumsUiState> = combine(uiState, _albumSearchQuery) { state, query ->
        if (state is GalleryUiState.Success) {
            val albums = state.images.groupBy { it.bucketId }
                .map { (id, items) ->
                    Album(
                        id = id,
                        name = items.first().bucketName,
                        coverItem = items.first(),
                        itemCount = items.size
                    )
                }
                .filter { it.name.contains(query, ignoreCase = true) }
                .sortedBy { it.name }
            AlbumsUiState.Success(albums)
        } else {
            AlbumsUiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlbumsUiState.Loading)

    init {
        loadImages()
    }

    fun loadImages() {
        viewModelScope.launch {
            try {
                val images = repository.fetchImages()
                _mediaItems.value = images
                _hasLoaded.value = true
            } catch (e: Exception) {
                _hasLoaded.value = true
                _mediaItems.value = emptyList()
            }
        }
    }

    fun toggleFavorite(mediaId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            if (isFavorite) {
                favoriteDao.deleteFavorite(FavoriteEntity(mediaId))
            } else {
                favoriteDao.insertFavorite(FavoriteEntity(mediaId))
            }
        }
    }

    private val _metadata = MutableStateFlow<Map<String, String>>(emptyMap())
    val metadata = _metadata.asStateFlow()

    fun loadMetadata(uri: android.net.Uri) {
        viewModelScope.launch {
            _metadata.value = repository.getExifMetadata(uri)
        }
    }

    fun clearMetadata() {
        _metadata.value = emptyMap()
    }

    fun setAlbumSearchQuery(query: String) {
        _albumSearchQuery.value = query
    }

    fun selectAlbum(albumId: Long?) {
        _selectedAlbumId.value = albumId
    }

    // --- Share ---
    fun sharePhotos(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        context.startActivity(Intent.createChooser(intent, "Share via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    // --- Delete ---
    fun deletePhotos(
        context: Context,
        uris: List<Uri>,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        if (uris.isEmpty()) return
        val pendingIntent = MediaStore.createDeleteRequest(
            context.contentResolver,
            uris
        )
        val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
        launcher.launch(intentSenderRequest)
    }

    fun onDeleteCompleted() {
        clearSelection()
        loadImages()
    }
}

sealed interface GalleryUiState {
    data object Loading : GalleryUiState
    data object Empty : GalleryUiState
    data class Success(val images: List<MediaItem>) : GalleryUiState
    data class Error(val message: String) : GalleryUiState
}

sealed interface AlbumsUiState {
    data object Loading : AlbumsUiState
    data class Success(val albums: List<Album>) : AlbumsUiState
    data class Error(val message: String) : AlbumsUiState
}
