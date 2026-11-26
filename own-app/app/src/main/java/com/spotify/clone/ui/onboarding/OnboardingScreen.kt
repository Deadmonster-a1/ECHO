package com.spotify.clone.ui.onboarding

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.clone.core.data.GlobalCatalogService
import com.spotify.clone.core.data.OnboardingArtist
import com.spotify.clone.core.data.WorldOnboardingCatalog
import com.spotify.clone.core.data.WorldRegion
import com.spotify.clone.core.designsystem.AppThemeOption
import com.spotify.clone.core.designsystem.EchoArtistAvatar
import com.spotify.clone.core.designsystem.EchoInsetCard
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.EchoSegmentedControl
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.tabularFigures
import com.spotify.clone.core.legal.GravitLegalCatalog
import com.spotify.clone.core.legal.LegalDocumentTab
import com.spotify.clone.core.model.MusicGenre
import com.spotify.clone.core.profile.PrismAvatarOption
import com.spotify.clone.core.profile.UserProfileManager
import com.spotify.clone.core.recommendation.EchoRecommendationEngine
import com.spotify.clone.ui.components.LegalDocumentSheet
import com.spotify.clone.ui.splash.EchoAcousticLogoMark
import kotlinx.coroutines.delay

private enum class LanguageFilter(val label: String) {
    ALL("All"),
    INDIA("Indian"),
    WORLD("Global")
}

private val AppleCrispFloatSpring = spring<Float>(
    dampingRatio = 0.88f,
    stiffness = 460f
)

private val AppleCrispDpSpring = spring<androidx.compose.ui.unit.Dp>(
    dampingRatio = 0.90f,
    stiffness = 440f
)

