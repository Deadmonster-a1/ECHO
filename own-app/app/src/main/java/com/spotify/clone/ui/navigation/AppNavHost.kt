package com.spotify.clone.ui.navigation

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.spotify.clone.core.data.AudioRepository
import com.spotify.clone.core.data.SampleCatalog
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.player.MusicController
import com.spotify.clone.core.profile.UserProfileManager
import com.spotify.clone.core.update.AppUpdateManager
import com.spotify.clone.core.update.UpdateInfo
import com.spotify.clone.ui.artist.ArtistDetailScreen
import com.spotify.clone.ui.components.AppUpdateDialog
import com.spotify.clone.ui.components.EchoBottomNavBar
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.spotify.clone.ui.components.MiniPlayerBar
import com.spotify.clone.ui.components.NavigationTab
import com.spotify.clone.ui.home.HomeEvent
import com.spotify.clone.ui.home.HomeScreen
import com.spotify.clone.ui.home.HomeViewModel
import com.spotify.clone.ui.library.LibraryScreen
import com.spotify.clone.ui.library.LibraryViewModel
import com.spotify.clone.ui.onboarding.OnboardingScreen
import com.spotify.clone.ui.player.NowPlayingScreen
import com.spotify.clone.ui.playlist.PlaylistDetailScreen
import com.spotify.clone.ui.search.SearchScreen
import com.spotify.clone.ui.search.SearchViewModel
import com.spotify.clone.ui.settings.SettingsScreen
import java.net.URLDecoder

