package com.spotify.clone.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.clone.core.designsystem.AppThemeOption
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoPrismAqua
import com.spotify.clone.core.designsystem.EchoPrismLavender
import com.spotify.clone.core.designsystem.EchoPrismRose
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.echoSpecularBorder
import com.spotify.clone.core.designsystem.resolvedBorder

enum class NavigationTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    SEARCH("search", "Search", Icons.Filled.Search, Icons.Outlined.Search),
    LIBRARY("library", "Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic)
}

/**
 * Apple-inspired floating translucent capsule navigation bar with a spring-gliding
 * active selection pill and tactile pointer-down feedback.
 */
@Composable
fun EchoBottomNavBar(
    currentRoute: String,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalAppTheme.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val isReducedMotion = LocalReducedMotion.current

    val capsuleBg = if (isDarkMode) Color(0xFC151519) else Color(0xFCFFFFFF)
    val capsuleBorder = activeTheme.resolvedBorder(isDarkMode)
    val activeColor = activeTheme.primaryColor
    val inactiveColor = if (isDarkMode) Color(0xFF9E9EA7) else Color(0xFF6E6E75)
    val capsuleShape = RoundedCornerShape(30.dp)

    val selectedIndex = NavigationTab.entries.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .height(60.dp)
                .shadow(
                    elevation = if (isDarkMode) 10.dp else 14.dp,
                    shape = capsuleShape,
                    ambientColor = Color.Black.copy(alpha = 0.12f),
                    spotColor = Color.Black.copy(alpha = 0.18f)
                )
                .clip(capsuleShape)
                .background(capsuleBg)
                .echoSpecularBorder(
                    isDarkMode = isDarkMode,
                    shape = capsuleShape,
                    fallbackBorder = capsuleBorder
                )
                .padding(horizontal = 6.dp, vertical = 5.dp)
        ) {
            val tabCount = NavigationTab.entries.size
            val tabWidth = maxWidth / tabCount

            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                animationSpec = if (isReducedMotion) {
                    tween(120)
                } else {
                    EchoMotion.springMomentum()
                },
                label = "nav_pill_offset"
            )

            // Sliding translucent selection pill behind active tab
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(25.dp))
                    .then(
                        if (activeTheme == AppThemeOption.PASTEL_PRISM) {
                            Modifier.background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(
                                        EchoPrismRose.copy(alpha = if (isDarkMode) 0.22f else 0.18f),
                                        EchoPrismLavender.copy(alpha = if (isDarkMode) 0.22f else 0.18f),
                                        EchoPrismAqua.copy(alpha = if (isDarkMode) 0.22f else 0.18f)
                                    )
                                )
                            )
                        } else {
                            Modifier.background(
                                activeColor.copy(alpha = if (isDarkMode) 0.16f else 0.11f)
                            )
                        }
                    )
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationTab.entries.forEach { tab ->
                    val isSelected = currentRoute == tab.route
                    val tint by animateColorAsState(
                        targetValue = if (isSelected) activeColor else inactiveColor,
                        animationSpec = tween(EchoMotion.DurationMicro, easing = EchoMotion.EaseOutStrong),
                        label = "nav_tab_tint"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected && !isReducedMotion) 1.08f else 1.0f,
                        animationSpec = EchoMotion.SpringBouncy,
                        label = "nav_icon_scale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(25.dp))
                            .echoPressable(
                                scaleOnPress = 0.93f,
                                hapticOnClick = true
                            ) { onTabSelected(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                tint = tint,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.title,
                                color = tint,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