/**
 * Apple HIG-inspired 3-step onboarding flow paired with Material 3 Expressive surfaces:
 * 1. Profile (name & badge color inside a clean grouped inset card)
 * 2. Languages & Genres (segmented filter + tactile selection pills)
 * 3. Favorite Artists (portrait grid with real photos for every artist + live related picks)
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val activeTheme by ThemeManager.currentTheme.collectAsState()
    val isDynamicColor by ThemeManager.dynamicColorEnabled.collectAsState()
    val isReducedMotion = LocalReducedMotion.current
    val focusManager = LocalFocusManager.current
    val catalogService = remember { GlobalCatalogService() }

    val existingName by UserProfileManager.userName.collectAsState()
    val existingAvatar by UserProfileManager.avatarId.collectAsState()
    val existingLangs by UserProfileManager.preferredLanguages.collectAsState()
    val existingGenres by UserProfileManager.preferredGenres.collectAsState()
    val existingArtists by UserProfileManager.favoriteArtists.collectAsState()

    var currentStep by remember { mutableIntStateOf(0) }
    var nameInput by remember {
        mutableStateOf(if (existingName == "Listener") "" else existingName)
    }
    var selectedAvatarId by remember { mutableStateOf(existingAvatar) }
    var selectedLanguages by remember {
        mutableStateOf(existingLangs.ifEmpty { setOf("English", "Hindi", "Telugu") })
    }
    var selectedGenreIds by remember {
        mutableStateOf(existingGenres.ifEmpty { setOf(MusicGenre.MELODIES.id, MusicGenre.GLOBAL_POP.id) })
    }

    var selectedArtists by remember { mutableStateOf(existingArtists) }
    var artistQuery by remember { mutableStateOf("") }
    var artistRegionFilter by remember { mutableStateOf(WorldRegion.ALL) }
    var liveArtistResults by remember { mutableStateOf<List<OnboardingArtist>>(emptyList()) }
    var activeLegalTab by remember { mutableStateOf<LegalDocumentTab?>(null) }

    // Debounced live catalog search for Artists step (surfaces any artist worldwide)
    LaunchedEffect(artistQuery, currentStep) {
        if (currentStep != 2) return@LaunchedEffect
        val q = artistQuery.trim()
        if (q.length < 2) {
            liveArtistResults = emptyList()
            return@LaunchedEffect
        }
        delay(280L)
        val tracks = runCatching { catalogService.searchTracks(q, limit = 16) }
            .getOrDefault(emptyList())
        liveArtistResults = tracks
            .flatMap { tr ->
                WorldOnboardingCatalog.extractArtistTokens(tr.artistName).map { token ->
                    OnboardingArtist(
                        name = token,
                        avatarUrl = tr.coverArtUrl,
                        languages = selectedLanguages.ifEmpty { setOf("Global") },
                        genreIds = setOf(MusicGenre.inferGenre(tr).id),
                        region = WorldRegion.WORLD,
                        popularityScore = 82
                    )
                }
            }
            .distinctBy { it.name.lowercase() }
    }

    BackHandler(enabled = currentStep > 0) {
        focusManager.clearFocus()
        currentStep--
    }

    val screenBg = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary

    fun saveAndFinish() {
        focusManager.clearFocus()
        val resolvedName = nameInput.trim().ifBlank { "Listener" }
        UserProfileManager.completeOnboarding(
            name = resolvedName,
            avatarId = selectedAvatarId,
            languages = selectedLanguages,
            genreIds = selectedGenreIds,
            artists = selectedArtists,
            songs = emptySet()
        )
        EchoRecommendationEngine.seedOnboardingPreferences(
            languages = selectedLanguages,
            genreIds = selectedGenreIds,
            artists = selectedArtists,
            songIds = emptySet(),
            songTracks = emptyList()
        )
        onFinished()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Apple HIG Top Navigation Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 20.dp)
        ) {
            if (currentStep > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .echoPressable(scaleOnPress = 0.90f, hapticOnClick = true) {
                            focusManager.clearFocus()
                            currentStep--
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier.align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center
                ) {
                    EchoAcousticLogoMark(modifier = Modifier.size(30.dp))
                }
            }

            // Segmented 3-step Apple HIG step indicator
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val isActive = index == currentStep
                    val isDone = index < currentStep
                    val segWidth by animateDpAsState(
                        targetValue = if (isActive) 34.dp else 18.dp,
                        animationSpec = if (isReducedMotion) tween(0) else AppleCrispDpSpring,
                        label = "step_seg_w_$index"
                    )
                    val segColor by animateColorAsState(
                        targetValue = when {
                            isActive -> accent
                            isDone -> accent.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        animationSpec = tween(180),
                        label = "step_seg_c_$index"
                    )
                    Box(
                        modifier = Modifier
                            .width(segWidth)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(segColor)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f))
                    .echoPressable(scaleOnPress = 0.95f) { saveAndFinish() }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Skip",
                    color = textSecondary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Step Content Area
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (isReducedMotion) {
                    fadeIn(tween(140)) togetherWith fadeOut(tween(110))
                } else {
                    val forward = targetState >= initialState
                    val slideDist = { full: Int -> if (forward) full / 10 else -full / 10 }
                    val exitDist = { full: Int -> if (forward) -full / 14 else full / 14 }
                    (slideInHorizontally(tween(240, easing = EchoMotion.EaseOutStrong), slideDist) +
                        fadeIn(tween(200, easing = EchoMotion.EaseOutStrong))) togetherWith
                        (slideOutHorizontally(tween(180), exitDist) +
                            fadeOut(tween(140)))
                }
            },
            modifier = Modifier.weight(1f),
            label = "onboarding_step_switch"
        ) { step ->
            when (step) {
                0 -> ProfileStep(
                    nameInput = nameInput,
                    onNameChange = { nameInput = it },
                    selectedAvatarId = selectedAvatarId,
                    onAvatarSelect = { selectedAvatarId = it },
                    activeTheme = activeTheme,
                    isDarkMode = isDarkMode,
                    isDynamicColor = isDynamicColor,
                    onSelectTheme = { option ->
                        ThemeManager.setDynamicColorEnabled(false)
                        ThemeManager.setTheme(option)
                    },
                    onToggleDarkMode = { dark -> ThemeManager.setDarkMode(dark) },
                    onToggleDynamicColor = { enabled -> ThemeManager.setDynamicColorEnabled(enabled) },
                    onSubmit = {
                        focusManager.clearFocus()
                        currentStep = 1
                    },
                    onOpenLegal = { tab -> activeLegalTab = tab },
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accent = accent
                )
                1 -> TasteStep(
                    selectedLanguages = selectedLanguages,
                    onToggleLanguage = { lang ->
                        selectedLanguages = if (lang in selectedLanguages && selectedLanguages.size > 1) {
                            selectedLanguages - lang
                        } else {
                            selectedLanguages + lang
                        }
                    },
                    selectedGenreIds = selectedGenreIds,
                    onToggleGenre = { gid ->
                        selectedGenreIds = if (gid in selectedGenreIds && selectedGenreIds.size > 1) {
                            selectedGenreIds - gid
                        } else {
                            selectedGenreIds + gid
                        }
                    },
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
                else -> ArtistsStep(
                    artistQuery = artistQuery,
                    onQueryChange = { artistQuery = it },
                    regionFilter = artistRegionFilter,
                    onRegionFilterChange = { artistRegionFilter = it },
                    selectedLanguages = selectedLanguages,
                    selectedGenreIds = selectedGenreIds,
                    selectedArtists = selectedArtists,
                    liveExtraArtists = liveArtistResults,
                    onToggleArtist = { artistName ->
                        selectedArtists = if (artistName in selectedArtists) {
                            selectedArtists - artistName
                        } else {
                            selectedArtists + artistName
                        }
                    },
                    isDarkMode = isDarkMode,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accent = accent
                )
            }
        }

        // Apple HIG Clean Primary Action Footer
        val canProceed = when (currentStep) {
            0 -> true
            1 -> selectedLanguages.isNotEmpty() && selectedGenreIds.isNotEmpty()
            else -> selectedArtists.size >= 3
        }
        val ctaText = when (currentStep) {
            0, 1 -> "Continue"
            else -> if (selectedArtists.size >= 3) "Start Listening" else "Select ${3 - selectedArtists.size} more"
        }
        val ctaBg by animateColorAsState(
            targetValue = if (canProceed) accent else MaterialTheme.colorScheme.surfaceContainerHighest,
            animationSpec = tween(180),
            label = "cta_bg"
        )
        val ctaFg by animateColorAsState(
            targetValue = if (canProceed) MaterialTheme.colorScheme.onPrimary else textSecondary,
            animationSpec = tween(180),
            label = "cta_fg"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(screenBg)
        ) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                thickness = 0.5.dp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(CircleShape)
                        .background(ctaBg)
                        .echoPressable(
                            scaleOnPress = if (canProceed) 0.975f else 1f,
                            hapticOnClick = canProceed
                        ) {
                            if (!canProceed) return@echoPressable
                            focusManager.clearFocus()
                            if (currentStep < 2) {
                                currentStep++
                            } else {
                                saveAndFinish()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = ctaText,
                            color = ctaFg,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (canProceed) {
                            Icon(
                                imageVector = if (currentStep < 2) Icons.AutoMirrored.Rounded.ArrowForward else Icons.Filled.Check,
                                contentDescription = null,
                                tint = ctaFg,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    activeLegalTab?.let { tab ->
        LegalDocumentSheet(
            initialTab = tab,
            onDismiss = { activeLegalTab = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileStep(
    nameInput: String,
    onNameChange: (String) -> Unit,
    selectedAvatarId: String,
    onAvatarSelect: (String) -> Unit,
    activeTheme: AppThemeOption,
    isDarkMode: Boolean,
    isDynamicColor: Boolean,
    onSelectTheme: (AppThemeOption) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onOpenLegal: (LegalDocumentTab) -> Unit,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color
) {
    val activeAvatar = PrismAvatarOption.fromId(selectedAvatarId)
    val initial = nameInput.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "E"
    val isReducedMotion = LocalReducedMotion.current
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        // Clean classic monogram avatar
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(activeAvatar.startColor, activeAvatar.endColor))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Minimal badge tint dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PrismAvatarOption.entries.forEach { option ->
                val selected = option.id == selectedAvatarId
                val dotScale by animateFloatAsState(
                    targetValue = if (selected && !isReducedMotion) 1.15f else 1f,
                    animationSpec = if (isReducedMotion) tween(0) else AppleCrispFloatSpring,
                    label = "dot_${option.id}"
                )
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = dotScale
                            scaleY = dotScale
                        }
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(option.startColor, option.endColor)))
                        .then(
                            if (selected) {
                                Modifier.border(
                                    2.dp,
                                    if (isDarkMode) Color.White else Color(0xFF16161D),
                                    CircleShape
                                )
                            } else Modifier
                        )
                        .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) {
                            onAvatarSelect(option.id)
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Welcome to echo",
            color = textPrimary,
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Enter your name and choose your look.",
            color = textSecondary,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(26.dp))

        // Classic single grouped setup card: Name + Appearance + Theme swatches
        EchoInsetCard(shape = RoundedCornerShape(EchoRadius.xl)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = nameInput,
                    onValueChange = onNameChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(accent),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (nameInput.isEmpty()) {
                            Text(
                                text = "Your name",
                                color = textSecondary.copy(alpha = 0.65f),
                                fontSize = 17.sp
                            )
                        }
                        inner()
                    }
                )

                if (nameInput.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .echoPressable(scaleOnPress = 0.88f) { onNameChange("") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear",
                            tint = textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = dividerColor,
                thickness = 0.6.dp,
                modifier = Modifier.padding(horizontal = 18.dp)
            )

            // Clean Light / Dark segmented switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(false to "Light", true to "Dark").forEach { (darkOption, label) ->
                    val selected = isDarkMode == darkOption
                    val pillBg by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        animationSpec = tween(160),
                        label = "classic_mode_bg_$label"
                    )
                    val pillFg by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else textSecondary,
                        animationSpec = tween(160),
                        label = "classic_mode_fg_$label"
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(pillBg)
                            .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true) {
                                onToggleDarkMode(darkOption)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (darkOption) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                            contentDescription = null,
                            tint = pillFg,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            color = pillFg,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }

            HorizontalDivider(
                color = dividerColor,
                thickness = 0.6.dp,
                modifier = Modifier.padding(horizontal = 18.dp)
            )

            // Simple classic single-row theme swatches
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val shortLabels = remember {
                    mapOf(
                        AppThemeOption.PASTEL_PRISM to "Violet",
                        AppThemeOption.ELECTRIC_CORAL to "Coral",
                        AppThemeOption.AURORA_TEAL to "Aurora",
                        AppThemeOption.STUDIO_AMBER to "Gold"
                    )
                }
                AppThemeOption.entries.forEach { option ->
                    val isSelected = activeTheme == option && !isDynamicColor
                    val swatchPrimary = option.resolvedPrimary(isDarkMode)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .echoPressable(scaleOnPress = 0.92f, hapticOnClick = true) {
                                onSelectTheme(option)
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(swatchPrimary, option.gradientEnd)))
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            2.5.dp,
                                            if (isDarkMode) Color.White else Color(0xFF16161D),
                                            CircleShape
                                        )
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = option.displayName,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = shortLabels[option] ?: option.displayName,
                            color = if (isSelected) textPrimary else textSecondary,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .echoPressable(scaleOnPress = 0.92f, hapticOnClick = true) {
                                onToggleDynamicColor(!isDynamicColor)
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .then(
                                    if (isDynamicColor) {
                                        Modifier.border(2.5.dp, accent, CircleShape)
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ColorLens,
                                contentDescription = "Dynamic Color",
                                tint = if (isDynamicColor) accent else textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Auto",
                            color = if (isDynamicColor) textPrimary else textSecondary,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isDynamicColor) FontWeight.Bold else FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Clean inline legal links
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "By continuing, you agree to ${GravitLegalCatalog.OWNER_NAME}'s ",
                    color = textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Privacy Policy",
                    color = accent,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.echoPressable(scaleOnPress = 0.96f) {
                        onOpenLegal(LegalDocumentTab.PRIVACY)
                    }
                )
                Text(
                    text = " & ",
                    color = textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Terms.",
                    color = accent,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.echoPressable(scaleOnPress = 0.96f) {
                        onOpenLegal(LegalDocumentTab.TERMS)
                    }
                )
            }
            Text(
                text = GravitLegalCatalog.COPYRIGHT_NOTICE,
                color = textSecondary.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.echoPressable(scaleOnPress = 0.96f) {
                    onOpenLegal(LegalDocumentTab.COPYRIGHT)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TasteStep(
    selectedLanguages: Set<String>,
    onToggleLanguage: (String) -> Unit,
    selectedGenreIds: Set<String>,
    onToggleGenre: (String) -> Unit,
    textPrimary: Color,
    textSecondary: Color
) {
    var langFilter by remember { mutableStateOf(LanguageFilter.ALL) }
    val visibleLanguages = remember(langFilter) {
        when (langFilter) {
            LanguageFilter.ALL -> WorldOnboardingCatalog.ALL_LANGUAGES
            LanguageFilter.INDIA -> WorldOnboardingCatalog.INDIAN_LANGUAGES
            LanguageFilter.WORLD -> WorldOnboardingCatalog.WORLD_LANGUAGES
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "TASTE PROFILE",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 0.9.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "What do you listen to?",
            color = textPrimary,
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Choose your languages and genres so we can recommend the right artists.",
            color = textSecondary,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Apple HIG Segmented Control for Languages
        EchoSegmentedControl(
            options = LanguageFilter.entries,
            selectedOption = langFilter,
            labelFor = { it.label },
            onSelect = { langFilter = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Languages",
                color = textPrimary,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "${selectedLanguages.size} selected",
                color = textSecondary,
                style = MaterialTheme.typography.labelMedium.tabularFigures()
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            visibleLanguages.forEach { lang ->
                AppleSelectionPill(
                    label = lang,
                    selected = lang in selectedLanguages,
                    textPrimary = textPrimary,
                    onClick = { onToggleLanguage(lang) }
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Genres",
                color = textPrimary,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "${selectedGenreIds.size} selected",
                color = textSecondary,
                style = MaterialTheme.typography.labelMedium.tabularFigures()
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MusicGenre.selectableGenres.forEach { genre ->
                AppleSelectionPill(
                    label = genre.shortLabel,
                    selected = genre.id in selectedGenreIds,
                    textPrimary = textPrimary,
                    onClick = { onToggleGenre(genre.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun ArtistsStep(
    artistQuery: String,
    onQueryChange: (String) -> Unit,
    regionFilter: WorldRegion,
    onRegionFilterChange: (WorldRegion) -> Unit,
    selectedLanguages: Set<String>,
    selectedGenreIds: Set<String>,
    selectedArtists: Set<String>,
    liveExtraArtists: List<OnboardingArtist>,
    onToggleArtist: (String) -> Unit,
    isDarkMode: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color
) {
    val haptic = LocalHapticFeedback.current
    val isReducedMotion = LocalReducedMotion.current

    val filtered = remember(
        artistQuery,
        regionFilter,
        selectedLanguages,
        selectedGenreIds,
        liveExtraArtists
    ) {
        WorldOnboardingCatalog.optimizeArtistsForUser(
            selectedLanguages = selectedLanguages,
            selectedGenreIds = selectedGenreIds,
            selectedSongTracks = emptyList(),
            selectedArtists = emptySet(),
            extraArtists = liveExtraArtists,
            query = artistQuery,
            regionFilter = regionFilter,
            prioritizeSelectedInList = false
        )
    }

    val similarStrip = remember(
        selectedLanguages,
        selectedGenreIds,
        selectedArtists
    ) {
        if (selectedArtists.isEmpty()) {
            emptyList()
        } else {
            WorldOnboardingCatalog.getSimilarArtistsStrip(
                selectedLanguages = selectedLanguages,
                selectedGenreIds = selectedGenreIds,
                selectedSongTracks = emptyList(),
                selectedArtists = selectedArtists,
                limit = 8
            )
        }
    }

    val statusText = when {
        selectedArtists.isEmpty() -> "Pick at least 3 artists to build your Home feed"
        selectedArtists.size < 3 -> "${3 - selectedArtists.size} more to go"
        else -> "${selectedArtists.size} selected • Looking great"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "FAVORITE ARTISTS",
            color = accent,
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 0.9.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Choose your artists",
            color = textPrimary,
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(modifier = Modifier.height(6.dp))

        AnimatedContent(
            targetState = statusText,
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(120)) },
            label = "artist_count_status"
        ) { text ->
            Text(
                text = text,
                color = if (selectedArtists.size >= 3) accent else textSecondary,
                style = MaterialTheme.typography.bodyMedium.tabularFigures().copy(
                    fontSize = 14.5.sp,
                    fontWeight = if (selectedArtists.size >= 3) FontWeight.SemiBold else FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        OnboardingSearchField(
            query = artistQuery,
            placeholder = "Search artists",
            onQueryChange = onQueryChange,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            accent = accent
        )

        Spacer(modifier = Modifier.height(12.dp))

        EchoSegmentedControl(
            options = WorldRegion.entries,
            selectedOption = regionFilter,
            labelFor = { if (it == WorldRegion.ALL) "For You" else it.label },
            onSelect = onRegionFilterChange
        )

        if (artistQuery.isBlank() && similarStrip.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Similar to your picks",
                color = textSecondary,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(similarStrip, key = { "rec_${it.artist.name}" }) { rec ->
                    Surface(
                        modifier = Modifier.echoPressable(scaleOnPress = 0.95f, hapticOnClick = false) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleArtist(rec.artist.name)
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 5.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EchoArtistAvatar(
                                name = rec.artist.name,
                                avatarUrl = rec.artist.avatarUrl,
                                size = 30.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = rec.artist.name,
                                color = textPrimary,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered, key = { it.artist.name }) { rec ->
                val artist = rec.artist
                val isSelected = artist.name in selectedArtists
                val scale by animateFloatAsState(
                    targetValue = if (isSelected && !isReducedMotion) 1.03f else 1f,
                    animationSpec = if (isReducedMotion) tween(0) else AppleCrispFloatSpring,
                    label = "artist_scale_${artist.name}"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(14.dp))
                        .echoPressable(
                            scaleOnPress = 0.95f,
                            hapticOnClick = false
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleArtist(artist.name)
                        }
                        .padding(vertical = 2.dp)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        EchoArtistAvatar(
                            name = artist.name,
                            avatarUrl = artist.avatarUrl,
                            size = 88.dp,
                            borderColor = if (isSelected) accent else Color.Transparent,
                            borderWidth = if (isSelected) 2.5.dp else 0.dp
                        )
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isSelected,
                            enter = if (isReducedMotion) fadeIn(tween(120)) else scaleIn(AppleCrispFloatSpring) + fadeIn(tween(120)),
                            exit = if (isReducedMotion) fadeOut(tween(90)) else scaleOut(tween(90)) + fadeOut(tween(90))
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(accent)
                                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = artist.name,
                        color = if (isSelected) accent else textPrimary,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingSearchField(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = textSecondary,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = textPrimary, fontSize = 15.5.sp, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(accent),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = textSecondary,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                inner()
            }
        )
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .echoPressable(scaleOnPress = 0.88f) { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun AppleSelectionPill(
    label: String,
    selected: Boolean,
    textPrimary: Color,
    onClick: () -> Unit
) {
    val corner by animateDpAsState(
        targetValue = if (selected) 24.dp else 14.dp,
        animationSpec = AppleCrispDpSpring,
        label = "sel_corner_$label"
    )
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(160),
        label = "pill_bg_$label"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else textPrimary,
        animationSpec = tween(160),
        label = "pill_fg_$label"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(corner))
            .background(bg)
            .echoPressable(scaleOnPress = 0.95f, hapticOnClick = true, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = fg,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(14.dp)
            )
        }
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        )
    }
}
