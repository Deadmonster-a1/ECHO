package com.spotify.clone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun CategoryCard(
    category: CategoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(EchoRadius.md)
    val thumbShape = RoundedCornerShape(EchoRadius.xs)
    val base = category.backgroundColor
    val gradient = Brush.linearGradient(
        colors = listOf(
            base,
            base.copy(
                red = (base.red * 0.78f).coerceIn(0f, 1f),
                green = (base.green * 0.78f).coerceIn(0f, 1f),
                blue = (base.blue * 0.86f).coerceIn(0f, 1f)
            )
        )
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(cardShape)
            .background(gradient)
            .echoPressable(scaleOnPress = 0.97f, hapticOnClick = true, onClick = onClick)
    ) {
        if (category.imageUrl.isNotBlank()) {
            AsyncImage(
                model = category.imageUrl,
                contentDescription = category.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 8.dp)
                    .rotate(12f)
                    .size(60.dp)
                    .shadow(4.dp, thumbShape)
                    .clip(thumbShape)
                    .background(Color.Black.copy(alpha = 0.18f))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to Color.Black.copy(alpha = 0.14f),
                        0.65f to Color.Transparent
                    )
                )
        )

        Text(
            text = category.title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 14.dp, end = 58.dp)
        )
    }
}
