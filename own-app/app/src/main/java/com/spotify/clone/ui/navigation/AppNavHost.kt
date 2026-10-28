package com.spotify.clone.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.spotify.clone.core.data.AudioRepository
import com.spotify.clone.core.designsystem.SpotifyBlack
import com.spotify.clone.core.player.MusicController
import com.spotify.clone.ui.artist.ArtistDetailScreen
import com.spotify.clone.ui.components.MiniPlayerBar
import com.spotify.clone.ui.components.NavigationTab
import com.spotify.clone.ui.components.SpotifyBottomNavBar
import com.spotify.clone.ui.home.HomeScreen
import com.spotify.clone.ui.home.HomeViewModel
import com.spotify.clone.ui.library.LibraryScreen
import com.spotify.clone.ui.library.LibraryViewModel
import com.spotify.clone.ui.player.NowPlayingScreen
import com.spotify.clone.ui.playlist.PlaylistDetailScreen
import com.spotify.clone.ui.profile.ProfileScreen
import com.spotify.clone.ui.search.SearchScreen
import com.spotify.clone.ui.search.SearchViewModel
import com.spotify.clone.ui.settings.SettingsScreen
import com.spotify.clone.ui.splash.SplashScreen

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val musicController = remember { MusicController.getInstance(context) }
    val repository = remember { AudioRepository(context) }

    var isPlayerExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = isPlayerExpanded) {
        isPlayerExpanded = false
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Splash.route

    val homeViewModel: HomeViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel()
    val libraryViewModel: LibraryViewModel = viewModel()

    val isReducedMotion = com.spotify.clone.core.designsystem.LocalReducedMotion.current
    var homeScrollTrigger by remember { mutableStateOf(0L) }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 1. Docked Mini-Player Bar (Isolated sub-tree to avoid root recompositions on progress ticks)
                    if (currentRoute != Screen.Splash.route) {
                        MiniPlayerBarHost(
                            musicController = musicController,
                            onExpand = { isPlayerExpanded = true }
                        )
                    }

                    // 2. Bottom Navigation Bar (Hidden in full-screen Splash, Settings, and Profile views)
                    val showBottomNav = currentRoute !in listOf(Screen.Splash.route, Screen.Profile.route, Screen.Settings.route)
                    if (showBottomNav) {
                        SpotifyBottomNavBar(
                            currentRoute = currentRoute,
                            onTabSelected = { tab ->
                                when (tab) {
                                    NavigationTab.HOME -> {
                                        if (currentRoute == Screen.Home.route) {
                                            // Reselecting Home: scroll back smoothly to top
                                            homeScrollTrigger = System.currentTimeMillis()
                                        } else {
                                            // From any screen (Playlist, Artist, Search, Library), navigate straight Home
                                            navController.navigate(Screen.Home.route) {
                                                popUpTo(Screen.Home.route) {
                                                    inclusive = false
                                                }
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                    NavigationTab.SEARCH -> {
                                        if (currentRoute != Screen.Search.route) {
                                            navController.navigate(Screen.Search.route) {
                                                popUpTo(Screen.Home.route) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                    NavigationTab.LIBRARY -> {
                                        if (currentRoute != Screen.Library.route) {
                                            navController.navigate(Screen.Library.route) {
                                                popUpTo(Screen.Home.route) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable(Screen.Splash.route) {
                    SplashScreen(
                        onSplashFinished = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Splash.route) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onPlaylistClick = { id ->
                            navController.navigate(Screen.PlaylistDetail.createRoute(id))
                        },
                        onArtistClick = { artistName ->
                            navController.navigate(Screen.ArtistDetail.createRoute(artistName))
                        },
                        onProfileClick = { navController.navigate(Screen.Profile.route) },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) },
                        onSearchClick = { navController.navigate(Screen.Search.route) },
                        scrollToTopTrigger = homeScrollTrigger
                    )
                }

                composable(Screen.Search.route) {
                    SearchScreen(
                        viewModel = searchViewModel,
                        onArtistClick = { artistName ->
                            navController.navigate(Screen.ArtistDetail.createRoute(artistName))
                        },
                        onPlaylistClick = { playlistId ->
                            navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                        }
                    )
                }

                composable(Screen.Library.route) {
                    LibraryScreen(
                        viewModel = libraryViewModel,
                        onPlaylistClick = { id ->
                            navController.navigate(Screen.PlaylistDetail.createRoute(id))
                        },
                        onProfileClick = { navController.navigate(Screen.Profile.route) }
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onBack = { navController.popBackStack() },
                        onSettingsClick = {
                            navController.navigate(Screen.Settings.route) {
                                launchSingleTop = true
                            }
                        },
                        onPlaylistClick = { id ->
                            navController.navigate(Screen.PlaylistDetail.createRoute(id))
                        },
                        onArtistClick = { artistName ->
                            navController.navigate(Screen.ArtistDetail.createRoute(artistName))
                        }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onProfileClick = {
                            navController.navigate(Screen.Profile.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(
                    route = Screen.PlaylistDetail.route,
                    arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
                    PlaylistDetailScreen(
                        playlistId = playlistId,
                        onBack = { navController.popBackStack() },
                        repository = repository,
                        musicController = musicController,
                        onArtistClick = { artistName ->
                            navController.navigate(Screen.ArtistDetail.createRoute(artistName))
                        }
                    )
                }

                composable(
                    route = Screen.ArtistDetail.route,
                    arguments = listOf(navArgument("artistName") { type = NavType.StringType })
                ) { backStackEntry ->
                    val encodedName = backStackEntry.arguments?.getString("artistName") ?: ""
                    val artistName = try {
                        java.net.URLDecoder.decode(encodedName, "UTF-8")
                    } catch (e: Exception) {
                        encodedName
                    }
                    ArtistDetailScreen(
                        artistName = artistName,
                        onBack = { navController.popBackStack() },
                        repository = repository,
                        musicController = musicController
                    )
                }
            }
        }

        // Fullscreen Now Playing Overlay with Coordinated Motion
        AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = if (isReducedMotion) androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(0))
            else (slideInVertically(animationSpec = com.spotify.clone.core.designsystem.EchoMotion.springGentle()) { it } +
                    androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(com.spotify.clone.core.designsystem.EchoMotion.DurationStandard))),
            exit = if (isReducedMotion) androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(0))
            else (slideOutVertically(animationSpec = com.spotify.clone.core.designsystem.EchoMotion.springSnappy()) { it } +
                    androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(com.spotify.clone.core.designsystem.EchoMotion.DurationStandard)))
        ) {
            NowPlayingScreen(
                musicController = musicController,
                onCollapse = { isPlayerExpanded = false },
                onViewArtist = { artistName ->
                    isPlayerExpanded = false
                    navController.navigate(Screen.ArtistDetail.createRoute(artistName))
                }
            )
        }
    }
}

@Composable
private fun MiniPlayerBarHost(
    musicController: MusicController,
    onExpand: () -> Unit
) {
    val playbackState by musicController.playbackState.collectAsState()
    if (playbackState.currentTrack != null) {
        MiniPlayerBar(
            playbackState = playbackState,
            onTogglePlayPause = { musicController.togglePlayPause() },
            onToggleFavorite = {
                playbackState.currentTrack?.let { track ->
                    musicController.toggleFavorite(track)
                }
            },
            onSkipNext = { musicController.skipNext() },
            onClick = onExpand
        )
    }
}
