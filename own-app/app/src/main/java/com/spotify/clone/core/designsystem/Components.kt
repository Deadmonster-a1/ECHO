package com.spotify.clone.core.designsystem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ECHO Brand Identity Mark
 * Tagline: Feel the sound.
 */
@Composable
fun EchoBrandMark(
    modifier: Modifier = Modifier,
    showTagline: Boolean = true
) {
    val activeTheme = LocalAppTheme.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(activeTheme.primaryColor, activeTheme.gradientEnd)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.GraphicEq,
                contentDescription = "ECHO",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = "ECHO",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (showTagline) {
                Text(
                    text = "Feel the sound.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Tactile, motion-responsive Play/Pause FAB
 */
@Composable
fun EchoPlayFab(
    isPlaying: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val isReducedMotion = LocalReducedMotion.current
    Box(
        modifier = modifier
            .size(size)
            .echoPressable(scaleOnPress = 0.92f, onClick = onToggle)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = {
                if (isReducedMotion) {
                    fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                } else {
                    fadeIn(tween(140)) togetherWith fadeOut(tween(100))
                }
            },
            label = "play_pause_icon"
        ) { playing ->
            Icon(
                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play",
                tint = contentColor,
                modifier = Modifier.size(size * 0.52f)
            )
        }
    }
}

/**
 * Spring-based tactile Favorite heart button
 */
@Composable
fun EchoHeartButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp
) {
    val isReducedMotion = LocalReducedMotion.current
    val activeTheme = LocalAppTheme.current

    val heartScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.15f else 1.0f,
        animationSpec = if (isReducedMotion) tween(0) else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "heart_bounce"
    )

    val tintColor by animateColorAsState(
        targetValue = if (isFavorite) activeTheme.primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(if (isReducedMotion) 0 else 200),
        label = "heart_tint"
    )

    IconButton(
        onClick = onToggle,
        modifier = modifier.scale(if (isReducedMotion) 1.0f else heartScale)
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (isFavorite) "Favorited" else "Favorite",
            tint = tintColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Motion chip with organic color/elevation transition
 */
@Composable
fun EchoFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isReducedMotion = LocalReducedMotion.current
    val activeTheme = LocalAppTheme.current

    val chipBg by animateColorAsState(
        targetValue = if (isSelected) activeTheme.primaryColor else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(if (isReducedMotion) 0 else 180),
        label = "chip_bg"
    )

    val chipText by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
        animationSpec = tween(if (isReducedMotion) 0 else 180),
        label = "chip_text"
    )

    Box(
        modifier = modifier
            .echoPressable(scaleOnPress = 0.94f, onClick = onClick)
            .clip(RoundedCornerShape(20.dp))
            .background(chipBg)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(
            text = text,
            color = chipText,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

// Backward-compatible Spotify wrappers
@Composable
fun SpotifyPlayFab(
    isPlaying: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    EchoPlayFab(isPlaying, onToggle, modifier, size)
}

@Composable
fun SpotifyHeartButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp
) {
    EchoHeartButton(isFavorite, onToggle, modifier, iconSize)
}

@Composable
fun SpotifyFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EchoFilterChip(text, isSelected, onClick, modifier)
}

@Composable
fun SpotifyPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}
