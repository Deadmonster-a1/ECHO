package com.spotify.clone.ui.onboarding

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import com.spotify.clone.core.designsystem.EchoArtistAvatar
import com.spotify.clone.core.designsystem.EchoInsetCard
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.EchoSegmentedControl
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.tabularFigures
import com.spotify.clone.core.model.MusicGenre
import com.spotify.clone.core.profile.PrismAvatarOption
import com.spotify.clone.core.profile.UserProfileManager
import com.spotify.clone.core.recommendation.EchoRecommendationEngine
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

    val progressFraction by animateFloatAsState(
        targetValue = (currentStep + 1) / 3f,
        animationSpec = if (isReducedMotion) tween(120) else EchoMotion.SpatialDefault,
        label = "onboarding_progress"
    )

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
            }

            // Clean 3-step capsule progress bar
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(108.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }

            Text(
                text = "Skip",
                color = textSecondary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(CircleShape)
                    .echoPressable(scaleOnPress = 0.95f) { saveAndFinish() }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
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
                    onSubmit = {
                        focusManager.clearFocus()
                        currentStep = 1
                    },
                    isDarkMode = isDarkMode,
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
                    Text(
                        text = ctaText,
                        color = ctaFg,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStep(
    nameInput: String,
    onNameChange: (String) -> Unit,
    selectedAvatarId: String,
    onAvatarSelect: (String) -> Unit,
    onSubmit: () -> Unit,
    isDarkMode: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color
) {
    val activeAvatar = PrismAvatarOption.fromId(selectedAvatarId)
    val initial = nameInput.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "E"
    val isReducedMotion = LocalReducedMotion.current
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Clean borderless floating ECHO acoustic mark
        EchoAcousticLogoMark(modifier = Modifier.size(38.dp))

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Welcome to echo",
            color = textPrimary,
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add your name and pick a badge color to personalize your listening space.",
            color = textSecondary,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Apple HIG grouped inset card: Name input + Badge color in one unified surface
        EchoInsetCard(shape = RoundedCornerShape(EchoRadius.xl)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(activeAvatar.startColor, activeAvatar.endColor))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

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
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .echoPressable(scaleOnPress = 0.88f) { onNameChange("") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear",
                            tint = textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = dividerColor,
                thickness = 0.6.dp,
                modifier = Modifier.padding(start = 74.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Badge color",
                    color = textSecondary,
                    style = MaterialTheme.typography.titleSmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrismAvatarOption.entries.forEach { option ->
                        val selected = option.id == selectedAvatarId
                        val dotScale by animateFloatAsState(
                            targetValue = if (selected && !isReducedMotion) 1.12f else 1f,
                            animationSpec = if (isReducedMotion) tween(0) else AppleCrispFloatSpring,
                            label = "dot_${option.id}"
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
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
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = option.label,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
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
        Spacer(modifier = Modifier.height(12.dp))

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
