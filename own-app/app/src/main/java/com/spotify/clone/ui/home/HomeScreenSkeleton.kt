package com.spotify.clone.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.resolvedBackground

@Composable
fun shimmerBrush(isDarkMode: Boolean): Brush {
    val isReducedMotion = LocalReducedMotion.current
    val baseColor = if (isDarkMode) Color(0xFF18181B) else Color(0xFFE8E8ED)
    val highlightColor = if (isDarkMode) Color(0xFF27272C) else Color(0xFFF5F5FA)

    if (isReducedMotion) {
        return Brush.linearGradient(colors = listOf(baseColor, baseColor))
    }

    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -320f,
        targetValue = 1320f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim - 360f, translateAnim - 360f),
        end = Offset(translateAnim, translateAnim)
    )
}

@Composable
fun HomeScreenSkeleton(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalAppTheme.current
    val brush = shimmerBrush(isDarkMode)
    val screenBg = activeTheme.resolvedBackground(isDarkMode)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 176.dp),
        userScrollEnabled = false
    ) {
        // Header skeleton
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(width = 118.dp, height = 12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 176.dp, height = 34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(brush)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(brush)
                    )
                }
            }
        }

        // Filter chips skeleton
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(52.dp, 76.dp, 94.dp, 96.dp, 72.dp).forEach { w ->
                    Box(
                        modifier = Modifier
                            .size(width = w, height = 34.dp)
                            .clip(RoundedCornerShape(50))
                            .background(brush)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Jump Back In 2-col bento skeleton
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        repeat(2) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(brush)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
        }

        // Editorial AI Showcase skeleton
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .size(width = 168.dp, height = 22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(3) {
                    Box(
                        modifier = Modifier
                            .width(304.dp)
                            .height(234.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(brush)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // New Songs Today shelf skeleton
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .size(width = 152.dp, height = 22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(4) {
                    Box(
                        modifier = Modifier
                            .width(152.dp)
                            .height(198.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(brush)
                    )
                }
            }
        }
    }
}
