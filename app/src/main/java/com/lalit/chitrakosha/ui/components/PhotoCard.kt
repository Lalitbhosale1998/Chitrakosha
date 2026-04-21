package com.lalit.chitrakosha.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.lalit.chitrakosha.data.MediaItem

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun PhotoCard(
    item: MediaItem,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongPress: () -> Unit = {},
    onToggleSelection: () -> Unit = {}
) {
    val selectionOverlayColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent,
        animationSpec = spring(),
        label = "selectionOverlay"
    )

    with(sharedTransitionScope) {
        Card(
            modifier = modifier
                .padding(4.dp)
                .aspectRatio(1f)
                .combinedClickable(
                    onClick = {
                        if (isSelectionMode) onToggleSelection() else onClick()
                    },
                    onLongClick = {
                        if (!isSelectionMode) onLongPress()
                    }
                )
                .sharedElement(
                    rememberSharedContentState(key = "photo-${item.id}"),
                    animatedVisibilityScope = animatedVisibilityScope
                ),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box {
                AsyncImage(
                    model = item.uri,
                    contentDescription = item.displayName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )

                // Selection overlay tint
                if (isSelectionMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = selectionOverlayColor,
                        content = {}
                    )
                }

                // Favorite badge (bottom-end)
                if (item.isFavorite && !isSelectionMode) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        tint = Color.Red
                    )
                }

                // Selection checkbox (top-start)
                if (isSelectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = if (isSelected) "Selected" else "Not selected",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .size(24.dp),
                        tint = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}