@Composable
fun AppNavHost(
    pendingIntent: Intent? = null,
    onIntentHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val musicController = remember { MusicController.getInstance(context) }
    val repository = remember { AudioRepository(context) }
    val isReducedMotion = LocalReducedMotion.current

    var isPlayerExpanded by remember { mutableStateOf(false) }
    var homeScrollTrigger by remember { mutableLongStateOf(0L) }
    var searchScrollTrigger by remember { mutableLongStateOf(0L) }
    var libraryScrollTrigger by remember { mutableLongStateOf(0L) }
    var availableLiveUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var hasCheckedUpdateInSession by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !hasCheckedUpdateInSession) {
                hasCheckedUpdateInSession = true
                coroutineScope.launch {
                    runCatching {
                        val installedVer = AppUpdateManager.getInstalledVersionName(context)
                        val result = AppUpdateManager.checkForUpdates(context, installedVer)
                        result.getOrNull()?.let { update ->
                            availableLiveUpdate = update
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = isPlayerExpanded) {
        isPlayerExpanded = false
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route
    val tabRoutes = remember { setOf(Screen.Home.route, Screen.Search.route, Screen.Library.route) }
    var activeTabRoute by remember { mutableStateOf(NavigationTab.HOME.route) }
    LaunchedEffect(currentRoute) {
        if (currentRoute in tabRoutes) {
            activeTabRoute = currentRoute
        }
    }

    val homeViewModel: HomeViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel()
    val libraryViewModel: LibraryViewModel = viewModel()

    fun navigateToTab(tab: NavigationTab) {
        isPlayerExpanded = false
        activeTabRoute = tab.route
        val activeRoute = navController.currentDestination?.route
        if (activeRoute == tab.route) {
            when (tab) {
                NavigationTab.HOME -> homeScrollTrigger = System.currentTimeMillis()
                NavigationTab.SEARCH -> searchScrollTrigger = System.currentTimeMillis()
                NavigationTab.LIBRARY -> libraryScrollTrigger = System.currentTimeMillis()
            }
            return
        }
        val hasHomeInGraph = runCatching { navController.getBackStackEntry(Screen.Home.route) }.isSuccess
        if (!hasHomeInGraph) {
            navController.navigate(Screen.Home.route) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
            if (tab == NavigationTab.HOME) return
        }
        navController.navigate(tab.route) {
            popUpTo(Screen.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToPlaylist(id: String) {
        val cleanId = id.trim()
        if (cleanId.isNotEmpty()) {
            navController.navigate(Screen.PlaylistDetail.createRoute(cleanId)) {
                launchSingleTop = true
            }
        }
    }

    fun navigateToArtist(name: String) {
        val cleanName = name.trim()
        if (cleanName.isNotEmpty()) {
            navController.navigate(Screen.ArtistDetail.createRoute(cleanName)) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(pendingIntent) {
        val intent = pendingIntent ?: return@LaunchedEffect
        try {
            when (intent.getStringExtra("nav_tab")) {
                "tab_library" -> navigateToTab(NavigationTab.LIBRARY)
                "tab_search" -> navigateToTab(NavigationTab.SEARCH)
                "tab_home" -> navigateToTab(NavigationTab.HOME)
            }

            val dataUri = intent.data
            if (dataUri != null && dataUri.scheme == "echo") {
                val (linkType, decodedArg) = if (!dataUri.host.isNullOrBlank()) {
                    val host = dataUri.host!!.lowercase()
                    val firstSeg = dataUri.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }
                    val seg = firstSeg ?: run {
                        val raw = dataUri.schemeSpecificPart.substringAfter(':', "").substringAfter('/', "")
                        runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)
                    }
                    host to seg
                } else {
                    val parts = dataUri.schemeSpecificPart.trimStart('/').split(":", limit = 2)
                    val rawType = parts.getOrNull(0)?.lowercase() ?: ""
                    val rawArg = parts.getOrNull(1) ?: ""
                    val arg = runCatching { URLDecoder.decode(rawArg, "UTF-8") }.getOrDefault(rawArg)
                    rawType to arg
                }
                when (linkType) {
                    "track" -> if (decodedArg.isNotBlank()) {
                        val track = repository.getTrackById(decodedArg)
                            ?: repository.search(decodedArg.replace("_", " ")).firstOrNull()
                        if (track != null) {
                            musicController.playTrack(track)
                            isPlayerExpanded = true
                        } else {
                            Toast.makeText(context, "Track not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "artist" -> if (decodedArg.isNotBlank()) {
                        isPlayerExpanded = false
                        navigateToArtist(decodedArg)
                    }
                    "playlist" -> if (decodedArg.isNotBlank()) {
                        isPlayerExpanded = false
                        navigateToPlaylist(decodedArg)
                    }
                    "library" -> navigateToTab(NavigationTab.LIBRARY)
                    "search" -> navigateToTab(NavigationTab.SEARCH)
                    "home" -> navigateToTab(NavigationTab.HOME)
                }
            }
        } finally {
            onIntentHandled()
        }
    }

    val screenBg = MaterialTheme.colorScheme.background

    val startRoute = remember {
        if (UserProfileManager.hasCompletedOnboarding.value) Screen.Home.route else Screen.Onboarding.route
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        // Content extends edge-to-edge behind the floating functional chrome (Apple HIG)
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                val isTabSwitch = targetState.destination.route in tabRoutes && initialState.destination.route in tabRoutes
                if (isReducedMotion || isTabSwitch) {
                    fadeIn(tween(150, easing = EchoMotion.EaseOutStrong))
                } else {
                    slideInHorizontally(tween(260, easing = EchoMotion.EaseOutStrong)) { it / 5 } +
                        fadeIn(tween(220, easing = EchoMotion.EaseOutStrong))
                }
            },
            exitTransition = {
                val isTabSwitch = targetState.destination.route in tabRoutes && initialState.destination.route in tabRoutes
                if (isReducedMotion || isTabSwitch) {
                    fadeOut(tween(120))
                } else {
                    slideOutHorizontally(tween(220, easing = EchoMotion.EaseOutStrong)) { -it / 8 } +
                        fadeOut(tween(180))
                }
            },
            popEnterTransition = {
                if (isReducedMotion) {
                    fadeIn(tween(150))
                } else {
                    slideInHorizontally(tween(240, easing = EchoMotion.EaseOutStrong)) { -it / 8 } +
                        fadeIn(tween(200, easing = EchoMotion.EaseOutStrong))
                }
            },
            popExitTransition = {
                if (isReducedMotion) {
                    fadeOut(tween(120))
                } else {
                    slideOutHorizontally(tween(220, easing = EchoMotion.EaseOutStrong)) { it / 4 } +
                        fadeOut(tween(180))
                }
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onPlaylistClick = { id -> navigateToPlaylist(id) },
                    onArtistClick = { name -> navigateToArtist(name) },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route) { launchSingleTop = true }
                    },
                    scrollToTopTrigger = homeScrollTrigger
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = searchViewModel,
                    onArtistClick = { name -> navigateToArtist(name) },
                    onPlaylistClick = { id -> navigateToPlaylist(id) },
                    scrollToTopTrigger = searchScrollTrigger
                )
            }

            composable(Screen.Library.route) {
                LibraryScreen(
                    viewModel = libraryViewModel,
                    onPlaylistClick = { id -> navigateToPlaylist(id) },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route) { launchSingleTop = true }
                    },
                    onArtistClick = { name -> navigateToArtist(name) },
                    scrollToTopTrigger = libraryScrollTrigger
                )
            }

            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinished = {
                        homeViewModel.dispatch(HomeEvent.Refresh)
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onRestartOnboarding = {
                        navController.navigate(Screen.Onboarding.route) { launchSingleTop = true }
                    }
                )
            }

            composable(
                route = Screen.PlaylistDetail.route,
                arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getString("playlistId").orEmpty()
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    onBack = { navController.popBackStack() },
                    repository = repository,
                    musicController = musicController,
                    onArtistClick = { name -> navigateToArtist(name) }
                )
            }

            composable(
                route = Screen.ArtistDetail.route,
                arguments = listOf(navArgument("artistName") { type = NavType.StringType })
            ) { backStackEntry ->
                val artistName = backStackEntry.arguments?.getString("artistName").orEmpty()
                ArtistDetailScreen(
                    artistName = artistName,
                    onBack = { navController.popBackStack() },
                    repository = repository,
                    musicController = musicController,
                    onArtistClick = { name -> navigateToArtist(name) }
                )
            }
        }

        // Scroll edge fade under unified floating glass dock
        val initialHasTrack = remember(musicController) { musicController.hasActiveTrackSnapshot() }
        val hasCurrentTrack by remember(musicController) {
            musicController.playbackState
                .map { it.currentTrack != null }
                .distinctUntilChanged()
        }.collectAsState(initial = initialHasTrack)
        val hasMiniPlayer = hasCurrentTrack && currentRoute != Screen.Onboarding.route
        val bottomScrimHeight = when {
            currentRoute == Screen.Onboarding.route -> 0.dp
            currentRoute == Screen.Settings.route -> if (hasMiniPlayer) 92.dp else 0.dp
            hasMiniPlayer -> 158.dp
            else -> 92.dp
        }
        if (bottomScrimHeight > 0.dp) {
            val scrimBrush = remember(screenBg) {
                Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.35f to screenBg.copy(alpha = 0.80f),
                    0.65f to screenBg.copy(alpha = 0.96f),
                    1.0f to screenBg
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(scrimBrush)
                    .navigationBarsPadding()
                    .height(bottomScrimHeight)
            )
        }

        // Material 3 Expressive Floating MiniPlayer + Floating Navigation Pill Dock
        if (currentRoute != Screen.Onboarding.route) {
            val dockBottomInset = if (currentRoute == Screen.Settings.route) {
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp)
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            }
            Column(modifier = dockBottomInset) {
                Box(modifier = Modifier.padding(bottom = 2.dp)) {
                    MiniPlayerBarHost(
                        musicController = musicController,
                        onExpand = { isPlayerExpanded = true }
                    )
                }

                AnimatedVisibility(
                    visible = currentRoute != Screen.Settings.route,
                    enter = if (isReducedMotion) fadeIn(tween(150)) else {
                        slideInVertically(tween(220, easing = EchoMotion.EaseOutStrong)) { it } + fadeIn(tween(180))
                    },
                    exit = if (isReducedMotion) fadeOut(tween(120)) else {
                        slideOutVertically(tween(180)) { it } + fadeOut(tween(140))
                    }
                ) {
                    EchoBottomNavBar(
                        currentRoute = activeTabRoute,
                        onTabSelected = { tab -> navigateToTab(tab) }
                    )
                }
            }
        }

        // Material 3 Expressive Container Expand: Fullscreen Now Playing Sheet
        AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = if (isReducedMotion) {
                fadeIn(tween(180))
            } else {
                slideInVertically(
                    animationSpec = EchoMotion.spatialDefault()
                ) { it / 2 } +
                    scaleIn(initialScale = 0.94f, animationSpec = EchoMotion.spatialDefault()) +
                    fadeIn(tween(220, easing = EchoMotion.EaseOutStrong))
            },
            exit = if (isReducedMotion) {
                fadeOut(tween(150))
            } else {
                slideOutVertically(tween(220, easing = EchoMotion.EaseOutStrong)) { it / 2 } +
                    scaleOut(targetScale = 0.95f, animationSpec = tween(200)) +
                    fadeOut(tween(180))
            }
        ) {
            NowPlayingScreen(
                musicController = musicController,
                onCollapse = { isPlayerExpanded = false },
                onViewArtist = { artistName ->
                    isPlayerExpanded = false
                    navigateToArtist(artistName)
                }
            )
        }

        availableLiveUpdate?.let { update ->
            AppUpdateDialog(
                update = update,
                onDismiss = { availableLiveUpdate = null }
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
    val isReducedMotion = LocalReducedMotion.current

    AnimatedVisibility(
        visible = playbackState.currentTrack != null,
        enter = if (isReducedMotion) {
            fadeIn(tween(150))
        } else {
            slideInVertically(tween(260, easing = EchoMotion.EncoreButtonEase)) { it / 2 } +
                fadeIn(tween(220, easing = EchoMotion.EaseOutStrong)) +
                scaleIn(initialScale = 0.94f, animationSpec = tween(240, easing = EchoMotion.EaseOutStrong))
        },
        exit = if (isReducedMotion) {
            fadeOut(tween(120))
        } else {
            slideOutVertically(tween(200)) { it / 2 } +
                fadeOut(tween(160)) +
                scaleOut(targetScale = 0.94f, animationSpec = tween(180))
        }
    ) {
        MiniPlayerBar(
            playbackState = playbackState,
            onTogglePlayPause = { musicController.togglePlayPause() },
            onSkipNext = { musicController.skipNext() },
            onSkipPrevious = { musicController.skipPrevious() },
            onToggleFavorite = {
                playbackState.currentTrack?.let { musicController.toggleFavorite(it) }
            },
            positionStateFlow = musicController.positionState,
            onClick = onExpand
        )
    }
}
