package com.spotify.clone.core.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * ECHO — Organic Sound Architecture Visual Identity
 * Liquid-inspired lowercase 'e' symbol rendered with precision Compose canvas paths.
 */
@Composable
fun EchoLiquidSymbol(
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    morphProgress: Float = 1f, // 0f = orb, 1f = fully formed 'e'
    glowAlpha: Float = 0.35f,
    isDarkMode: Boolean = true,
    enableBreathing: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "EchoAmbientPulse")
    val ambientPulse by if (enableBreathing) {
        infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(2800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ambientPulse"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    }

    val bubbleFloat by if (enableBreathing) {
        infiniteTransition.animateFloat(
            initialValue = -3f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bubbleFloat"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = ambientPulse
                scaleY = ambientPulse
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val scale = (w / 512f)

            // 1. Ambient Radial Glow behind the symbol
            val glowColor = if (isDarkMode) EchoLavender.copy(alpha = glowAlpha) else EchoLavender.copy(alpha = glowAlpha * 0.5f)
            val glowRadius = (w * 0.48f) * (0.6f + 0.4f * morphProgress)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor,
                        EchoCoral.copy(alpha = glowAlpha * 0.45f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = Offset(w * 0.5f, h * 0.5f)
            )

            if (morphProgress < 0.25f) {
                // Stage 1: Soft Translucent Awakening Orb
                val orbRadius = (w * 0.12f) + (w * 0.08f) * (morphProgress / 0.25f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            EchoLavender,
                            EchoCoral.copy(alpha = 0.7f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.45f, h * 0.45f),
                        radius = orbRadius
                    ),
                    radius = orbRadius,
                    center = Offset(w * 0.5f, h * 0.5f)
                )
                return@Canvas
            }

            // Stage 2+: Liquid 'e' Ribbon Interpolation
            // Outer ribbon gradient: Soft Blue -> Lavender -> Coral -> Peach
            val ribbonBrush = Brush.linearGradient(
                colors = listOf(
                    EchoSoftBlue,
                    EchoLavender,
                    EchoCoral,
                    Color(0xFFFFB3A3)
                ),
                start = Offset(w * 0.15f, h * 0.15f),
                end = Offset(w * 0.85f, h * 0.85f)
            )

            // Inner ribbon gradient: Coral -> Lavender -> Soft Blue
            val innerRibbonBrush = Brush.linearGradient(
                colors = listOf(
                    EchoCoral.copy(alpha = 0.92f),
                    EchoLavender.copy(alpha = 0.88f),
                    EchoSoftBlue.copy(alpha = 0.95f)
                ),
                start = Offset(w * 0.75f, h * 0.20f),
                end = Offset(w * 0.35f, h * 0.60f)
            )

            // Calculate interpolated path points from center orb outward
            fun pt(x: Float, y: Float): Offset {
                val cx = 256f
                val cy = 256f
                val targetX = x * scale
                val targetY = y * scale
                val centerX = cx * scale
                val centerY = cy * scale
                val f = ((morphProgress - 0.25f) / 0.75f).coerceIn(0f, 1f)
                val curX = centerX + (targetX - centerX) * f
                val curY = centerY + (targetY - centerY) * f
                return Offset(curX, curY)
            }

            // Path for the primary sweeping 'e' ribbon
            val mainRibbon = Path().apply {
                val p0 = pt(170f, 330f)
                moveTo(p0.x, p0.y)

                val c1a = pt(130f, 330f)
                val c1b = pt(95f, 295f)
                val p1 = pt(95f, 255f)
                cubicTo(c1a.x, c1a.y, c1b.x, c1b.y, p1.x, p1.y)

                val c2a = pt(95f, 235f)
                val c2b = pt(110f, 220f)
                val p2 = pt(130f, 220f)
                cubicTo(c2a.x, c2a.y, c2b.x, c2b.y, p2.x, p2.y)

                val c3a = pt(150f, 220f)
                val c3b = pt(160f, 235f)
                val p3 = pt(175f, 255f)
                cubicTo(c3a.x, c3a.y, c3b.x, c3b.y, p3.x, p3.y)

                val c4a = pt(190f, 275f)
                val c4b = pt(220f, 295f)
                val p4 = pt(260f, 295f)
                cubicTo(c4a.x, c4a.y, c4b.x, c4b.y, p4.x, p4.y)

                val c5a = pt(315f, 295f)
                val c5b = pt(360f, 255f)
                val p5 = pt(360f, 200f)
                cubicTo(c5a.x, c5a.y, c5b.x, c5b.y, p5.x, p5.y)

                val c6a = pt(360f, 145f)
                val c6b = pt(315f, 105f)
                val p6 = pt(255f, 105f)
                cubicTo(c6a.x, c6a.y, c6b.x, c6b.y, p6.x, p6.y)

                val c7a = pt(175f, 105f)
                val c7b = pt(120f, 165f)
                val p7 = pt(120f, 255f)
                cubicTo(c7a.x, c7a.y, c7b.x, c7b.y, p7.x, p7.y)

                val c8a = pt(120f, 345f)
                val c8b = pt(185f, 410f)
                val p8 = pt(280f, 410f)
                cubicTo(c8a.x, c8a.y, c8b.x, c8b.y, p8.x, p8.y)

                val c9a = pt(340f, 410f)
                val c9b = pt(395f, 380f)
                val p9 = pt(435f, 340f)
                cubicTo(c9a.x, c9a.y, c9b.x, c9b.y, p9.x, p9.y)

                val c10a = pt(445f, 330f)
                val c10b = pt(440f, 315f)
                val p10 = pt(425f, 315f)
                cubicTo(c10a.x, c10a.y, c10b.x, c10b.y, p10.x, p10.y)

                val c11a = pt(410f, 315f)
                val c11b = pt(395f, 325f)
                val p11 = pt(380f, 335f)
                cubicTo(c11a.x, c11a.y, c11b.x, c11b.y, p11.x, p11.y)

                val c12a = pt(350f, 355f)
                val c12b = pt(315f, 365f)
                val p12 = pt(275f, 365f)
                cubicTo(c12a.x, c12a.y, c12b.x, c12b.y, p12.x, p12.y)

                val c13a = pt(205f, 365f)
                val c13b = pt(165f, 315f)
                val p13 = pt(165f, 255f)
                cubicTo(c13a.x, c13a.y, c13b.x, c13b.y, p13.x, p13.y)

                val c14a = pt(165f, 240f)
                val c14b = pt(170f, 225f)
                val p14 = pt(180f, 215f)
                cubicTo(c14a.x, c14a.y, c14b.x, c14b.y, p14.x, p14.y)

                val c15a = pt(200f, 195f)
                val c15b = pt(235f, 185f)
                val p15 = pt(270f, 185f)
                cubicTo(c15a.x, c15a.y, c15b.x, c15b.y, p15.x, p15.y)

                val c16a = pt(325f, 185f)
                val c16b = pt(365f, 215f)
                val p16 = pt(365f, 255f)
                cubicTo(c16a.x, c16a.y, c16b.x, c16b.y, p16.x, p16.y)

                val c17a = pt(365f, 305f)
                val c17b = pt(315f, 330f)
                val p17 = pt(255f, 330f)
                cubicTo(c17a.x, c17a.y, c17b.x, c17b.y, p17.x, p17.y)

                close()
            }
            drawPath(mainRibbon, ribbonBrush)

            // Inner Glass Loop Fold
            val innerLoop = Path().apply {
                val p0 = pt(255f, 105f)
                moveTo(p0.x, p0.y)

                val c1a = pt(315f, 105f)
                val c1b = pt(360f, 145f)
                val p1 = pt(360f, 200f)
                cubicTo(c1a.x, c1a.y, c1b.x, c1b.y, p1.x, p1.y)

                val c2a = pt(360f, 240f)
                val c2b = pt(335f, 270f)
                val p2 = pt(290f, 280f)
                cubicTo(c2a.x, c2a.y, c2b.x, c2b.y, p2.x, p2.y)

                val c3a = pt(250f, 290f)
                val c3b = pt(205f, 280f)
                val p3 = pt(175f, 255f)
                cubicTo(c3a.x, c3a.y, c3b.x, c3b.y, p3.x, p3.y)

                val c4a = pt(200f, 210f)
                val c4b = pt(245f, 185f)
                val p4 = pt(290f, 185f)
                cubicTo(c4a.x, c4a.y, c4b.x, c4b.y, p4.x, p4.y)

                val c5a = pt(325f, 185f)
                val c5b = pt(350f, 200f)
                val p5 = pt(350f, 220f)
                cubicTo(c5a.x, c5a.y, c5b.x, c5b.y, p5.x, p5.y)

                val c6a = pt(350f, 240f)
                val c6b = pt(320f, 255f)
                val p6 = pt(280f, 255f)
                cubicTo(c6a.x, c6a.y, c6b.x, c6b.y, p6.x, p6.y)

                val c7a = pt(240f, 255f)
                val c7b = pt(210f, 240f)
                val p7 = pt(205f, 220f)
                cubicTo(c7a.x, c7a.y, c7b.x, c7b.y, p7.x, p7.y)

                val c8a = pt(215f, 160f)
                val c8b = pt(260f, 115f)
                val p8 = pt(320f, 110f)
                cubicTo(c8a.x, c8a.y, c8b.x, c8b.y, p8.x, p8.y)

                close()
            }
            drawPath(innerLoop, innerRibbonBrush)

            // Specular Highlight Crest Rim
            val highlightPath = Path().apply {
                val start = pt(140f, 185f)
                moveTo(start.x, start.y)
                val c1 = pt(170f, 130f)
                val c2 = pt(215f, 110f)
                val p1 = pt(265f, 110f)
                cubicTo(c1.x, c1.y, c2.x, c2.y, p1.x, p1.y)

                val c3 = pt(320f, 110f)
                val c4 = pt(355f, 140f)
                val p2 = pt(355f, 185f)
                cubicTo(c3.x, c3.y, c4.x, c4.y, p2.x, p2.y)
            }
            drawPath(
                path = highlightPath,
                color = Color.White.copy(alpha = 0.75f * morphProgress),
                style = Stroke(width = 7f * scale, cap = StrokeCap.Round)
            )

            // Lower Tail Specular Line
            val tailPath = Path().apply {
                val start = pt(260f, 375f)
                moveTo(start.x, start.y)
                val c1 = pt(310f, 370f)
                val c2 = pt(360f, 350f)
                val p1 = pt(400f, 320f)
                cubicTo(c1.x, c1.y, c2.x, c2.y, p1.x, p1.y)
            }
            drawPath(
                path = tailPath,
                color = Color.White.copy(alpha = 0.55f * morphProgress),
                style = Stroke(width = 4f * scale, cap = StrokeCap.Round)
            )

            // Micro-Orb Sound Droplets (with gentle floating movement)
            if (morphProgress > 0.4f) {
                val fOrb = ((morphProgress - 0.4f) / 0.6f).coerceIn(0f, 1f)
                val orb1Center = Offset(
                    115f * scale,
                    (240f + bubbleFloat) * scale
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.95f), EchoSoftBlue, EchoLavender.copy(alpha = 0.5f)),
                        center = orb1Center - Offset(3f * scale, 3f * scale),
                        radius = 16f * scale
                    ),
                    radius = 16f * scale * fOrb,
                    center = orb1Center
                )

                val orb2Center = Offset(
                    430f * scale,
                    (305f - bubbleFloat) * scale
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.95f), EchoCoral, EchoLavender.copy(alpha = 0.5f)),
                        center = orb2Center - Offset(2f * scale, 2f * scale),
                        radius = 14f * scale
                    ),
                    radius = 14f * scale * fOrb,
                    center = orb2Center
                )
            }
        }
    }
}

