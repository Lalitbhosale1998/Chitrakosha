package com.lalit.chitrakosha.data

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAdded: Long,
    val size: Long,
    val bucketId: Long,
    val bucketName: String,
    val isFavorite: Boolean = false
)