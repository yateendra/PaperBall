package com.example.paperball.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(onDismiss: () -> Unit) {
    var showContent by remember { mutableStateOf(false) }

    // Animated ball position
    val infiniteTransition = rememberInfiniteTransition(label = "ball")
    val ballY by infiniteTransition.animateFloat(
        initialValue = 100f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ballY"
    )

    val ballRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Fade in animation
    val alpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(800),
        label = "alpha"
    )

    // Scale animation for icon
    val iconScale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )

    LaunchedEffect(Unit) {
        delay(200)
        showContent = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(Color(0xFF333333), Color(0xFF000000)),
                    center = Offset(400f, 400f),
                    radius = 2000f
                )
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // App Icon - Large Premium Golf Ball
            Canvas(
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                        rotationZ = ballRotation * 0.5f
                    }
            ) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val radius = size.minDimension / 2.2f

                // Poly-White Volume
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFF0F0F0), Color(0xFFB0B0B0)),
                        center = Offset(centerX - radius * 0.3f, centerY - radius * 0.3f),
                        radius = radius * 1.5f
                    ),
                    radius = radius,
                    center = Offset(centerX, centerY)
                )

                // Dimple pattern
                val random = kotlin.random.Random(42)
                repeat(45) { i ->
                    val angle = (i * 8f)
                    val rad = Math.toRadians(angle.toDouble())
                    val dist = radius * (0.2f + (i % 3) * 0.25f)
                    val dimpleX = centerX + cos(rad).toFloat() * dist
                    val dimpleY = centerY + sin(rad).toFloat() * dist

                    drawCircle(
                        color = Color.Black.copy(alpha = 0.1f),
                        radius = radius * 0.1f,
                        center = Offset(dimpleX, dimpleY)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                "PREMIUM",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFD700).copy(alpha = alpha),
                letterSpacing = 8.sp,
                textAlign = TextAlign.Center
            )

            Text(
                "PAPER BALL",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = alpha),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "EXECUTIVE EDITION",
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFFFFD700).copy(alpha = alpha * 0.8f),
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(64.dp))

            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )

            Text(
                "TAP TO START",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = pulseAlpha),
                letterSpacing = 4.sp
            )
        }

        Text(
            "© 2026 Paper Ball Pro",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.4f)
        )
    }
}
