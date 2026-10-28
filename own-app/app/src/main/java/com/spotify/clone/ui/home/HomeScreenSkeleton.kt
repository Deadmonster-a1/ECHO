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

/**
 * Creates a sweeping linear gradient shimmer brush for authentic skeleton loaders.
 */
@Composable
fun shimmerBrush(isDarkMode: Boolean): Brush {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1300f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val baseColor = if (isDarkMode) Color(0xFF1E1C27) else Color(0xFFE5E2DC)
    val highlightColor = if (isDarkMode) Color(0xFF2E2B3C) else Color(0xFFF3F0EA)

    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim - 350f, translateAnim - 350f),
        end = Offset(translateAnim, translateAnim)
    )
}

/**
 * High-fidelity Skeleton Screen for ECHO Home feed.
 * Accurately mirrors every component on the real Home Screen:
 * - Top Bar (Avatar, Logo, Action pills)
 * - Personalized Greeting Header
 * - Top Filter Chips (All, Music, Podcasts)
 * - 2x3 "Jump Back In" Quick Access Grid
 * - Daylist / Flow Banner
 * - Mood Filter Row
 * - Horizontal Blockbuster Album Carousel
 * - Horizontal Made For You Mixes Carousel
 */
@Composable
fun HomeScreenSkeleton(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val brush = shimmerBrush(isDarkMode = isDarkMode)
    val screenBg = if (isDarkMode) Color(0xFF121212) else Color(0xFFF7F5F2)
    val cardBg = if (isDarkMode) Color(0xFF191722) else Color(0xFFFFFFFF)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
            userScrollEnabled = false
        ) {
            // 1. Top Bar Skeleton
            item(key = "sk_top_bar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // User Avatar
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(brush)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        // Logo pill
                        Box(
                            modifier = Modifier
                                .size(width = 72.dp, height = 24.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(brush)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Action buttons
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(brush)
                        )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(brush)
                        )
                    }
                }
            }

            // 2. Personalized Greeting Skeleton
            item(key = "sk_greeting") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 190.dp, height = 26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 120.dp, height = 13.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. Filter Chips Skeleton
            item(key = "sk_filter_chips") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(54.dp, 72.dp, 90.dp).forEach { width ->
                        Box(
                            modifier = Modifier
                                .size(width = width, height = 32.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(brush)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // 4. 2x3 Quick Access Grid Skeleton (Jump Back In)
            item(key = "sk_quick_access_grid") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(2) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(cardBg)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Card artwork placeholder
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .background(brush)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 80.dp, height = 11.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(brush)
                                            )
                                            Spacer(modifier = Modifier.height(5.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 46.dp, height = 9.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(brush)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 5. Daylist / Flow Card Skeleton
            item(key = "sk_daylist_card") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(105.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 6. Mood Filter Row Skeleton
            item(key = "sk_moods") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(5) {
                        Box(
                            modifier = Modifier
                                .size(width = 74.dp, height = 30.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(brush)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(26.dp))
            }

            // 7. Section 1 Carousel Skeleton (Blockbuster Albums)
            item(key = "sk_section_1") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(width = 220.dp, height = 20.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(brush)
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .size(width = 150.dp, height = 12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(brush)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        userScrollEnabled = false
                    ) {
                        items(4) {
                            Column(modifier = Modifier.width(150.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(150.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(brush)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(width = 115.dp, height = 13.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(brush)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(width = 75.dp, height = 11.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(brush)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // 8. Section 2 Carousel Skeleton (Made For You)
            item(key = "sk_section_2") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(width = 190.dp, height = 20.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(brush)
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .size(width = 130.dp, height = 12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(brush)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        userScrollEnabled = false
                    ) {
                        items(4) {
                            Column(modifier = Modifier.width(150.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(150.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(brush)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(width = 100.dp, height = 13.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(brush)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(width = 65.dp, height = 11.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(brush)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
