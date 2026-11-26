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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.LocalReducedMotion

@Composable
fun shimmerBrush(isDarkMode: Boolean): Brush {
    val isReducedMotion = LocalReducedMotion.current
    val baseColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlightColor = MaterialTheme.colorScheme.surfaceContainerHighest

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
    val brush = shimmerBrush(isDarkMode)
    val screenBg = MaterialTheme.colorScheme.background

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 176.dp),
        userScrollEnabled = false
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 112.dp, height = 12.dp)
                            .clip(RoundedCornerShape(EchoRadius.pill))
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 156.dp, height = 22.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(brush)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(64.dp, 86.dp, 72.dp, 80.dp).forEach { w ->
                    Box(
                        modifier = Modifier
                            .size(width = w, height = 38.dp)
                            .clip(RoundedCornerShape(EchoRadius.pill))
                            .background(brush)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

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
                                    .height(62.dp)
                                    .clip(RoundedCornerShape(EchoRadius.md))
                                    .background(brush)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(26.dp))
        }

        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .size(width = 148.dp, height = 24.dp)
                    .clip(RoundedCornerShape(8.dp))
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
                            .height(216.dp)
                            .clip(RoundedCornerShape(EchoRadius.xl))
                            .background(brush)
                    )
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }

        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .size(width = 140.dp, height = 24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(4) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(152.dp)
                                .clip(RoundedCornerShape(EchoRadius.lg))
                                .background(brush)
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 112.dp, height = 14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(brush)
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 80.dp, height = 12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(brush)
                        )
                    }
                }
            }
        }
    }
}
