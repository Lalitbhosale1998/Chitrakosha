package com.lalit.chitrakosha.ui.screens.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lalit.chitrakosha.data.MediaRepository

import com.lalit.chitrakosha.data.local.FavoriteDao

class GalleryViewModelFactory(
    private val repository: MediaRepository,
    private val favoriteDao: FavoriteDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GalleryViewModel(repository, favoriteDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}