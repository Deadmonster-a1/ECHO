package com.spotify.clone.core.designsystem

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.spotify.clone.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Dynamic Artwork Palette & Atmosphere Manager
 * Extracts rich, vibrant dominant colors from album covers to give the entire
 * application atmospheric living light and dynamic color harmony.
 */
data class ArtworkColors(
    val dominant: Color,
    val vibrantAccent: Color,
    val ambientGlow: Color,
    val backgroundTop: Color,
    val backgroundMid: Color,
    val backgroundBottom: Color
) {
    companion object {
        val Default = ArtworkColors(
            dominant = EchoPrismLavender,
            vibrantAccent = EchoPrismSky,
            ambientGlow = EchoPrismRose,
            backgroundTop = Color(0xFF221C35),
            backgroundMid = Color(0xFF151524),
            backgroundBottom = Color(0xFF09090E)
        )
    }
}

object ArtworkPaletteManager {
    private val cache = LruCache<String, ArtworkColors>(128)

    /**
     * Instantly resolves an atmospheric color palette for a track,
     * using memory cache, intelligent aesthetic heuristics, and asynchronous bitmap extraction.
     */
    suspend fun resolvePalette(context: Context, track: Track?): ArtworkColors {
        if (track == null) return ArtworkColors.Default

        val cacheKey = track.coverArtUrl.ifBlank { track.id }
        cache.get(cacheKey)?.let { return it }

        // Fast fallback heuristic while image is being fetched
        val heuristic = deriveHeuristicPalette(track)

        if (track.coverArtUrl.isBlank()) {
            cache.put(cacheKey, heuristic)
            return heuristic
        }

        return withContext(Dispatchers.IO) {
            try {
                val loader = context.imageLoader
                val request = ImageRequest.Builder(context)
                    .data(track.coverArtUrl)
                    .allowHardware(false)
                    .size(48, 48)
                    .build()

                val result = (loader.execute(request) as? SuccessResult)?.drawable?.toBitmap()
                if (result != null) {
                    val extracted = extractFromBitmap(result)
                    cache.put(cacheKey, extracted)
                    extracted
                } else {
                    cache.put(cacheKey, heuristic)
                    heuristic
                }
            } catch (_: Exception) {
                cache.put(cacheKey, heuristic)
                heuristic
            }
        }
    }

    private fun extractFromBitmap(bitmap: Bitmap): ArtworkColors {
        val scaled = Bitmap.createScaledBitmap(bitmap, 24, 24, false)
        val hsv = FloatArray(3)
        var maxSat = 0f
        var bestVibrantR = 127
        var bestVibrantG = 82
        var bestVibrantB = 255

        var domR = 0L
        var domG = 0L
        var domB = 0L
        var sampleCount = 0

        try {
            for (x in 0 until scaled.width) {
                for (y in 0 until scaled.height) {
                    val pixel = scaled.getPixel(x, y)
                    val r = android.graphics.Color.red(pixel)
                    val g = android.graphics.Color.green(pixel)
                    val b = android.graphics.Color.blue(pixel)

                    android.graphics.Color.RGBToHSV(r, g, b, hsv)
                    val sat = hsv[1]
                    val value = hsv[2]

                    // Discard pitch black, pure white, and pure greys for vibrant selection
                    if (value in 0.18f..0.92f && sat > 0.22f) {
                        val vibrancyScore = sat * 0.7f + value * 0.3f
                        if (vibrancyScore > maxSat) {
                            maxSat = vibrancyScore
                            bestVibrantR = r
                            bestVibrantG = g
                            bestVibrantB = b
                        }
                    }

                    domR += r
                    domG += g
                    domB += b
                    sampleCount++
                }
            }
        } finally {
            if (scaled != bitmap) {
                scaled.recycle()
            }
        }

        if (maxSat == 0f && sampleCount > 0) {
            bestVibrantR = (domR / sampleCount).toInt().coerceIn(30, 220)
            bestVibrantG = (domG / sampleCount).toInt().coerceIn(30, 220)
            bestVibrantB = (domB / sampleCount).toInt().coerceIn(30, 220)
        }

        val vibrantAccent = Color(bestVibrantR, bestVibrantG, bestVibrantB)

        // Generate harmonious background and glow tones
        val ambientGlow = Color(
            (bestVibrantR * 0.85f).toInt().coerceIn(0, 255),
            (bestVibrantG * 0.85f).toInt().coerceIn(0, 255),
            (bestVibrantB * 0.85f).toInt().coerceIn(0, 255)
        )

        val bgTop = Color(
            red = (bestVibrantR / 255f * 0.38f + 0.04f).coerceIn(0f, 1f),
            green = (bestVibrantG / 255f * 0.38f + 0.04f).coerceIn(0f, 1f),
            blue = (bestVibrantB / 255f * 0.38f + 0.04f).coerceIn(0f, 1f),
            alpha = 1.0f
        )

        val bgMid = Color(
            red = (bestVibrantR / 255f * 0.16f + 0.06f).coerceIn(0f, 1f),
            green = (bestVibrantG / 255f * 0.16f + 0.06f).coerceIn(0f, 1f),
            blue = (bestVibrantB / 255f * 0.16f + 0.06f).coerceIn(0f, 1f),
            alpha = 1.0f
        )

        return ArtworkColors(
            dominant = vibrantAccent,
            vibrantAccent = vibrantAccent,
            ambientGlow = ambientGlow,
            backgroundTop = bgTop,
            backgroundMid = bgMid,
            backgroundBottom = Color(0xFF0C0B10)
        )
    }

