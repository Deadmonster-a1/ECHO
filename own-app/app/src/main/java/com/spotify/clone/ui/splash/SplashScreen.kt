package com.spotify.clone.ui.splash

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.spotify.clone.R
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoPrismGradientColors
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.ThemeManager
import kotlinx.coroutines.delay
import kotlin.math.sin

private const val TAG = "EchoSplash"
private const val SPLASH_PREPARE_TIMEOUT_MS = 8000L
private const val SPLASH_PLAYBACK_TIMEOUT_MS = 4000L
private const val SPLASH_FAST_DISMISS_MS = 180L
private val SplashLightBgColor = Color(0xFFFFFFFF)
private val SplashDarkBgColor = Color(0xFF09080D)

/**
 * Animated ECHO Splash Screen with sound:
 * - Plays the theme-matched Echo splash video + audio (`echo_splash_light.mp4` / `echo_splash_dark.mp4`)
 * - Falls back cleanly to a minimal static mark when fastDismiss or reduced motion is active
 */
@OptIn(UnstableApi::class)
@Composable
fun EchoSplashOverlay(
    visible: Boolean,
    fastDismiss: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val isReducedMotion = LocalReducedMotion.current
    val isInspection = LocalInspectionMode.current
    val currentOnFinished by rememberUpdatedState(onFinished)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(80)),
        exit = fadeOut(tween(300, easing = EchoMotion.EaseOutStrong)) +
            scaleOut(targetScale = 1.04f, animationSpec = tween(300, easing = EchoMotion.EaseOutStrong)),
        modifier = modifier
    ) {
        val splashDarkMode = remember { isDarkMode }
        val bgColor = if (splashDarkMode) SplashDarkBgColor else SplashLightBgColor
        val shutterColorInt = if (splashDarkMode) 0xFF09080D.toInt() else 0xFFFFFFFF.toInt()
        val interactionSource = remember { MutableInteractionSource() }

        if (fastDismiss || isReducedMotion || isInspection) {
            LaunchedEffect(Unit) {
                delay(SPLASH_FAST_DISMISS_MS)
                currentOnFinished()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
                    .clickable(interactionSource = interactionSource, indication = null) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    EchoAcousticLogoMark(modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "echo",
                        color = if (splashDarkMode) Color(0xFFF8F8FC) else Color(0xFF16161D),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        } else {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            val rawResId = if (splashDarkMode) R.raw.echo_splash_dark else R.raw.echo_splash_light

            var hasFinished by remember { mutableStateOf(false) }
            var playbackStarted by remember { mutableStateOf(false) }
            val triggerFinish = remember {
                {
                    if (!hasFinished) {
                        hasFinished = true
                        currentOnFinished()
                    }
                }
            }

            val exoPlayer = remember(context, rawResId) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build()
                ExoPlayer.Builder(context)
                    .setAudioAttributes(audioAttributes, false)
                    .build()
                    .apply {
                        val uri = Uri.parse("android.resource://${context.packageName}/$rawResId")
                        setMediaItem(MediaItem.fromUri(uri))
                        repeatMode = Player.REPEAT_MODE_OFF
                        playWhenReady = true
                        prepare()
                    }
            }

            DisposableEffect(exoPlayer, lifecycleOwner) {
                val playerListener = object : Player.Listener {
                    override fun onRenderedFirstFrame() {
                        if (!playbackStarted) {
                            playbackStarted = true
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying && !playbackStarted) {
                            playbackStarted = true
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            triggerFinish()
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "Splash playback error: ${error.errorCodeName}", error)
                        triggerFinish()
                    }
                }
                val lifecycleObserver = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                        Lifecycle.Event.ON_RESUME -> if (!hasFinished) exoPlayer.play()
                        else -> Unit
                    }
                }
                exoPlayer.addListener(playerListener)
                lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
                    exoPlayer.removeListener(playerListener)
                    exoPlayer.release()
                }
            }

            LaunchedEffect(playbackStarted) {
                delay(if (playbackStarted) SPLASH_PLAYBACK_TIMEOUT_MS else SPLASH_PREPARE_TIMEOUT_MS)
                triggerFinish()
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
                    .clickable(interactionSource = interactionSource, indication = null) {},
                contentAlignment = Alignment.Center
            ) {
                val resizeMode = if (maxHeight >= maxWidth) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }

                AndroidView(
                    factory = { ctx ->
                        (LayoutInflater.from(ctx).inflate(R.layout.view_splash_player, null, false) as PlayerView).apply {
                            useController = false
                            setShutterBackgroundColor(shutterColorInt)
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            this.resizeMode = resizeMode
                            keepScreenOn = true
                            player = exoPlayer
                        }
                    },
                    update = { playerView ->
                        if (playerView.player !== exoPlayer) {
                            playerView.player = exoPlayer
                        }
                        if (playerView.resizeMode != resizeMode) {
                            playerView.resizeMode = resizeMode
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private val LOGO_HEIGHT_FRACTIONS = floatArrayOf(0.34f, 0.60f, 0.88f, 0.60f, 0.34f)
private val LOGO_WAVE_FREQS = floatArrayOf(1.7f, 2.3f, 1.3f, 2.7f, 1.9f)
private val LOGO_WAVE_OFFSETS = floatArrayOf(0.0f, 1.15f, 2.30f, 0.65f, 1.85f)

/**
 * Clean 5-bar symmetric acoustic mark painted with the Pastel Prism Apple gradient (no circles).
 */
@Composable
fun EchoAcousticLogoMark(
    progress: Float = 1f,
    breathPhase: Float? = null,
    shimmerShift: Float = 0f,
    playingWaveStrength: Float = 0f,
    breathPhaseState: androidx.compose.runtime.State<Float>? = null,
    shimmerShiftState: androidx.compose.runtime.State<Float>? = null,
    playingWaveStrengthState: androidx.compose.runtime.State<Float>? = null,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val livePhase = breathPhaseState?.value ?: breathPhase
        val liveShimmer = shimmerShiftState?.value ?: shimmerShift
        val liveStrength = playingWaveStrengthState?.value ?: playingWaveStrength

        val gradientBrush = Brush.linearGradient(
            colors = EchoPrismGradientColors,
            start = Offset(w * (0.08f + liveShimmer * 0.15f), h * 0.08f),
            end = Offset(w * (0.92f + liveShimmer * 0.15f), h * 0.92f)
        )

        val barWidth = w * 0.108f
        val pitch = w * 0.180f

        for (idx in LOGO_HEIGHT_FRACTIONS.indices) {
            val targetFrac = LOGO_HEIGHT_FRACTIONS[idx]
            val distFromCenter = kotlin.math.abs(idx - 2)
            val staggered = ((progress - distFromCenter * 0.12f) / (1f - distFromCenter * 0.12f))
                .coerceIn(0.18f, 1f)
            val waveScale = if (livePhase != null && progress > 0.85f) {
                if (liveStrength > 0.01f) {
                    val primary = sin(livePhase * LOGO_WAVE_FREQS[idx] + LOGO_WAVE_OFFSETS[idx])
                    val secondary = sin(livePhase * 1.1f - idx * 0.9f) * 0.35f
                    val combined = (primary + secondary).coerceIn(-1f, 1f)
                    1f + combined * (0.34f * liveStrength)
                } else {
                    1f + 0.09f * sin(livePhase + idx * 0.75f)
                }
            } else {
                1f
            }
            val barHeight = (h * targetFrac * staggered * waveScale).coerceIn(barWidth, h * 0.96f)
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
