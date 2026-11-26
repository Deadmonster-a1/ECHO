package com.spotify.clone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.echoPressable

data class CategoryItem(
    val id: String,
    val title: String,
    val backgroundColor: Color,
    val imageUrl: String = "",
    val searchQuery: String = title
)

/**
 * Material 3 Expressive Genre Category Tile with generous 24.dp shape,
 * playful tilted artwork framing, subtle circular tonal blob accent, and bold expressive display typography.
 */
@Composable
fun CategoryCard(
    category: CategoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = remember { RoundedCornerShape(EchoRadius.lg) }
    val thumbShape = remember { RoundedCornerShape(EchoRadius.sm) }
    val base = category.backgroundColor
    val gradient = remember(base) {
        Brush.linearGradient(
            colors = listOf(
                base,
                base.copy(
                    red = (base.red * 0.72f).coerceIn(0f, 1f),
                    green = (base.green * 0.72f).coerceIn(0f, 1f),
                    blue = (base.blue * 0.85f).coerceIn(0f, 1f)
                )
            )
        )
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(102.dp)
            .shadow(4.dp, cardShape, spotColor = base.copy(alpha = 0.28f))
            .clip(cardShape)
            .background(gradient)
            .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true, onClick = onClick)
    ) {
        // Expressive organic background orb accent
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 18.dp, y = 18.dp)
                .size(92.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
        )

        if (category.imageUrl.isNotBlank()) {
            AsyncImage(
                model = category.imageUrl,
                contentDescription = category.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 10.dp, y = 10.dp)
                    .rotate(14f)
                    .size(64.dp)
                    .shadow(6.dp, thumbShape)
                    .clip(thumbShape)
                    .background(Color.Black.copy(alpha = 0.18f))
            )
        }

        Text(
            text = category.title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 16.dp, end = 60.dp)
        )
    }
}
