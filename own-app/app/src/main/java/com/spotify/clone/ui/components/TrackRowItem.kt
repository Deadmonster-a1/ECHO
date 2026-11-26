package com.spotify.clone.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.clone.core.designsystem.EchoArtworkImage
import com.spotify.clone.core.designsystem.EchoMarqueeText
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.LivingEqualizer
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.SwipeableTrackRow
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.model.Track

@Composable
fun TrackRowItem(
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
    onSwipeQueue: (() -> Unit)? = null,
    onSwipeFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isReducedMotion = LocalReducedMotion.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val titleColor by animateColorAsState(
        targetValue = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(EchoMotion.DurationMicro, easing = EchoMotion.EaseOutStrong),
        label = "track_row_title_color"
    )
    val rowHighlight by animateColorAsState(
        targetValue = if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f) else Color.Transparent,
        animationSpec = tween(EchoMotion.DurationStandard),
        label = "track_row_bg"
    )
    val rowCorner by animateDpAsState(
        targetValue = if (isPlaying) EchoRadius.md else 14.dp,
        animationSpec = if (isReducedMotion) tween(100) else EchoMotion.spatialFast(),
        label = "track_row_corner"
    )

    val rowContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(rowCorner))
                .background(rowHighlight)
                .echoPressable(scaleOnPress = 0.99f, onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EchoArtworkImage(
                imageUrl = track.coverArtUrl,
                contentDescription = track.title,
                seedKey = "${track.title}_${track.artistName}",
                iconSize = 20.dp,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(
                        visible = isPlaying,
                        enter = fadeIn(tween(160)) + expandHorizontally(tween(180, easing = EchoMotion.EaseOutStrong)),
                        exit = fadeOut(tween(120)) + shrinkHorizontally(tween(160))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LivingEqualizer(
                                isPlaying = true,
                                barCount = 3,
                                barColor = primaryColor,
                                maxHeight = 13.dp,
                                barWidth = 2.5.dp,
                                barSpacing = 2.dp
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                        }
                    }
                    EchoMarqueeText(
                        text = track.title,
                        color = titleColor,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        enabled = isPlaying
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (track.isExplicit) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "E",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    val subtitle = if (track.albumName.isNotBlank() && !track.albumName.equals("Single", ignoreCase = true)) {
                        "${track.artistName} • ${track.albumName}"
                    } else {
                        track.artistName
                    }
                    Text(
                        text = subtitle,
                        color = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onMoreClick != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .echoPressable(scaleOnPress = 0.88f, onClick = onMoreClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = "More options",
                        tint = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (onSwipeQueue != null) {
        SwipeableTrackRow(
            isFavorite = track.isFavorite,
            onSwipeQueue = onSwipeQueue,
            onSwipeFavorite = onSwipeFavorite,
            modifier = modifier
        ) {
            rowContent()
        }
    } else {
        Box(modifier = modifier) {
            rowContent()
        }
    }
}
