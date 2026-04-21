package com.lalit.chitrakosha

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lalit.chitrakosha.data.MediaRepository
import com.lalit.chitrakosha.data.local.AppDatabase
import com.lalit.chitrakosha.ui.screens.favorites.FavoritesScreen
import androidx.compose.material.icons.filled.Favorite
import com.lalit.chitrakosha.ui.screens.albums.AlbumsScreen
import com.lalit.chitrakosha.ui.screens.detail.PhotoDetailScreen
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

                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    NavigationSuiteScaffold(
                        navigationSuiteItems = {
                            item(
                                selected = currentRoute == "gallery",
                                onClick = { 
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
                                        }
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
                                        }
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
                                        }
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
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Basic permission landing
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
