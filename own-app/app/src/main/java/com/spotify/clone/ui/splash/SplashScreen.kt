package com.spotify.clone.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.clone.core.designsystem.EchoBlue
import com.spotify.clone.core.designsystem.EchoCharcoal
import com.spotify.clone.core.designsystem.EchoCoral
import com.spotify.clone.core.designsystem.EchoIvory
import com.spotify.clone.core.designsystem.EchoLavender
import com.spotify.clone.core.designsystem.EchoLiquidSymbol
import com.spotify.clone.core.designsystem.EchoSoftBlue
import com.spotify.clone.core.designsystem.EchoWordmark
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.profile.UserProfileManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * ECHO — Feel the sound.
 * Master Animated Splash Screen implementing the 5-Stage Liquid Transformation Sequence.
 *
 * Stage 1: Awakening (0–400ms) - Center glowing orb expands with spring motion
 * Stage 2: Liquid Transformation (400–1200ms) - Orb morphs into the fluid 'e' ribbon
 * Stage 3: Logo Reveal (1000–1800ms) - Lowercase 'echo' wordmark and tagline track in
 * Stage 4: Settle (1800–2500ms) - Ambient light stabilization, progress completes, user greeting
 * Stage 5: Enter the app (2500–2800ms) - Smooth coordinated fade-out into Home
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onSplashFinished: () -> Unit
) {
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val isReducedMotion = LocalReducedMotion.current
    val userName by UserProfileManager.userName.collectAsState()

    val bgColor = if (isDarkMode) EchoCharcoal else EchoIvory

    // Animation States
    val screenAlpha = remember { Animatable(1f) }
    val screenScale = remember { Animatable(1f) }

    // Stage 1: Awakening Orb
    val orbAlpha = remember { Animatable(if (isReducedMotion) 1f else 0f) }
    val orbScale = remember { Animatable(if (isReducedMotion) 1f else 0.4f) }

    // Stage 2: Liquid Ribbon Transformation (0.25f -> 1.0f)
    val morphProgress = remember { Animatable(if (isReducedMotion) 1f else 0f) }
    val symbolRotation = remember { Animatable(if (isReducedMotion) 0f else -18f) }
    val symbolScale = remember { Animatable(if (isReducedMotion) 1f else 0.85f) }

    // Stage 3 & 4: Wordmark & Tagline Reveal
    val wordmarkAlpha = remember { Animatable(if (isReducedMotion) 1f else 0f) }
    val wordmarkOffsetY = remember { Animatable(if (isReducedMotion) 0f else 28f) }
    val taglineAlpha = remember { Animatable(if (isReducedMotion) 1f else 0f) }

    // Progress Bar & Subtitle Status
    val progressBarWidth = remember { Animatable(if (isReducedMotion) 1f else 0f) }
    var statusText by remember { mutableStateOf("Awakening...") }

    // Background Ambient Lighting Waves
    val ambientGlowScale = remember { Animatable(if (isReducedMotion) 1f else 0.7f) }
    val ambientGlowAlpha = remember { Animatable(if (isReducedMotion) 0.35f else 0.05f) }

    LaunchedEffect(Unit) {
        if (isReducedMotion) {
            // Accessible fast-path: serene static presentation
            delay(1200)
            screenAlpha.animateTo(0f, tween(350, easing = LinearEasing))
            onSplashFinished()
            return@LaunchedEffect
        }

        // ==========================================
        // Stage 1 — Awakening (0–400 ms)
        // ==========================================
        statusText = "Awakening..."
        launch {
            orbAlpha.animateTo(1f, tween(320, easing = LinearOutSlowInEasing))
        }
        launch {
            orbScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }
        launch {
            ambientGlowAlpha.animateTo(0.28f, tween(500, easing = LinearEasing))
            ambientGlowScale.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }

        delay(380)

        // ==========================================
        // Stage 2 — Liquid Transformation (400–1200 ms)
        // ==========================================
        statusText = "Shaping sound..."
        launch {
            morphProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(850, easing = FastOutSlowInEasing)
            )
        }
        launch {
            symbolRotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
            )
        }
        launch {
            symbolScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(750, easing = FastOutSlowInEasing)
            )
        }

        delay(600)

        // ==========================================
        // Stage 3 — Logo Reveal (1000–1800 ms)
        // ==========================================
        statusText = "Loading your world..."
        launch {
            wordmarkAlpha.animateTo(1f, tween(500, easing = LinearOutSlowInEasing))
            taglineAlpha.animateTo(1f, tween(600, easing = LinearOutSlowInEasing))
        }
        launch {
            wordmarkOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
        }
        launch {
            progressBarWidth.animateTo(0.65f, tween(700, easing = FastOutSlowInEasing))
        }

        delay(800)

        // ==========================================
        // Stage 4 — Settle (1800–2500 ms)
        // ==========================================
        statusText = if (userName.isNotBlank()) "Welcome, $userName." else "Welcome to ECHO."
        launch {
            progressBarWidth.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        }

        delay(700)

        // ==========================================
        // Stage 5 — Enter the app (2500–2800 ms)
        // ==========================================
        launch {
            screenScale.animateTo(1.04f, tween(320, easing = FastOutSlowInEasing))
            screenAlpha.animateTo(0f, tween(300, easing = LinearEasing))
        }

        delay(320)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .graphicsLayer {
                alpha = screenAlpha.value
                scaleX = screenScale.value
                scaleY = screenScale.value
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Interactive tap to skip smoothly
                onSplashFinished()
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. Organic Sound Atmosphere & Light Trails Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Large ambient gradient aura
            val auraColor1 = if (isDarkMode) EchoLavender.copy(alpha = 0.18f * ambientGlowAlpha.value) else EchoLavender.copy(alpha = 0.08f * ambientGlowAlpha.value)
            val auraColor2 = if (isDarkMode) EchoCoral.copy(alpha = 0.12f * ambientGlowAlpha.value) else EchoCoral.copy(alpha = 0.06f * ambientGlowAlpha.value)
            val auraColor3 = if (isDarkMode) EchoBlue.copy(alpha = 0.14f * ambientGlowAlpha.value) else EchoBlue.copy(alpha = 0.07f * ambientGlowAlpha.value)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(auraColor1, auraColor2, Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.42f),
                    radius = (w * 0.75f) * ambientGlowScale.value
                ),
                radius = (w * 0.75f) * ambientGlowScale.value,
                center = Offset(w * 0.5f, h * 0.42f)
            )

            // Flowing subtle sound arcs in background
            if (morphProgress.value > 0.3f) {
                val arcAlpha = ((morphProgress.value - 0.3f) / 0.7f).coerceIn(0f, 1f) * 0.4f
                val sweepColor = if (isDarkMode) EchoLavender.copy(alpha = arcAlpha * 0.6f) else EchoLavender.copy(alpha = arcAlpha * 0.25f)
                drawCircle(
                    color = sweepColor,
                    radius = w * 0.62f,
                    center = Offset(w * 0.5f, h * 0.42f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                )
                drawCircle(
                    color = if (isDarkMode) EchoCoral.copy(alpha = arcAlpha * 0.4f) else EchoCoral.copy(alpha = arcAlpha * 0.18f),
                    radius = w * 0.82f,
                    center = Offset(w * 0.5f, h * 0.42f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                )
            }
        }

        // 2. Central Core: Liquid 'e' Symbol + 'echo' Wordmark Lockup
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Stylized Liquid 'e' with coordinated transformation
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = symbolScale.value * orbScale.value
                        scaleY = symbolScale.value * orbScale.value
                        rotationZ = symbolRotation.value
                        alpha = orbAlpha.value
                    },
                contentAlignment = Alignment.Center
            ) {
                EchoLiquidSymbol(
                    size = 190.dp,
                    morphProgress = morphProgress.value,
                    glowAlpha = if (isDarkMode) 0.40f else 0.20f,
                    isDarkMode = isDarkMode,
                    enableBreathing = true
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Lowercase 'echo' Wordmark + Tagline
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = wordmarkAlpha.value
                        translationY = wordmarkOffsetY.value
                    },
                contentAlignment = Alignment.Center
            ) {
                EchoWordmark(
                    titleSize = 56.sp,
                    taglineSize = 18.sp,
                    isDarkMode = isDarkMode,
                    taglineAlpha = taglineAlpha.value,
                    horizontalAlignment = Alignment.CenterHorizontally
                )
            }
        }

        // 3. Bottom Loading Progress Bar & Refined Status Indicator
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 54.dp, start = 48.dp, end = 48.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status text
            Text(
                text = statusText,
                fontSize = 13.5.sp,
                color = if (isDarkMode) Color(0xFFA6A2B3) else Color(0xFF777680),
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Minimalist Glowing Progress Track
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isDarkMode) Color(0xFF262534) else Color(0xFFE5E0E8))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressBarWidth.value)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(EchoSoftBlue, EchoLavender, EchoCoral)
                            )
                        )
                )
            }
        }
    }
}
