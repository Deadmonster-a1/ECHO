package com.spotify.clone.ui.navigation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.spotify.clone.core.data.AudioRepository
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.player.MusicController
import com.spotify.clone.core.profile.UserProfileManager
import com.spotify.clone.core.update.AppUpdateManager
import com.spotify.clone.core.update.UpdateInfo
import com.spotify.clone.ui.artist.ArtistDetailScreen
import com.spotify.clone.ui.components.MiniPlayerBar
import com.spotify.clone.ui.components.NavigationTab
import com.spotify.clone.ui.components.SpotifyBottomNavBar
import com.spotify.clone.ui.home.HomeScreen
import com.spotify.clone.ui.home.HomeViewModel
import com.spotify.clone.ui.library.LibraryScreen
import com.spotify.clone.ui.library.LibraryViewModel
import com.spotify.clone.ui.player.NowPlayingScreen
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Icon
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.ui.playlist.PlaylistDetailScreen
import com.spotify.clone.ui.profile.ProfileScreen
import com.spotify.clone.ui.search.SearchScreen
import com.spotify.clone.ui.search.SearchViewModel
import com.spotify.clone.ui.settings.SettingsScreen
import com.spotify.clone.ui.splash.SplashScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeTheme = LocalAppTheme.current
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

    // User Profile Name Prompt State
    val isNamePromptNeeded by UserProfileManager.isNamePromptNeeded.collectAsState()
    var nameInputValue by remember { mutableStateOf("") }

    // Direct Live App Update States
    var availableLiveUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var showLiveUpdateDialog by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var updateDownloadProgress by remember { mutableFloatStateOf(0f) }

    // Auto-check for updates on launch and whenever returning to foreground
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    try {
                        val installedVer = AppUpdateManager.getInstalledVersionName(context)
                        val result = AppUpdateManager.checkForUpdates(context, installedVer)
                        result.getOrNull()?.let { update ->
                            availableLiveUpdate = update
                            showLiveUpdateDialog = true
                        }
                    } catch (_: Exception) {}
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
                        onCheckUpdateClick = {
                            coroutineScope.launch {
                                val installedVer = AppUpdateManager.getInstalledVersionName(context)
                                Toast.makeText(context, "Checking for ECHO updates...", Toast.LENGTH_SHORT).show()
                                val result = AppUpdateManager.checkForUpdates(context, installedVer)
                                result.onSuccess { update ->
                                    if (update != null) {
                                        availableLiveUpdate = update
                                        showLiveUpdateDialog = true
                                    } else {
                                        Toast.makeText(context, "You are using the latest version (v$installedVer)", Toast.LENGTH_SHORT).show()
                                    }
                                }.onFailure {
                                    Toast.makeText(context, "Update check: ${it.message ?: "Failed"}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
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
                        musicController = musicController
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

        // Fullscreen Now Playing Overlay - Smooth Clean Slide without spring bounce
        AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = slideInVertically(animationSpec = androidx.compose.animation.core.tween(260)) { it } +
                    androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)),
            exit = slideOutVertically(animationSpec = androidx.compose.animation.core.tween(220)) { it } +
                    androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF121212))
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

        // First Run: User Name Setup Prompt Dialog
        if (isNamePromptNeeded && currentRoute != Screen.Splash.route) {
            AlertDialog(
                onDismissRequest = { UserProfileManager.dismissNamePrompt() },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        text = "Welcome to ECHO",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "What should we call you? Your name personalizes your sound experience and profile.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.5.sp
                        )
                        OutlinedTextField(
                            value = nameInputValue,
                            onValueChange = { nameInputValue = it },
                            label = { Text("Your Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            placeholder = { Text("e.g. Alex", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            UserProfileManager.setUserName(nameInputValue)
                            Toast.makeText(context, "Welcome, ${UserProfileManager.getDisplayName()}!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Continue", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { UserProfileManager.dismissNamePrompt() }) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }

        // Premium Direct In-App Live Update Modal Card
        if (showLiveUpdateDialog && availableLiveUpdate != null) {
            val update = availableLiveUpdate!!
            Dialog(
                onDismissRequest = {
                    if (!isDownloadingUpdate) showLiveUpdateDialog = false
                },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = !isDownloadingUpdate,
                    dismissOnClickOutside = !isDownloadingUpdate
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF242133),
                                    Color(0xFF171524),
                                    Color(0xFF0F0E16)
                                )
                            )
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    activeTheme.primaryColor.copy(alpha = 0.85f),
                                    activeTheme.primaryColor.copy(alpha = 0.25f),
                                    Color(0xFF333044)
                                )
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .padding(horizontal = 22.dp, vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Glowing Icon Badge
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            activeTheme.primaryColor.copy(alpha = 0.35f),
                                            activeTheme.primaryColor.copy(alpha = 0.06f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .border(1.5.dp, activeTheme.primaryColor.copy(alpha = 0.7f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = activeTheme.primaryColor,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Category Pill Tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(activeTheme.primaryColor.copy(alpha = 0.18f))
                                .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "NEW VERSION AVAILABLE",
                                color = activeTheme.primaryColor,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Title & Version
                        Text(
                            text = "ECHO v${update.versionName}",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Official Studio Live Update",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Changelog Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF191724))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Bolt,
                                        contentDescription = null,
                                        tint = activeTheme.primaryColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "WHAT'S NEW",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                update.releaseNotes.lines().filter { it.isNotBlank() }.forEach { line ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "•",
                                            color = activeTheme.primaryColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            text = line.removePrefix("•").removePrefix("-").trim(),
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Download progress or action buttons
                        if (isDownloadingUpdate) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(
                                    progress = { updateDownloadProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = activeTheme.primaryColor,
                                    trackColor = Color(0xFF292639)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Downloading studio update...",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "${(updateDownloadProgress * 100).toInt()}%",
                                        color = activeTheme.primaryColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            // "Update Now" Action Button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                activeTheme.primaryColor,
                                                activeTheme.primaryColor.copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                                    .echoPressable(scaleOnPress = 0.96f) {
                                        isDownloadingUpdate = true
                                        coroutineScope.launch {
                                            val apkFile = AppUpdateManager.downloadApk(context, update.downloadUrl) { progress ->
                                                updateDownloadProgress = progress
                                            }
                                            isDownloadingUpdate = false
                                            if (apkFile != null && apkFile.exists() && apkFile.length() > 1_000_000L) {
                                                showLiveUpdateDialog = false
                                                if (!AppUpdateManager.canRequestPackageInstalls(context)) {
                                                    AppUpdateManager.openInstallPermissionSettings(context)
                                                }
                                                AppUpdateManager.installApk(context, apkFile)
                                            } else {
                                                Toast.makeText(context, "Direct download failed. Opening browser...", Toast.LENGTH_LONG).show()
                                                AppUpdateManager.openBrowserDownload(context, update.downloadUrl)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Update Now",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Secondary Options: Browser Mirror & Later
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = {
                                    AppUpdateManager.openBrowserDownload(context, update.downloadUrl)
                                    showLiveUpdateDialog = false
                                }) {
                                    Text(
                                        text = "Download from Browser",
                                        color = activeTheme.primaryColor.copy(alpha = 0.9f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                TextButton(onClick = { showLiveUpdateDialog = false }) {
                                    Text(
                                        text = "Maybe Later",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
