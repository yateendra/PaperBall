package com.example.paperball.ui.game

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paperball.models.Ball
import com.example.paperball.models.Bin
import com.example.paperball.models.GameState
import com.example.paperball.models.Particle
import com.example.paperball.utils.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.cos
import kotlin.random.Random
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight

@Composable
fun PaperBallGame() {
    val context = LocalContext.current
    val soundManager = remember { SoundManager() }
    val config = LocalConfiguration.current
    val density = LocalDensity.current

    // Convert dp to px ONCE at composable level (not in models)
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    val ballRadiusPx = with(density) { 24.dp.toPx() }

    val floorY = screenHeightPx * 0.85f
    val cupHeight = 420f
    
    var gameState by remember {
        mutableStateOf(GameState(
            ball = Ball(position = Offset(screenWidthPx * 0.2f, floorY - ballRadiusPx)),
            bin = Bin(position = Offset(screenWidthPx * 0.75f, floorY - cupHeight))
        ))
    }

    // Bin position - moves based on score for difficulty
    val binX = remember(gameState.score) {
        when {
            gameState.score < 3 -> screenWidthPx * 0.75f
            gameState.score < 6 -> screenWidthPx * 0.65f
            gameState.score < 10 -> screenWidthPx * 0.55f
            else -> screenWidthPx * (0.4f + (gameState.score % 3) * 0.15f)
        }
    }
    val binY = screenHeightPx * 0.7f

    val scope = rememberCoroutineScope()

    // Particle animation
    LaunchedEffect(gameState.particles) {
        if (gameState.particles.isNotEmpty()) {
            while (gameState.particles.isNotEmpty()) {
                delay(16)
                gameState = gameState.copy(
                    particles = gameState.particles.mapNotNull { particle ->
                        val newAlpha = particle.alpha - 0.02f
                        if (newAlpha <= 0f) null
                        else particle.copy(
                            position = Offset(
                                particle.position.x + particle.velocity.x,
                                particle.position.y + particle.velocity.y
                            ),
                            velocity = Offset(
                                particle.velocity.x * 0.98f,
                                particle.velocity.y + 0.5f
                            ),
                            alpha = newAlpha
                        )
                    }
                )
            }
        }
    }

    // Physics simulation
    LaunchedEffect(gameState.isAnimating) {
        if (gameState.isAnimating && !gameState.ball.inHand) {
            var vx = gameState.ball.velocity.x
            var vy = gameState.ball.velocity.y
            var x = gameState.ball.position.x
            var y = gameState.ball.position.y
            var rotation = gameState.ball.rotation
            var scale = 1f
            val gravity = 4000f
            val drag = 0.99f
            val bounceDamping = 0.6f
            
            while (y < screenHeightPx + 100) {
                vy += gravity * 0.016f
                vx *= drag
                
                x += vx * 0.016f
                y += vy * 0.016f
                
                // Rotation based on velocity (3D tumbling effect)
                rotation += (vx * 0.5f + vy * 0.3f) * 0.016f
                
                // Keep scale constant - no zoom effect
                scale = 1f

                // Top boundary - bounce back if going too high
                if (y < ballRadiusPx && vy < 0) {
                    vy = -vy * bounceDamping
                    y = ballRadiusPx
                    soundManager.playSound(SoundManager.SoundType.BOUNCE)
                }

                // Wall bounces - keep ball on screen
                if (x < ballRadiusPx) {
                    vx = -vx * bounceDamping
                    x = ballRadiusPx
                    soundManager.playSound(SoundManager.SoundType.BOUNCE)
                } else if (x > screenWidthPx - ballRadiusPx) {
                    vx = -vx * bounceDamping
                    x = screenWidthPx - ballRadiusPx
                    soundManager.playSound(SoundManager.SoundType.BOUNCE)
                }

                // Floor bounce
                if (y > screenHeightPx * 0.85f - ballRadiusPx && vy > 0) {
                    if (abs(vy) > 100f) {
                        vy = -vy * bounceDamping
                        y = screenHeightPx * 0.85f - ballRadiusPx
                        vx *= 0.9f // Extra friction on floor
                        soundManager.playSound(SoundManager.SoundType.BOUNCE)
                    } else {
                        // Ball stopped bouncing on floor - check if missed
                        gameState = gameState.copy(
                            combo = 0
                        )
                        break
                    }
                }

                // Cup Side Wall Bounces (Below the rim)
                val cupWidth = 200f
                val rimHeight = 30f
                val rimTop = binY + rimHeight
                val cupBottom = binY + 250f
                
                if (y > rimTop && y < cupBottom) {
                    if (x + ballRadiusPx > binX && x < binX + 20f && vx > 0) {
                        vx = -abs(vx) * bounceDamping
                        x = binX - ballRadiusPx
                    } else if (x - ballRadiusPx < binX + cupWidth && x > binX + cupWidth - 20f && vx < 0) {
                        vx = abs(vx) * bounceDamping
                        x = binX + cupWidth + ballRadiusPx
                    }
                }

                // Tightened Scoring Logic: Cross aperture from ABOVE
                val inAperture = x in (binX + 25f)..(binX + cupWidth - 25f)
                val crossedRimPlane = y > rimTop && (y - vy * 0.016f) <= rimTop

                if (inAperture && crossedRimPlane && vy > 0) {
                    soundManager.playSound(SoundManager.SoundType.SWISH)
                    val newCombo = gameState.combo + 1
                    val bonusPoints = if (newCombo > 1) newCombo else 1
                    val newScore = gameState.score + bonusPoints
                    
                    // Create celebration particles
                    val particles = List(20) {
                        Particle(
                            position = Offset(binX + 40f, binY),
                            velocity = Offset(
                                Random.nextFloat() * 10f - 5f,
                                Random.nextFloat() * -15f - 5f
                            ),
                            color = listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFF6B6B),
                                Color(0xFF4ECDC4),
                                Color(0xFF95E1D3)
                            ).random()
                        )
                    }
                    
                    val scoreMsg = if (newCombo > 1) "🔥 ${bonusPoints}x COMBO!" else " Scored!"
                    Toast.makeText(context, scoreMsg, Toast.LENGTH_SHORT).show()
                    
                    gameState = gameState.copy(
                        score = newScore,
                        highScore = maxOf(newScore, gameState.highScore),
                        combo = newCombo,
                        showMessage = null,
                        particles = particles
                    )
                    break
                }

                // Update ball position
                gameState = gameState.copy(
                    ball = gameState.ball.copy(
                        position = Offset(x, y),
                        velocity = Offset(vx, vy),
                        rotation = rotation,
                        scale = scale
                    )
                )
                delay(16)
            }

            delay(1000)
            // Reset ball
            gameState = gameState.copy(
                ball = Ball(position = Offset(screenWidthPx * 0.2f, floorY - ballRadiusPx)),
                isAnimating = false,
                attempts = gameState.attempts + 1,
                showMessage = null
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4E04D)) // Yellow notepad color
    ) {
        // Horizontal notepad lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineSpacing = 60f
            val lineCount = (size.height / lineSpacing).toInt()
            
            repeat(lineCount) { i ->
                val y = i * lineSpacing
                drawLine(
                    color = Color(0xFFD4C03D).copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 2f
                )
            }
        }

        // Checkered floor pattern at bottom
        Canvas(modifier = Modifier.fillMaxSize()) {
            val floorY = size.height * 0.85f
            val checkSize = 30f
            val checksX = (size.width / checkSize).toInt() + 1
            
            repeat(checksX) { i ->
                val isBlack = i % 2 == 0
                drawRect(
                    color = if (isBlack) Color.Black else Color.White,
                    topLeft = Offset(i * checkSize, floorY),
                    size = androidx.compose.ui.geometry.Size(checkSize, checkSize)
                )
            }
        }

        // White Coffee Cup (bigger size)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cupWidth = 360f
            val cupHeight = 420f
            val cupBottom = binY + cupHeight
            
            // Cup shadow
            drawOval(
                color = Color.Black.copy(alpha = 0.15f),
                topLeft = Offset(binX - 10f, cupBottom - 5f),
                size = androidx.compose.ui.geometry.Size(cupWidth + 20f, 25f)
            )
            
            // Cup body - trapezoid (wider at top)
            val cupPath = Path().apply {
                moveTo(binX + 30f, cupBottom) // Bottom left
                lineTo(binX, binY + 30f) // Top left
                lineTo(binX + cupWidth, binY + 30f) // Top right
                lineTo(binX + cupWidth - 30f, cupBottom) // Bottom right
                close()
            }
            
            // Main cup body - white
            drawPath(
                path = cupPath,
                color = Color(0xFFF5F5F5)
            )
            
            // Left side shading for 3D effect
            val leftShade = Path().apply {
                moveTo(binX + 30f, cupBottom)
                lineTo(binX, binY + 30f)
                lineTo(binX + 20f, binY + 30f)
                lineTo(binX + 40f, cupBottom)
                close()
            }
            drawPath(
                path = leftShade,
                color = Color(0xFFE0E0E0)
            )
            
            // Right side highlight
            val rightHighlight = Path().apply {
                moveTo(binX + cupWidth - 30f, cupBottom)
                lineTo(binX + cupWidth, binY + 30f)
                lineTo(binX + cupWidth - 20f, binY + 30f)
                lineTo(binX + cupWidth - 40f, cupBottom)
                close()
            }
            drawPath(
                path = rightHighlight,
                color = Color.White
            )
            
            // Cup rim - ellipse for perspective
            drawOval(
                color = Color(0xFFE8E8E8),
                topLeft = Offset(binX - 5f, binY + 25f),
                size = androidx.compose.ui.geometry.Size(cupWidth + 10f, 20f)
            )
            
            // Inner rim (darker)
            drawOval(
                color = Color(0xFFD0D0D0),
                topLeft = Offset(binX + 5f, binY + 28f),
                size = androidx.compose.ui.geometry.Size(cupWidth - 10f, 15f)
            )
            
            // Cup opening (dark inside)
            drawOval(
                color = Color(0xFF3D3D3D),
                topLeft = Offset(binX + 15f, binY + 30f),
                size = androidx.compose.ui.geometry.Size(cupWidth - 30f, 12f)
            )
            
            // Rim highlight
            drawOval(
                color = Color.White.copy(alpha = 0.6f),
                topLeft = Offset(binX + 10f, binY + 26f),
                size = androidx.compose.ui.geometry.Size(cupWidth - 20f, 8f)
            )
            
            // Subtle vertical lines for texture
            repeat(3) { i ->
                val xPos = binX + 50f + i * 30f
                drawLine(
                    color = Color(0xFFE0E0E0),
                    start = Offset(xPos, binY + 50f),
                    end = Offset(xPos + 8f, cupBottom - 20f),
                    strokeWidth = 1.5f
                )
            }
        }

        // Trajectory preview and flick indicator when dragging
        if (gameState.isDragging && !gameState.isAnimating) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val power = 8f
                val throwVelocity = Offset(
                    (gameState.ball.position.x - gameState.dragStart.x) * power,
                    (gameState.ball.position.y - gameState.dragStart.y) * power
                )
                
                // Flick arrow indicator
                val arrowLength = 80f
                val arrowAngle = kotlin.math.atan2(
                    throwVelocity.y.toDouble(),
                    throwVelocity.x.toDouble()
                ).toFloat()
                
                val arrowEndX = gameState.ball.position.x + cos(arrowAngle.toDouble()).toFloat() * arrowLength
                val arrowEndY = gameState.ball.position.y + sin(arrowAngle.toDouble()).toFloat() * arrowLength
                
                // Arrow shaft with dashed lines
                repeat(5) { i ->
                    val startRatio = i * 0.2f
                    val endRatio = startRatio + 0.1f
                    drawLine(
                        color = Color(0xFF6B5D3F),
                        start = Offset(
                            gameState.ball.position.x + (arrowEndX - gameState.ball.position.x) * startRatio,
                            gameState.ball.position.y + (arrowEndY - gameState.ball.position.y) * startRatio
                        ),
                        end = Offset(
                            gameState.ball.position.x + (arrowEndX - gameState.ball.position.x) * endRatio,
                            gameState.ball.position.y + (arrowEndY - gameState.ball.position.y) * endRatio
                        ),
                        strokeWidth = 8f
                    )
                }
                
                // Arrow head
                val arrowHeadSize = 20f
                val perpAngle = arrowAngle + Math.PI.toFloat() / 2f
                
                val arrowHeadPath = Path().apply {
                    moveTo(arrowEndX, arrowEndY)
                    lineTo(
                        arrowEndX - cos(arrowAngle.toDouble()).toFloat() * arrowHeadSize + cos(perpAngle.toDouble()).toFloat() * arrowHeadSize / 2f,
                        arrowEndY - sin(arrowAngle.toDouble()).toFloat() * arrowHeadSize + sin(perpAngle.toDouble()).toFloat() * arrowHeadSize / 2f
                    )
                    lineTo(
                        arrowEndX - cos(arrowAngle.toDouble()).toFloat() * arrowHeadSize - cos(perpAngle.toDouble()).toFloat() * arrowHeadSize / 2f,
                        arrowEndY - sin(arrowAngle.toDouble()).toFloat() * arrowHeadSize - sin(perpAngle.toDouble()).toFloat() * arrowHeadSize / 2f
                    )
                    close()
                }
                
                drawPath(
                    path = arrowHeadPath,
                    color = Color(0xFF6B5D3F)
                )
            }
        }

        // Particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            gameState.particles.forEach { particle ->
                drawCircle(
                    color = particle.color.copy(alpha = particle.alpha),
                    radius = particle.size,
                    center = particle.position
                )
            }
        }

        // High-Fidelity 3D White Ball
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ball = gameState.ball
            val radiusPx = ballRadiusPx * ball.scale
            val isInsideCup = ball.position.y > binY && ball.position.x in binX..(binX + 200f)

            rotate(ball.rotation, pivot = ball.position) {
                scale(ball.scale, pivot = ball.position) {
                    // 1. Soft Drop Shadow
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.15f),
                        radius = radiusPx * 1.05f,
                        center = Offset(ball.position.x + 4f, ball.position.y + 4f)
                    )
                    
                    // 2. 3D Volume Gradient
                    drawCircle(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(Color(0xFFFFFFFF), Color(0xFFF0F0F0), Color(0xFFD0D0D0), Color(0xFFC0C0C0)),
                            center = Offset(ball.position.x - radiusPx * 0.4f, ball.position.y - radiusPx * 0.4f),
                            radius = radiusPx * 1.5f
                        ),
                        radius = radiusPx,
                        center = ball.position
                    )
                    
                    // 3. Fresnel / Edge Rim
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = radiusPx,
                        center = ball.position,
                        style = Stroke(width = 1f)
                    )

                    // 4. Specular Studio Glint
                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = radiusPx * 0.2f,
                        center = Offset(ball.position.x - radiusPx * 0.45f, ball.position.y - radiusPx * 0.45f)
                    )

                    // 5. Internal Cup Illumination (Light Color when inside)
                    if (isInsideCup) {
                        drawCircle(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(Color(0xFF87CEFA).copy(alpha = 0.4f), Color.Transparent),
                                radius = radiusPx * 1.8f
                            ),
                            radius = radiusPx * 1.5f,
                            center = ball.position
                        )
                    }
                }
            }
        }

        // Modernized Score Dashboard
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                .align(Alignment.BottomCenter)
        ) {
            Surface(
                color = Color.Transparent,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, Color(0xFFD4C03D).copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF).copy(alpha = 0.95f),
                                Color(0xFFFFFDE7).copy(alpha = 0.9f)
                            )
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SCORE", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("${gameState.score}", style = MaterialTheme.typography.titleLarge, color = Color(0xFF6B5D3F), fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.2f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BEST", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("${gameState.highScore}", style = MaterialTheme.typography.titleLarge, color = Color(0xFF6B5D3F), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Instructions overlay - simplified
        if (gameState.attempts == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    "Flick the paper ball into the cup!",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF6B5D3F),
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.8f),
                            MaterialTheme.shapes.medium
                        )
                        .padding(16.dp)
                )
            }
        }

        // Message overlay - simplified
        gameState.showMessage?.let { msg ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    msg,
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (msg.contains("✓") || msg.contains("🔥")) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.9f),
                            MaterialTheme.shapes.large
                        )
                        .padding(32.dp)
                )
            }
        }

        // Touch handler
        var dragStart by remember { mutableStateOf(Offset.Zero) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (!gameState.isAnimating &&
                                gameState.ball.position.minus(offset).getDistance() < 100f) {
                                dragStart = offset
                                gameState = gameState.copy(
                                    isDragging = true,
                                    dragStart = offset
                                )
                            }
                        },
                        onDrag = { _, dragAmount ->
                            if (!gameState.isAnimating && gameState.isDragging) {
                                val newX = (gameState.ball.position.x + dragAmount.x)
                                    .coerceIn(50f, screenWidthPx - 50f)
                                val newY = (gameState.ball.position.y + dragAmount.y)
                                    .coerceIn(100f, screenHeightPx * 0.9f)
                                gameState = gameState.copy(
                                    ball = gameState.ball.copy(position = Offset(newX, newY))
                                )
                            }
                        },
                        onDragEnd = {
                            if (!gameState.isAnimating && gameState.isDragging) {
                                val power = 8f
                                val throwVelocity = Offset(
                                    (gameState.ball.position.x - gameState.dragStart.x) * power,
                                    (gameState.ball.position.y - gameState.dragStart.y) * power
                                )

                                soundManager.playSound(SoundManager.SoundType.FLICK)
                                gameState = gameState.copy(
                                    ball = gameState.ball.copy(
                                        velocity = throwVelocity,
                                        inHand = false
                                    ),
                                    isAnimating = true,
                                    isDragging = false
                                )
                            }
                        }
                    )
                }
        )
    }
}

// Helper extensions
private fun Offset.minus(other: Offset) = Offset(x - other.x, y - other.y)
private fun Offset.getDistance() = sqrt(x * x + y * y)