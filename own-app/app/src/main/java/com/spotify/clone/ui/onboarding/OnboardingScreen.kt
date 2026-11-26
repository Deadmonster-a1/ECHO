package com.spotify.clone.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.AsyncImage
import com.spotify.clone.core.data.SampleCatalog
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoPrismPrimary
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.resolvedBackground
import com.spotify.clone.core.designsystem.resolvedBorder
import com.spotify.clone.core.designsystem.resolvedCard
import com.spotify.clone.core.designsystem.tabularFigures
import com.spotify.clone.core.model.MusicGenre
import com.spotify.clone.core.profile.PrismAvatarOption
import com.spotify.clone.core.profile.UserProfileManager
import com.spotify.clone.core.recommendation.EchoRecommendationEngine
import com.spotify.clone.ui.splash.EchoAcousticLogoMark

private data class OnboardingArtistItem(
    val name: String,
    val avatarUrl: String
)

private val ONBOARDING_LANGUAGES = listOf(
    "Telugu",
    "Hindi",
    "English",
    "Tamil",
    "Punjabi",
    "Global"
)

private val ONBOARDING_ARTISTS: List<OnboardingArtistItem> by lazy {
    val fromCatalog = SampleCatalog.sampleArtists.map {
        OnboardingArtistItem(name = it.name, avatarUrl = it.avatarUrl)
    }
    val extra = listOf(
        OnboardingArtistItem("Shilpa Rao", "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg"),
        OnboardingArtistItem("Arijit Singh", "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg"),
        OnboardingArtistItem("The Weeknd", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/49/3d/ab/493dab54-f920-9043-6181-80993b8116c9/19UMGIM53909.rgb.jpg/600x600bb.jpg"),
        OnboardingArtistItem("Taylor Swift", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&q=80"),
        OnboardingArtistItem("Shreya Ghoshal", "https://c.saavncdn.com/366/Pushpa-2-The-Rule-Telugu-Telugu-2024-20241205211012-500x500.jpg"),
        OnboardingArtistItem("Diljit Dosanjh", "https://c.saavncdn.com/888/Kalki-2898-Ad-Telugu-Telugu-2024-20240712063717-500x500.jpg"),
        OnboardingArtistItem("Anuv Jain", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&q=80"),
        OnboardingArtistItem("A.R. Rahman", "https://c.saavncdn.com/041/Sita-Ramam-Telugu-Original-Motion-Picture-Soundtrack-Telugu-2022-20220802140738-500x500.jpg"),
        OnboardingArtistItem("Pritam", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&q=80"),
        OnboardingArtistItem("Santhosh Narayanan", "https://c.saavncdn.com/888/Kalki-2898-Ad-Telugu-Telugu-2024-20240712063717-500x500.jpg"),
        OnboardingArtistItem("KSHMR", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400&q=80"),
        OnboardingArtistItem("Coldplay", "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=400&q=80")
    )
    (fromCatalog + extra).distinctBy { it.name.lowercase() }
}

// Emil Kowalski-inspired crisp, non-bouncy spring specs
private val CrispMotionSpring = spring<Float>(
    dampingRatio = 0.90f,
    stiffness = 480f
)

private val CrispDpSpring = spring<androidx.compose.ui.unit.Dp>(
    dampingRatio = 0.92f,
    stiffness = 460f
)

/**
 * Minimalist Apple HIG + Emil Kowalski-inspired 3-step onboarding flow.
 * Prioritizes generous negative space, crisp typographic hierarchy, single-focus
 * steps, and restrained tactile motion without decorative clutter.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalAppTheme.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val isReducedMotion = LocalReducedMotion.current
    val focusManager = LocalFocusManager.current

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
        mutableStateOf(existingLangs.ifEmpty { setOf("Telugu", "English") })
    }
    var selectedGenreIds by remember {
        mutableStateOf(existingGenres.ifEmpty { setOf(MusicGenre.MELODIES.id, MusicGenre.GLOBAL_POP.id) })
    }
    var selectedArtists by remember { mutableStateOf(existingArtists) }
    var artistQuery by remember { mutableStateOf("") }

    BackHandler(enabled = currentStep > 0) {
        focusManager.clearFocus()
        currentStep--
    }

    val screenBg = activeTheme.resolvedBackground(isDarkMode)
    val cardBg = activeTheme.resolvedCard(isDarkMode)
    val borderColor = activeTheme.resolvedBorder(isDarkMode)
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = activeTheme.primaryColor

    val progressFraction by animateFloatAsState(
        targetValue = (currentStep + 1) / 3f,
        animationSpec = if (isReducedMotion) tween(120) else CrispMotionSpring,
        label = "onboarding_track"
    )

    fun saveAndFinish() {
        focusManager.clearFocus()
        val resolvedName = nameInput.trim().ifBlank { "Listener" }
        UserProfileManager.completeOnboarding(
            name = resolvedName,
            avatarId = selectedAvatarId,
            languages = selectedLanguages,
            genreIds = selectedGenreIds,
            artists = selectedArtists
        )
        EchoRecommendationEngine.seedOnboardingPreferences(
            languages = selectedLanguages,
            genreIds = selectedGenreIds,
            artists = selectedArtists
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
        // Minimal Top Navigation Bar
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
                        .size(44.dp)
                        .clip(CircleShape)
                        .echoPressable(scaleOnPress = 0.90f, hapticOnClick = true) {
                            focusManager.clearFocus()
                            currentStep--
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Hair-thin centered progress track
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(96.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(borderColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }

            Text(
                text = "Skip",
                color = textSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(RoundedCornerShape(8.dp))
                    .echoPressable(scaleOnPress = 0.95f) { saveAndFinish() }
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            )
        }

        // Step Content Area
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (isReducedMotion) {
                    fadeIn(tween(150)) togetherWith fadeOut(tween(120))
                } else {
                    val forward = targetState >= initialState
                    val slideDist = { full: Int -> if (forward) full / 12 else -full / 12 }
                    val exitDist = { full: Int -> if (forward) -full / 16 else full / 16 }
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
                    cardBg = cardBg,
                    borderColor = borderColor,
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
                    isDarkMode = isDarkMode,
                    cardBg = cardBg,
                    borderColor = borderColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accent = accent
                )
                else -> ArtistsStep(
                    artistQuery = artistQuery,
                    onQueryChange = { artistQuery = it },
                    selectedArtists = selectedArtists,
                    onToggleArtist = { artistName ->
                        selectedArtists = if (artistName in selectedArtists) {
                            selectedArtists - artistName
                        } else {
                            selectedArtists + artistName
                        }
                    },
                    isDarkMode = isDarkMode,
                    cardBg = cardBg,
                    borderColor = borderColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accent = accent
                )
            }
        }

        // Clean Apple-style Primary CTA Footer
        val canProceed = when (currentStep) {
            0 -> true
            1 -> selectedLanguages.isNotEmpty() && selectedGenreIds.isNotEmpty()
            else -> selectedArtists.size >= 3
        }
        val ctaText = when (currentStep) {
            0 -> "Continue"
            1 -> "Continue"
            else -> if (selectedArtists.size >= 3) "Done" else "Pick ${3 - selectedArtists.size} more"
        }
        val ctaBg by animateColorAsState(
            targetValue = if (canProceed) accent else borderColor.copy(alpha = 0.7f),
            animationSpec = tween(180),
            label = "cta_bg"
        )
        val ctaFg by animateColorAsState(
            targetValue = if (canProceed) Color.White else textSecondary,
            animationSpec = tween(180),
            label = "cta_fg"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp
                )
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
    cardBg: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color
) {
    var isFocused by remember { mutableStateOf(false) }
    val activeAvatar = PrismAvatarOption.fromId(selectedAvatarId)
    val initial = nameInput.trim().firstOrNull()?.uppercaseChar()?.toString()

    val fieldBorderColor by animateColorAsState(
        targetValue = if (isFocused) accent else borderColor,
        animationSpec = tween(160),
        label = "name_field_border"
    )
    val fieldBorderWidth by animateDpAsState(
        targetValue = if (isFocused) 1.5.dp else 0.75.dp,
        animationSpec = CrispDpSpring,
        label = "name_field_width"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Crisp White ECHO Logo Tile
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(
                    elevation = if (isDarkMode) 12.dp else 8.dp,
                    shape = RoundedCornerShape(15.dp),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.12f)
                )
                .clip(RoundedCornerShape(15.dp))
                .background(Color.White)
                .border(0.75.dp, borderColor, RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            EchoAcousticLogoMark(modifier = Modifier.size(34.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "What’s your name?",
            color = textPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.7).sp,
            lineHeight = 36.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We’ll use this to personalize your home feed and mixes.",
            color = textSecondary,
            fontSize = 15.sp,
            lineHeight = 21.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Minimalist Apple Input Row with subtle inline Avatar Badge
        val inputShape = RoundedCornerShape(14.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(inputShape)
                .background(cardBg)
                .border(fieldBorderWidth, fieldBorderColor, inputShape)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(activeAvatar.startColor, activeAvatar.endColor))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial ?: "E",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            BasicTextField(
                value = nameInput,
                onValueChange = onNameChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isFocused = it.isFocused },
                decorationBox = { inner ->
                    if (nameInput.isEmpty()) {
                        Text(
                            text = "Your name",
                            color = textSecondary.copy(alpha = 0.7f),
                            fontSize = 16.sp
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
                        .echoPressable(scaleOnPress = 0.88f) { onNameChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear",
                        tint = textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Quiet 1-Row Circular Badge Swatches
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Badge color",
                color = textSecondary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrismAvatarOption.entries.forEach { option ->
                    val selected = option.id == selectedAvatarId
                    val dotScale by animateFloatAsState(
                        targetValue = if (selected) 1.12f else 1f,
                        animationSpec = CrispMotionSpring,
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
                                    Modifier
                                        .border(2.dp, screenBgForRing(isDarkMode), CircleShape)
                                        .padding(2.dp)
                                } else {
                                    Modifier
                                }
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

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun screenBgForRing(isDarkMode: Boolean): Color =
    if (isDarkMode) Color.White else Color(0xFF16161D)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TasteStep(
    selectedLanguages: Set<String>,
    onToggleLanguage: (String) -> Unit,
    selectedGenreIds: Set<String>,
    onToggleGenre: (String) -> Unit,
    isDarkMode: Boolean,
    cardBg: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color
) {
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
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp,
            lineHeight = 35.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Choose languages and genres to shape your mixes.",
            color = textSecondary,
            fontSize = 15.sp,
            lineHeight = 21.sp
        )

        Spacer(modifier = Modifier.height(26.dp))

        Text(
            text = "Languages",
            color = textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ONBOARDING_LANGUAGES.forEach { lang ->
                AppleSelectionPill(
                    label = lang,
                    selected = lang in selectedLanguages,
                    accent = accent,
                    cardBg = cardBg,
                    borderColor = borderColor,
                    textPrimary = textPrimary,
                    isDarkMode = isDarkMode,
                    onClick = { onToggleLanguage(lang) }
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Genres",
            color = textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MusicGenre.selectableGenres.forEach { genre ->
                AppleSelectionPill(
                    label = genre.shortLabel,
                    selected = genre.id in selectedGenreIds,
                    accent = accent,
                    cardBg = cardBg,
                    borderColor = borderColor,
                    textPrimary = textPrimary,
                    isDarkMode = isDarkMode,
                    onClick = { onToggleGenre(genre.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AppleSelectionPill(
    label: String,
    selected: Boolean,
    accent: Color,
    cardBg: Color,
    borderColor: Color,
    textPrimary: Color,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) accent else cardBg,
        animationSpec = tween(160),
        label = "pill_bg_$label"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) Color.White else textPrimary,
        animationSpec = tween(160),
        label = "pill_fg_$label"
    )
    val stroke by animateColorAsState(
        targetValue = if (selected) accent else borderColor,
        animationSpec = tween(160),
        label = "pill_border_$label"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(0.75.dp, stroke, RoundedCornerShape(50))
            .echoPressable(scaleOnPress = 0.95f, hapticOnClick = true, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun ArtistsStep(
    artistQuery: String,
    onQueryChange: (String) -> Unit,
    selectedArtists: Set<String>,
    onToggleArtist: (String) -> Unit,
    isDarkMode: Boolean,
    cardBg: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color
) {
    val haptic = LocalHapticFeedback.current
    val filtered = remember(artistQuery) {
        val q = artistQuery.trim().lowercase()
        if (q.isBlank()) ONBOARDING_ARTISTS
        else ONBOARDING_ARTISTS.filter { it.name.lowercase().contains(q) }
    }

    val statusText = when {
        selectedArtists.isEmpty() -> "Pick at least 3 to personalize your mixes"
        selectedArtists.size < 3 -> "${3 - selectedArtists.size} more to go"
        else -> "${selectedArtists.size} selected"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Choose 3 or more artists",
            color = textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp,
            lineHeight = 33.sp
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
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // iOS-style Minimal Search Field
        val searchBg = if (isDarkMode) Color(0xFF1A1A24) else Color(0xFFF1F2F7)
        val searchShape = RoundedCornerShape(12.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(searchShape)
                .background(searchBg)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = artistQuery,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = textPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(accent),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (artistQuery.isEmpty()) {
                        Text(
                            text = "Search",
                            color = textSecondary,
                            fontSize = 15.sp
                        )
                    }
                    inner()
                }
            )
            if (artistQuery.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .echoPressable(scaleOnPress = 0.88f) { onQueryChange("") },
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

        Spacer(modifier = Modifier.height(18.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered, key = { it.name }) { artist ->
                val isSelected = artist.name in selectedArtists
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.02f else 1f,
                    animationSpec = CrispMotionSpring,
                    label = "artist_scale_${artist.name}"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(12.dp))
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
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(if (isDarkMode) Color(0xFF1F2030) else Color(0xFFECEEF6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = artist.name.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
                                color = textSecondary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            AsyncImage(
                                model = artist.avatarUrl,
                                contentDescription = artist.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(2.2.dp, accent, CircleShape)
                                        } else {
                                            Modifier.border(0.6.dp, borderColor, CircleShape)
                                        }
                                    )
                            )
                        }
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isSelected,
                            enter = scaleIn(CrispMotionSpring) + fadeIn(tween(120)),
                            exit = scaleOut(tween(90)) + fadeOut(tween(90))
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(accent)
                                    .border(2.dp, if (isDarkMode) Color(0xFF09090E) else Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = artist.name,
                        color = if (isSelected) accent else textPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