/**
 * ECHO — Wordmark Typography ('echo' + 'Feel the sound.')
 */
@Composable
fun EchoWordmark(
    modifier: Modifier = Modifier,
    titleSize: TextUnit = 48.sp,
    taglineSize: TextUnit = 16.sp,
    isDarkMode: Boolean = true,
    taglineAlpha: Float = 1f,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally
) {
    val wordmarkColor = if (isDarkMode) Color(0xFFF6F3FA) else Color(0xFF101016)
    val taglineColor = if (isDarkMode) Color(0xFFA6A2B3) else Color(0xFF777680)

    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        Text(
            text = "echo",
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            color = wordmarkColor,
            letterSpacing = (-1.5).sp,
            fontFamily = FontFamily.SansSerif
        )

        if (taglineAlpha > 0f) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Feel the sound.",
                fontSize = taglineSize,
                fontWeight = FontWeight.Normal,
                color = taglineColor.copy(alpha = taglineAlpha),
                letterSpacing = 0.4.sp,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

/**
 * ECHO — Horizontal Brand Logo (Symbol + Wordmark + Tagline)
 */
@Composable
fun EchoHorizontalLogo(
    modifier: Modifier = Modifier,
    symbolSize: Dp = 56.dp,
    titleSize: TextUnit = 28.sp,
    taglineSize: TextUnit = 12.sp,
    isDarkMode: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        EchoLiquidSymbol(
            size = symbolSize,
            glowAlpha = 0.25f,
            isDarkMode = isDarkMode,
            enableBreathing = true
        )
        Spacer(modifier = Modifier.width(14.dp))
        EchoWordmark(
            titleSize = titleSize,
            taglineSize = taglineSize,
            isDarkMode = isDarkMode,
            horizontalAlignment = Alignment.Start
        )
    }
}
