package com.spotify.clone.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoPrismAqua
import com.spotify.clone.core.designsystem.EchoPrismGradientColors
import com.spotify.clone.core.designsystem.EchoPrismLavender
import com.spotify.clone.core.designsystem.EchoPrismRose
import com.spotify.clone.core.designsystem.EchoPrismSky
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.resolvedBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Animated Apple-inspired ECHO Splash Screen (~1.1s):
 * - Breathing 5-bar Pastel Prism wave
 * - Soft drifting gradient aura matching the white ECHO logo tile
 * - Shimmering wordmark reveal before transitioning cleanly into onboarding or Home
 */
@Composable
fun EchoSplashOverlay(
    visible: Boolean,
    fastDismiss: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalAppTheme.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val isReducedMotion = LocalReducedMotion.current

    val iconScale = remember { Animatable(if (isReducedMotion) 1f else 0.84f) }
    val waveRise = remember { Animatable(if (isReducedMotion) 1f else 0.12f) }
    val wordmarkAlpha = remember { Animatable(if (isReducedMotion) 1f else 0f) }
    val shimmerOffset = remember { Animatable(-0.5f) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_breath")
    val breathPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "splash_breath_phase"
    )

    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        if (isReducedMotion || fastDismiss) {
            iconScale.snapTo(1f)
            waveRise.snapTo(1f)
            wordmarkAlpha.snapTo(1f)
            delay(180L)
            onFinished()
            return@LaunchedEffect
        }

        launch {
            iconScale.animateTo(1f, animationSpec = EchoMotion.SpringGentle)
        }
        launch {
            waveRise.animateTo(1f, animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing))
        }
        launch {
            delay(160L)
            wordmarkAlpha.animateTo(1f, animationSpec = tween(durationMillis = 320, easing = EchoMotion.EaseOutStrong))
        }
        launch {
            delay(220L)
            shimmerOffset.animateTo(1.5f, animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing))
        }

        delay(1100L)
        onFinished()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(80)),
        exit = fadeOut(tween(300, easing = EchoMotion.EaseOutStrong)) +
            scaleOut(targetScale = 1.04f, animationSpec = tween(300, easing = EchoMotion.EaseOutStrong)),
        modifier = modifier
    ) {
        val screenBg = activeTheme.resolvedBackground(isDarkMode)
        val squircleShape = RoundedCornerShape(24.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg),
            contentAlignment = Alignment.Center
        ) {
            // Soft Pastel Prism Aurora Backdrop
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .graphicsLayer {
                        val s = if (isReducedMotion) 1f else 1f + 0.05f * sin(breathPhase)
                        scaleX = s
                        scaleY = s
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                EchoPrismRose.copy(alpha = if (isDarkMode) 0.16f else 0.18f),
                                EchoPrismLavender.copy(alpha = if (isDarkMode) 0.12f else 0.14f),
                                EchoPrismAqua.copy(alpha = if (isDarkMode) 0.08f else 0.10f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Crisp White Squircle Tile with Pastel Prism Shimmer Border
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .graphicsLayer {
                            scaleX = iconScale.value
                            scaleY = iconScale.value
                        }
                        .shadow(
                            elevation = if (isDarkMode) 24.dp else 18.dp,
                            shape = squircleShape,
                            ambientColor = EchoPrismLavender.copy(alpha = 0.28f),
                            spotColor = EchoPrismSky.copy(alpha = 0.34f)
                        )
                        .clip(squircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF),
                                    Color(0xFFF7F8FC)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    EchoPrismRose.copy(alpha = 0.45f),
                                    EchoPrismLavender.copy(alpha = 0.45f),
                                    EchoPrismAqua.copy(alpha = 0.45f)
                                )
                            ),
                            shape = squircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    EchoAcousticLogoMark(
                        progress = waveRise.value,
                        breathPhase = if (isReducedMotion) null else breathPhase,
                        shimmerShift = shimmerOffset.value,
                        modifier = Modifier.size(58.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "ECHO",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp,
                    modifier = Modifier.graphicsLayer { alpha = wordmarkAlpha.value }
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Feel the sound",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.graphicsLayer { alpha = wordmarkAlpha.value * 0.85f }
                )
            }
        }
    }
}

/**
 * Clean 5-bar symmetric acoustic mark painted with the Pastel Prism Apple gradient (no circles).
 */
@Composable
fun EchoAcousticLogoMark(
    progress: Float = 1f,
    breathPhase: Float? = null,
    shimmerShift: Float = 0f,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        val gradientBrush = Brush.linearGradient(
            colors = EchoPrismGradientColors,
            start = Offset(w * (0.08f + shimmerShift * 0.15f), h * 0.08f),
            end = Offset(w * (0.92f + shimmerShift * 0.15f), h * 0.92f)
        )

        val barWidth = w * 0.108f
        val pitch = w * 0.180f
        val heightFractions = listOf(0.32f, 0.58f, 0.86f, 0.58f, 0.32f)

        heightFractions.forEachIndexed { idx, targetFrac ->
            val distFromCenter = kotlin.math.abs(idx - 2)
            val staggered = ((progress - distFromCenter * 0.12f) / (1f - distFromCenter * 0.12f))
                .coerceIn(0.18f, 1f)
            val breathScale = if (breathPhase != null && progress > 0.85f) {
                1f + 0.09f * sin(breathPhase + idx * 0.75f)
            } else {
                1f
            }
            val barHeight = (h * targetFrac * staggered * breathScale).coerceIn(barWidth, h * 0.94f)
            val cx = center.x + (idx - 2) * pitch
            drawRoundRect(
                brush = gradientBrush,
                topLeft = Offset(cx - barWidth / 2f, center.y - barHeight / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
