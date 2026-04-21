package com.lalit.chitrakosha.ui.screens.gallery

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
    
    val uiState: StateFlow<GalleryUiState> = combine(
        _mediaItems,
        favoriteDao.getAllFavoriteIds()
    ) { items, favoriteIds ->
        if (items.isEmpty()) {
            GalleryUiState.Loading
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

    val filteredGalleryState: StateFlow<GalleryUiState> = combine(uiState, _selectedAlbumId) { state, albumId ->
        if (state is GalleryUiState.Success && albumId != null) {
            GalleryUiState.Success(state.images.filter { it.bucketId == albumId })
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
            } catch (e: Exception) {
                // In a real app, handle error state via another flow
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
}

sealed interface GalleryUiState {
    object Loading : GalleryUiState
    data class Success(val images: List<MediaItem>) : GalleryUiState
    data class Error(val message: String) : GalleryUiState
}

sealed interface AlbumsUiState {
    object Loading : AlbumsUiState
    data class Success(val albums: List<Album>) : AlbumsUiState
    data class Error(val message: String) : AlbumsUiState
}
