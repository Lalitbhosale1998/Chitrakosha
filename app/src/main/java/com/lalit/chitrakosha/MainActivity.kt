package com.lalit.chitrakosha

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lalit.chitrakosha.data.MediaRepository
import com.lalit.chitrakosha.data.local.AppDatabase
import com.lalit.chitrakosha.ui.screens.albums.AlbumsScreen
import com.lalit.chitrakosha.ui.screens.detail.PhotoDetailScreen
import com.lalit.chitrakosha.ui.screens.favorites.FavoritesScreen
import com.lalit.chitrakosha.ui.screens.gallery.GalleryScreen
import com.lalit.chitrakosha.ui.screens.gallery.GalleryViewModel
import com.lalit.chitrakosha.ui.screens.gallery.GalleryViewModelFactory
import com.lalit.chitrakosha.ui.theme.ChitrakoshaTheme

class MainActivity : ComponentActivity() {

    private var hasPermission by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasPermission = isGranted
    }

    private val deleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Deletion was successful — ViewModel will refresh
            viewModelInstance?.onDeleteCompleted()
        }
    }

    private var viewModelInstance: GalleryViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkPermission()

        setContent {
            ChitrakoshaTheme {
                if (hasPermission) {
                    val navController = rememberNavController()
                    val context = applicationContext
                    val database = remember { AppDatabase.getDatabase(context) }
                    val repository = remember<MediaRepository> { MediaRepository(context) }
                    val viewModel: GalleryViewModel = viewModel(
                        factory = GalleryViewModelFactory(repository, database.favoriteDao())
                    )
                    viewModelInstance = viewModel

                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    NavigationSuiteScaffold(
                        navigationSuiteItems = {
                            item(
                                selected = currentRoute == "gallery",
                                onClick = {
                                    viewModel.clearSelection()
                                    navController.navigate("gallery") {
                                        popUpTo("gallery") { inclusive = true }
                                    }
                                },
                                icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                                label = { Text("Gallery") }
                            )
                            item(
                                selected = currentRoute == "albums",
                                onClick = {
                                    viewModel.clearSelection()
                                    navController.navigate("albums") {
                                        launchSingleTop = true
                                    }
                                },
                                icon = { Icon(Icons.Default.Collections, contentDescription = null) },
                                label = { Text("Albums") }
                            )
                            item(
                                selected = currentRoute == "favorites",
                                onClick = {
                                    viewModel.clearSelection()
                                    navController.navigate("favorites") {
                                        launchSingleTop = true
                                    }
                                },
                                icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                                label = { Text("Favorites") }
                            )
                        }
                    ) {
                        SharedTransitionLayout {
                            NavHost(navController = navController, startDestination = "gallery") {
                                composable("gallery") {
                                    viewModel.selectAlbum(null)
                                    GalleryScreen(
                                        viewModel = viewModel,
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = this@composable,
                                        onPhotoClick = { photoId ->
                                            navController.navigate("detail/$photoId")
                                        },
                                        deleteLauncher = deleteLauncher
                                    )
                                }
                                composable("albums") {
                                    AlbumsScreen(
                                        viewModel = viewModel,
                                        onAlbumClick = { bucketId, name ->
                                            navController.navigate("album_detail/$bucketId/$name")
                                        }
                                    )
                                }
                                composable("favorites") {
                                    FavoritesScreen(
                                        viewModel = viewModel,
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = this@composable,
                                        onPhotoClick = { photoId ->
                                            navController.navigate("detail/$photoId")
                                        },
                                        deleteLauncher = deleteLauncher
                                    )
                                }
                                composable(
                                    route = "album_detail/{bucketId}/{name}",
                                    arguments = listOf(
                                        navArgument("bucketId") { type = NavType.LongType },
                                        navArgument("name") { type = NavType.StringType }
                                    )
                                ) { backStackEntry ->
                                    val bucketId = backStackEntry.arguments?.getLong("bucketId") ?: return@composable
                                    val name = backStackEntry.arguments?.getString("name") ?: ""
                                    viewModel.selectAlbum(bucketId)
                                    GalleryScreen(
                                        viewModel = viewModel,
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = this@composable,
                                        albumName = name,
                                        onBack = { navController.popBackStack() },
                                        onPhotoClick = { photoId ->
                                            navController.navigate("detail/$photoId")
                                        },
                                        deleteLauncher = deleteLauncher
                                    )
                                }
                                composable(
                                    route = "detail/{photoId}",
                                    arguments = listOf(navArgument("photoId") { type = NavType.LongType })
                                ) { backStackEntry ->
                                    val photoId = backStackEntry.arguments?.getLong("photoId") ?: return@composable
                                    PhotoDetailScreen(
                                        photoId = photoId,
                                        viewModel = viewModel,
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = this@composable,
                                        onBack = { navController.popBackStack() },
                                        deleteLauncher = deleteLauncher
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Permission denied UI
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(96.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Welcome to Chitrakosha",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Chitrakosha needs access to your photos and videos to display your gallery. Your media stays on your device.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { checkPermission() }
                        ) {
                            Text("Grant Access")
                        }
                    }
                }
            }
        }
    }

    private fun checkPermission() {
        val permission = Manifest.permission.READ_MEDIA_IMAGES

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            hasPermission = true
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }
}