    /**
     * Curated acoustic & thematic color mapping for instant zero-latency styling.
     */
    fun deriveHeuristicPalette(track: Track): ArtworkColors {
        val text = "${track.title} ${track.albumName} ${track.artistName}".lowercase()

        return when {
            text.contains("devara") || text.contains("fear") || text.contains("ayudha") || text.contains("daavudi") -> {
                // Fiery molten embers & amber gold
                createPalette(Color(0xFFFF5722), Color(0xFFFF9800), Color(0xFF381508))
            }
            text.contains("vaikunthapurramuloo") || text.contains("samajavaragamana") || text.contains("sid sriram") || text.contains("nijame") -> {
                // Warm acoustic sunflower gold & honey
                createPalette(Color(0xFFFFB300), Color(0xFFFFD54F), Color(0xFF362808))
            }
            text.contains("pushpa") || text.contains("jaalam") || text.contains("mass") -> {
                // Electric red & vermilion
                createPalette(Color(0xFFE53935), Color(0xFFFF6E40), Color(0xFF380E10))
            }
            text.contains("kalki") || text.contains("prabhas") || text.contains("bhairava") -> {
                // Cyberpunk teal & galactic cyan
                createPalette(Color(0xFF00E5FF), Color(0xFF00B0FF), Color(0xFF07242E))
            }
            text.contains("sita ramam") || text.contains("lo-fi") || text.contains("reverie") || text.contains("inaye") -> {
                // Dreamy romantic lavender & violet
                createPalette(Color(0xFF9C27B0), Color(0xFFCE93D8), Color(0xFF26102E))
            }
            text.contains("anirudh") || text.contains("rock") || text.contains("bass") -> {
                // Neon electric purple
                createPalette(Color(0xFF7F52FF), Color(0xFFA882FF), Color(0xFF20153D))
            }
            else -> {
                // Hash-seeded harmonic color
                val hash = abs(track.title.hashCode())
                val hues = listOf(
                    Color(0xFF7F52FF) to Color(0xFF20153D),
                    Color(0xFF00B0FF) to Color(0xFF07242E),
                    Color(0xFFFF7043) to Color(0xFF35170C),
                    Color(0xFF00E676) to Color(0xFF072B15),
                    Color(0xFFE91E63) to Color(0xFF32091A),
                    Color(0xFFFFB300) to Color(0xFF332608)
                )
                val pair = hues[hash % hues.size]
                createPalette(pair.first, pair.first, pair.second)
            }
        }
    }

    private fun createPalette(dominant: Color, accent: Color, topBg: Color): ArtworkColors {
        return ArtworkColors(
            dominant = dominant,
            vibrantAccent = accent,
            ambientGlow = dominant.copy(alpha = 0.85f),
            backgroundTop = topBg,
            backgroundMid = Color(0xFF14121E),
            backgroundBottom = Color(0xFF0C0B10)
        )
    }
}

/**
 * Reactive Compose hook that extracts and smoothly animates dynamic artwork atmosphere.
 */
@Composable
fun rememberAnimatedArtworkColors(track: Track?): ArtworkColors {
    val context = LocalContext.current
    var currentPalette by remember(track?.id) {
        mutableStateOf(
            if (track != null) ArtworkPaletteManager.deriveHeuristicPalette(track)
            else ArtworkColors.Default
        )
    }

    LaunchedEffect(track?.id, track?.coverArtUrl) {
        if (track != null) {
            val resolved = ArtworkPaletteManager.resolvePalette(context, track)
            currentPalette = resolved
        } else {
            currentPalette = ArtworkColors.Default
        }
    }

    // Smooth 600ms cross-fade between album transitions
    val animDom by animateColorAsState(
        targetValue = currentPalette.dominant,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "anim_dom"
    )
    val animAccent by animateColorAsState(
        targetValue = currentPalette.vibrantAccent,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "anim_accent"
    )
    val animGlow by animateColorAsState(
        targetValue = currentPalette.ambientGlow,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "anim_glow"
    )
    val animTop by animateColorAsState(
        targetValue = currentPalette.backgroundTop,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "anim_top"
    )
    val animMid by animateColorAsState(
        targetValue = currentPalette.backgroundMid,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "anim_mid"
    )

    return remember(animDom, animAccent, animGlow, animTop, animMid) {
        ArtworkColors(
            dominant = animDom,
            vibrantAccent = animAccent,
            ambientGlow = animGlow,
            backgroundTop = animTop,
            backgroundMid = animMid,
            backgroundBottom = Color(0xFF0C0B10)
        )
    }
}
