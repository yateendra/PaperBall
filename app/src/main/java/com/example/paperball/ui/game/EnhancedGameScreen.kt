package com.example.paperball.ui.game

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paperball.models.*
import com.example.paperball.ui.components.SplashScreen
import com.example.paperball.ui.components.*
import com.example.paperball.ui.components.*
import com.example.paperball.utils.PreferencesManager
import com.example.paperball.utils.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.*
import kotlin.random.Random
import androidx.compose.ui.res.painterResource
import com.example.paperball.R
import androidx.compose.ui.graphics.PathEffect
import androidx.activity.compose.BackHandler

// Helper operator for Offset
operator fun Offset.plus(other: Offset) = Offset(x + other.x, y + other.y)

@Composable
fun EnhancedPaperBallGame() {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    
    val prefsManager = remember { PreferencesManager(context) }
    val soundManager = remember { SoundManager() }
    val ballPainter = painterResource(id = R.drawable.ball)
    val cupPainter = painterResource(id = R.drawable.cup)
    var showSplash by remember { mutableStateOf(!prefsManager.hasSeenSplash) }
    
    val vibrator = remember {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    var isFlipped by remember { mutableStateOf(false) }
    
    var controlSystem by remember { mutableStateOf(prefsManager.controlSystem) }
    var ballSizeMult by remember { mutableStateOf(prefsManager.ballSizeMult) }
    var cupSizeMult by remember { mutableStateOf(prefsManager.cupSizeMult) }
    var isCupDraggable by remember { mutableStateOf(prefsManager.isCupDraggable) }

    val ballRadiusPx = with(density) { 32.dp.toPx() * ballSizeMult }
    val cupHeight = 530f * cupSizeMult
    val cupWidth = 480f * cupSizeMult
    
    // Unified Grounding System (Raised to clear UI)
    val floorY = screenHeightPx * 0.82f 

    val ballGrounding = with(density) { 24.dp.toPx() } 
    val cupGrounding = with(density) { 12.dp.toPx() }  
    
    val savedCupX = prefsManager.cupPositionX
    val savedCupY = prefsManager.cupPositionY
    
    val defaultCupX = screenWidthPx - cupWidth - 64f
    val defaultCupY = floorY - cupHeight + cupGrounding
    
    var spawnPoint by remember(screenWidthPx, floorY, ballRadiusPx, ballGrounding) {
        mutableStateOf(Offset(screenWidthPx * 0.35f, floorY - ballRadiusPx + ballGrounding))
    }
    
    var gameState by remember {
        mutableStateOf(GameState(
            ball = Ball(position = spawnPoint),

            bin = Bin(position = Offset(
                (if (savedCupX > 0) savedCupX else defaultCupX).coerceIn(0f, screenWidthPx - cupWidth),
                if (savedCupY > 0) savedCupY else defaultCupY
            )),
            highScore = prefsManager.highScore,
            bestStreak = prefsManager.bestStreak,
            // Pre-allocate 100 particles for pooling
            particles = List(100) { Particle(Offset.Zero, Offset.Zero, Color.Transparent, isActive = false) }
        ))
    }
    
    // Show splash screen first
    if (showSplash) {
        SplashScreen(
            onDismiss = {
                showSplash = false
                prefsManager.hasSeenSplash = true
            }
        )
        return
    }
    
    var isDraggingCup by remember { mutableStateOf(false) }
    
    // Pre-calculate dimple offsets for the golf ball (Performance)
    val dimpleOffsets = remember {
        val random = kotlin.random.Random(42)
        List(40) {
            val angle = it * 9f + random.nextFloat() * 5f
            val rad = Math.toRadians(angle.toDouble()).toFloat()
            val distFactor = 0.3f + random.nextFloat() * 0.6f
            Triple(cos(rad) * distFactor, sin(rad) * distFactor, random.nextFloat())
        }
    }

    // Haptic feedback helper
    fun vibrate(duration: Long = 50) {
        if (prefsManager.hapticEnabled) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(duration)
                }
            } catch (e: Exception) {
                // Ignore vibration errors
            }
        }
    }
    
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Game") },
            text = { Text("Are you sure you want to quit?") },
            confirmButton = {
                Button(
                    onClick = { 
                        showExitDialog = false 
                        (context as? android.app.Activity)?.finish() 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Quit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCCCCCC)
        )
    }

    // OPTIMIZED MASTER LOOP (HUD & Particles)
    // Only handles visual effects that outlive the physics cycle.
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis {
                if (gameState.particles.any { it.isActive } || gameState.hudGlow > 0f) {
                    gameState = gameState.copy(
                        particles = if (!gameState.particles.any { it.isActive }) gameState.particles 
                                     else gameState.particles.map { p ->
                            if (!p.isActive) p else {
                                val newAlpha = p.alpha - 0.015f
                                if (newAlpha <= 0f) p.copy(isActive = false, alpha = 0f)
                                else p.copy(
                                    position = p.position + p.velocity,
                                    velocity = Offset(p.velocity.x * 0.99f, p.velocity.y + 0.4f),
                                    alpha = newAlpha
                                )
                            }
                        },
                        hudGlow = (gameState.hudGlow - 0.04f).coerceAtLeast(0f)
                    )
                }
            }
        }
    }

    // Physics simulation with Frame-Syncing
    LaunchedEffect(gameState.isAnimating) {
        if (gameState.isAnimating && !gameState.ball.inHand) {
            var vx = gameState.ball.velocity.x
            var vy = gameState.ball.velocity.y
            var x = gameState.ball.position.x
            var y = gameState.ball.position.y
            var rotation = gameState.ball.rotation
            val gravity = 4000f
            val drag = 0.99f
            val bounceDamping = 0.6f
            
            var hasScored = false
            var hasMissed = false
            var stopSimulation = false
            var lastBounceTime = 0L
            var motionlessFrames = 0

            while (y < screenHeightPx + 100 && !stopSimulation && !hasMissed && gameState.isAnimating) {
                withFrameMillis {
                    // Stuck detection: if ball stops moving for ~1.5 seconds, reset
                    if (abs(vx) < 10f && abs(vy) < 10f) {
                        motionlessFrames++
                    } else {
                        motionlessFrames = 0
                    }
                    if (motionlessFrames > 90) {
                        hasMissed = true
                    }

                    val currentBinX = gameState.bin.position.x
                    val currentBinY = gameState.bin.position.y
                    val currentTime = System.currentTimeMillis()

                    vy += gravity * 0.016f
                    vx *= drag
                    x += vx * 0.016f
                    y += vy * 0.016f
                    rotation += (vx * 0.5f + vy * 0.3f) * 0.016f

                    // Helper for throttling sounds
                    fun tryPlayBounceSound(impactVelocity: Float, source: String = "unknown") {
                        // Ignore weak impacts to prevent dragging/rolling sounds
                        if (impactVelocity < 60f) return

                        val timeDiff = currentTime - lastBounceTime
                        if (timeDiff > 400) { 
                             vibrate(if (impactVelocity > 150f) 40 else 30)
                             soundManager.playSound(SoundManager.SoundType.BOUNCE)
                             lastBounceTime = currentTime
                        }
                    }

                    // 2. Floor (Physics)
                    val physicsFloorY = floorY + ballGrounding
                    if (y > physicsFloorY - ballRadiusPx && vy > 0) {
                        if (abs(vy) > 100f) {
                            vy = -vy * bounceDamping
                            y = physicsFloorY - ballRadiusPx
                            vx *= 0.9f
                            tryPlayBounceSound(abs(vy), "Floor")
                        } else {
                            hasMissed = true
                        }
                    }

                    // 3. Cup Collision
                    val currentRimHeight = 40f * cupSizeMult
                    val currentRimLeft = currentBinX + 40f * cupSizeMult
                    val currentRimRight = currentBinX + cupWidth - 40f * cupSizeMult
                    val currentRimTop = currentBinY + currentRimHeight

                    // Rim bounce
                    val isAboveOpening = y < currentRimTop && y + vy * 0.016f > currentRimTop - 20f
                    val hitRim = (abs(x - currentRimLeft) < 25f || abs(x - currentRimRight) < 25f) && isAboveOpening
                    if (hitRim && vy > 0) {
                        val isLeftRim = x < currentBinX + cupWidth/2
                        
                        // Intelligent bounce: if near a wall, don't kick into it
                        val targetVx = if (isLeftRim) -abs(vx + 200f) else abs(vx + 200f)
                        vx = if (x < ballRadiusPx + 20f && targetVx < 0) abs(targetVx) 
                             else if (x > screenWidthPx - ballRadiusPx - 20f && targetVx > 0) -abs(targetVx)
                             else targetVx
                        
                        vx *= 0.8f
                        vx += (kotlin.random.Random.nextFloat() * 100f - 50f)
                        
                        vy = -vy * 0.5f
                        y = currentRimTop - 10f // Higher reset for stability
                        
                        // Nudge respecting boundaries
                        val nudge = if (isLeftRim) -15f else 15f
                        x = (x + nudge).coerceIn(ballRadiusPx, screenWidthPx - ballRadiusPx)

                        if (abs(vy) > 200f) {
                            tryPlayBounceSound(abs(vy), "Rim")
                        }
                    }

                    // Side walls
                    val cupBottom = currentBinY + cupHeight
                    if (y > currentRimTop && y < cupBottom) {
                        if (x + ballRadiusPx > currentBinX && x < currentBinX + 30f && vx > 0) {
                            vx = -abs(vx) * bounceDamping
                            x = (currentBinX - ballRadiusPx).coerceIn(ballRadiusPx * 0.5f, screenWidthPx - ballRadiusPx * 0.5f)
                            vibrate(30)
                        } else if (x - ballRadiusPx < currentBinX + cupWidth && x > currentBinX + cupWidth - 30f && vx < 0) {
                            vx = abs(vx) * bounceDamping
                            x = (currentBinX + cupWidth + ballRadiusPx).coerceIn(ballRadiusPx * 0.5f, screenWidthPx - ballRadiusPx * 0.5f)
                            vibrate(30)
                        }
                    }


                    // 1. Boundaries (Applied LAST for stability)
                    if (y < ballRadiusPx && vy < 0) {
                        vy = -vy * bounceDamping
                        y = ballRadiusPx
                        tryPlayBounceSound(abs(vy), "Ceiling")
                    }
                    if (x < ballRadiusPx) {
                        vx = -vx * bounceDamping
                        x = ballRadiusPx
                        tryPlayBounceSound(abs(vx), "LeftWall")
                    } else if (x > screenWidthPx - ballRadiusPx) {
                        vx = -vx * bounceDamping
                        x = screenWidthPx - ballRadiusPx
                        tryPlayBounceSound(abs(vx), "RightWall")
                    }

                    // 4. Scoring check
                    val inAperture = x in (currentRimLeft + 15f)..(currentRimRight - 15f)
                    val crossedRimPlane = y > currentRimTop && (y - vy * 0.016f) <= currentRimTop

                    if (inAperture && crossedRimPlane && vy > 0) {
                        hasScored = true
                        val isPerfect = abs(x - (currentBinX + cupWidth/2)) < 30f
                        
                        var activated = 0
                        val updatedParticles = gameState.particles.map { p ->
                            if (!p.isActive && activated < (if (isPerfect) 40 else 25)) {
                                activated++
                                p.copy(
                                    position = Offset(currentBinX + cupWidth/2, currentRimTop),

                                    velocity = Offset(Random.nextFloat() * 14f - 7f, Random.nextFloat() * -20f - 8f),
                                    color = if (Random.nextFloat() > 0.3f) Color(0xFFFFD700) else Color(0xFFFFFACD),
                                    isActive = true,
                                    alpha = 1f
                                )
                            } else p
                        }

                        gameState = gameState.copy(
                            score = gameState.score + 1,
                            currentStreak = gameState.currentStreak + 1,
                            particles = updatedParticles,
                            hudGlow = 1f
                        )
                        if (isPerfect) {
                            soundManager.playSound(SoundManager.SoundType.PERFECT)
                            vibrate(100)
                            Toast.makeText(context, "🎯 PERFECT!", Toast.LENGTH_SHORT).show()
                        } else {
                            soundManager.playSound(SoundManager.SoundType.SWISH)
                            vibrate(70)
                            Toast.makeText(context, "Scored!", Toast.LENGTH_SHORT).show()
                        }
                        stopSimulation = true
                    }

                    // Apply Frame State
                    if (!stopSimulation) {
                        gameState = gameState.copy(
                            ball = gameState.ball.copy(position = Offset(x, y), rotation = rotation),
                            trailPoints = (gameState.trailPoints + TrailPoint(Offset(x, y), 0.4f)).takeLast(10)

                        )
                    }
                }
            }
            
            if (hasScored) {
                // Scoring celebration sink
                val targetX = gameState.bin.position.x + 275f
                val targetY = gameState.bin.position.y + 180f
                repeat(30) {
                    withFrameMillis {
                        val ball = gameState.ball
                        gameState = gameState.copy(
                            ball = ball.copy(
                                position = Offset(
                                    ball.position.x + (targetX - ball.position.x) * 0.15f,
                                    ball.position.y + (targetY - ball.position.y) * 0.15f
                                ),
                                rotation = ball.rotation + 0.8f
                            )
                        )
                    }
                }
            }

            if (hasMissed) {
                gameState = gameState.copy(currentStreak = 0, trailPoints = emptyList())
                vibrate(200)
            }

            delay(800)
            // Randomize & Flip positions on SCORE
            if (hasScored) {
                isFlipped = !isFlipped
                
                // Ranges: Side A (12-40%), Side B (60-88%)
                // We keep them in separate halves but ensure they aren't "too close"
                val rangeA = 0.12f..0.40f
                val rangeB = 0.60f..0.88f
                
                val ballRange = if (!isFlipped) rangeA else rangeB
                val cupRange = if (!isFlipped) rangeB else rangeA
                
                var newBallX: Float
                var newCupX: Float
                
                // Safety loop: Ensure a significant gap (at least 40% of screen width)
                var attempts = 0
                do {
                    newBallX = screenWidthPx * (ballRange.start + Random.nextFloat() * (ballRange.endInclusive - ballRange.start))
                    newCupX = screenWidthPx * (cupRange.start + Random.nextFloat() * (cupRange.endInclusive - cupRange.start))
                    attempts++
                } while (abs(newBallX - (newCupX + cupWidth/2)) < screenWidthPx * 0.40f && attempts < 10)
                
                spawnPoint = Offset(newBallX, floorY - ballRadiusPx + ballGrounding)
                
                gameState = gameState.copy(
                    bin = gameState.bin.copy(position = Offset(
                        newCupX.coerceIn(0f, screenWidthPx - cupWidth),
                        floorY - cupHeight + cupGrounding
                    ))
                )
                
                // Save new cup position
                prefsManager.cupPositionX = gameState.bin.position.x
                prefsManager.cupPositionY = gameState.bin.position.y
            }

            prefsManager.totalShots += 1
            gameState = gameState.copy(
                ball = Ball(position = spawnPoint),

                isAnimating = false,
                attempts = gameState.attempts + 1,
                trailPoints = emptyList()
            )
        } else if (!gameState.isAnimating && gameState.trailPoints.isNotEmpty()) {
            gameState = gameState.copy(trailPoints = emptyList())
        }
    }

    // Polished Studio Environment (Optimized with Cache)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                onDrawBehind {
                    // Use the unified floorY for background
                    val horizonY = floorY
                    
                    // 1. Abstract Luxury Black Gradient with Vignette
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF353535), // Lighter center (Spotlight)
                                Color(0xFF1A1A1A), // Mid-tone
                                Color(0xFF0D0D0D), // Dark Grey
                                Color(0xFF000000)  // Deep Black Vignette Border 
                            ),
                            center = Offset(size.width / 2f, size.height * 0.35f),
                            radius = size.height * 0.8f,
                            tileMode = androidx.compose.ui.graphics.TileMode.Clamp
                        )
                    )
                    
                    // 2. Reflective Studio Floor
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color(0xFF151515), Color(0xFF000000)),
                            startY = horizonY,
                            endY = size.height
                        ),
                        topLeft = Offset(0f, horizonY),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height - horizonY)
                    )
                    
                    // 3. Horizon Glow Line
                    drawLine(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.08f), Color.Transparent)
                        ),
                        start = Offset(0f, horizonY),
                        end = Offset(size.width, horizonY),
                        strokeWidth = 2f
                    )
                }
            }
    ) {

        // Unified Environment & Objects Layer
        val horizonY = floorY
        val binX = gameState.bin.position.x
        val binY = gameState.bin.position.y
        val currentCupWidth = 480f * cupSizeMult
        val currentCupHeight = 530f * cupSizeMult
        
        // 1. CUP RENDERING LAYER
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cupBottom = binY + currentCupHeight
            
            // Cup Reflection (Flipped and faded)
            val reflectionAlpha = 0.15f
            withTransform({
                scale(scaleX = 1f, scaleY = -1f, pivot = Offset(binX, horizonY))
            }) {
                translate(binX, binY + (horizonY - binY) * 2f - currentCupHeight) {
                    with(cupPainter) {
                        draw(size = Size(currentCupWidth, currentCupHeight), alpha = reflectionAlpha)
                    }
                }
            }
            
            // Cup Shadow
            drawOval(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(binX + currentCupWidth / 2f, cupBottom - cupGrounding),
                    radius = currentCupWidth * 0.45f
                ),
                topLeft = Offset(binX + 40f * cupSizeMult, cupBottom - cupGrounding - 10f),
                size = Size(currentCupWidth - 80f * cupSizeMult, 20f)
            )
            
            // Main Cup Image
            translate(binX, binY) {
                with(cupPainter) {
                    draw(size = Size(currentCupWidth, currentCupHeight))
                }
            }

            // Internal Depth Overlay
            drawOval(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent),
                    radius = currentCupWidth / 2.2f
                ),
                topLeft = Offset(binX + 50f * cupSizeMult, binY + 45f * cupSizeMult),
                size = Size(currentCupWidth - 100f * cupSizeMult, 25f * cupSizeMult)
            )
        }

        // 2. TRAJECTORY DOTS LAYER
        if (gameState.isDragging && !gameState.isAnimating) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pullVector = spawnPoint - gameState.ball.position
                
                if (controlSystem == "pull") {
                    // Slingshot Pull Logic - Dots start at ball
                    val power = 22f
                    val predVx = pullVector.x * power
                    val predVy = pullVector.y * power
                    val grav = 4000f

                    // Rubber band line
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = spawnPoint,
                        end = gameState.ball.position,
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )

                    // Trajectory dots (Parabolic arc)
                    repeat(12) { i ->
                        val t = (i + 1) * 0.08f
                        val dx = predVx * t
                        val dy = predVy * t + 0.5f * grav * t * t
                        val dotPos = gameState.ball.position + Offset(dx, dy)
                        
                        if (dotPos.x in 0f..size.width && dotPos.y in 0f..size.height) {
                            drawCircle(
                                color = Color(0xFFFFD700).copy(alpha = 0.8f - (i * 0.06f)),
                                radius = 6f - (i * 0.3f),
                                center = dotPos
                            )
                        }
                    }
                } else {
                    // Normal system (Push/Flick) - Dots start at spawn
                    val power = 22f
                    val dragVector = gameState.ball.position - spawnPoint
                    val predVx = dragVector.x * power
                    val predVy = dragVector.y * power
                    val grav = 4000f

                    // Trajectory dots (Forward)
                    repeat(12) { i ->
                        val t = (i + 1) * 0.08f
                        val dx = predVx * t
                        val dy = predVy * t + 0.5f * grav * t * t
                        val dotPos = spawnPoint + Offset(dx, dy)
                        
                        if (dotPos.x in 0f..size.width && dotPos.y in 0f..size.height) {
                            drawCircle(
                                color = Color(0xFFFFD700).copy(alpha = 0.8f - (i * 0.06f)),
                                radius = 6f - (i * 0.3f),
                                center = dotPos
                            )
                        }
                    }
                }
            }
        }

        // 3. TOUCH HANDLER (GESTURE LAYER) - Must be below HUD for button clicks to work
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (gameState.isAnimating) {
                                gameState = gameState.copy(
                                    isAnimating = false,
                                    ball = gameState.ball.copy(position = spawnPoint, velocity = Offset.Zero),
                                    trailPoints = emptyList()
                                )
                                return@detectDragGestures
                            }

                            val currentCupWidth = 480f * cupSizeMult
                            val currentCupHeight = 530f * cupSizeMult
                            val binX = gameState.bin.position.x
                            val binY = gameState.bin.position.y
                            
                            val touchingCup = offset.x in (binX)..(binX + currentCupWidth) &&
                                    offset.y in (binY)..(binY + currentCupHeight)
                            
                            val touchingBall = gameState.ball.position.minus(offset).getDistance() < 250f
                            
                            if (!gameState.isAnimating && !gameState.isPaused) {
                                if (touchingCup && isCupDraggable) {
                                    isDraggingCup = true
                                    vibrate(20)
                                } else if (touchingBall) {
                                    gameState = gameState.copy(
                                        isDragging = true,
                                        dragStart = offset
                                    )
                                    vibrate(20)
                                }
                            }
                        },
                        onDrag = { _, dragAmount ->
                            if (isDraggingCup) {
                                 val currentCupWidth = 480f * cupSizeMult
                                 val currentCupHeight = 530f * cupSizeMult
                                val newX = (gameState.bin.position.x + dragAmount.x)
                                    .coerceIn(0f, screenWidthPx - currentCupWidth)
                                val newY = (gameState.bin.position.y + dragAmount.y)
                                    .coerceIn(50f, screenHeightPx * 0.85f - currentCupHeight)
                                gameState = gameState.copy(
                                    bin = gameState.bin.copy(position = Offset(newX, newY))
                                )
                            } else if (!gameState.isAnimating && gameState.isDragging) {
                                if (controlSystem == "pull") {
                                    val rawPos = gameState.ball.position + dragAmount
                                    val pull = rawPos - spawnPoint
                                    val maxPull = 400f
                                    
                                    val cappedPos = if (pull.getDistance() > maxPull) {
                                        spawnPoint + (pull / pull.getDistance() * maxPull)
                                    } else {
                                        rawPos
                                    }
                                    
                                    gameState = gameState.copy(
                                        ball = gameState.ball.copy(
                                            position = Offset(
                                                cappedPos.x.coerceIn(0f, screenWidthPx),
                                                cappedPos.y.coerceIn(0f, screenHeightPx)
                                            ),
                                            rotation = gameState.ball.rotation + dragAmount.x * 0.6f
                                        )
                                    )
                                } else {
                                    gameState = gameState.copy(
                                        ball = gameState.ball.copy(
                                            position = (gameState.ball.position + dragAmount),
                                            rotation = gameState.ball.rotation + dragAmount.x * 0.6f
                                        )
                                    )
                                }
                            }
                        },
                        onDragEnd = {
                            if (isDraggingCup) {
                                prefsManager.cupPositionX = gameState.bin.position.x
                                prefsManager.cupPositionY = gameState.bin.position.y
                                isDraggingCup = false
                                vibrate(30)
                            } else if (!gameState.isAnimating && gameState.isDragging) {
                                val power = 22f
                                val isSlingshot = controlSystem == "pull"
                                
                                val throwVelocity = if (isSlingshot) {
                                    val pullVector = spawnPoint - gameState.ball.position
                                    Offset(pullVector.x * power, pullVector.y * power)
                                } else {
                                    val dragVector = gameState.ball.position - spawnPoint
                                    Offset(dragVector.x * power, dragVector.y * power)
                                }

                                gameState = gameState.copy(
                                    ball = gameState.ball.copy(
                                        position = if (isSlingshot) gameState.ball.position else spawnPoint,
                                        velocity = throwVelocity,
                                        inHand = false
                                    ),
                                    isAnimating = true,
                                    isDragging = false
                                )
                                vibrate(60)
                                soundManager.playSound(SoundManager.SoundType.FLICK)
                            }
                        }
                    )
                }
        )

        // 4. Premium Floating Scoreboard (Pill-shaped with Glassmorphism)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
                .align(Alignment.BottomCenter)
        ) {
            val goldBrush = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFD700), // Gold
                    Color(0xFFFFFACD), // Shine
                    Color(0xFFDAA520), // Deep Gold
                    Color(0xFFFFD700)
                )
            )

            Surface(
                color = Color.Transparent,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(40.dp),
                border = BorderStroke(1.5.dp, goldBrush),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        shadowElevation = 12f
                        clip = true
                    }
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color(0xAA1A1A1A), // Semi-transparent Glassmorphism
                                Color(0xDD000000)
                            )
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(40.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Score Card
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "CURRENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFD700).copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            "${gameState.score}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFFACD) // Light Golden Yellow
                        )
                    }

                    // Gold Vertical Divider
                    Box(modifier = Modifier.width(1.dp).height(32.dp).alpha(0.4f).background(goldBrush))

                    // Best Card
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "BEST",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFD700).copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            "${gameState.highScore}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }

                    // Gold Vertical Divider
                    Box(modifier = Modifier.width(1.dp).height(32.dp).alpha(0.4f).background(goldBrush))

                    // Streak Card
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "STREAK",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFD700).copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${gameState.currentStreak}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = if (gameState.currentStreak > 1) Color(0xFFFFD700) else Color(0xFFFFFACD)
                            )
                            if (gameState.currentStreak > 1) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Streak",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(16.dp).padding(start = 4.dp)
                                )
                            }
                        }
                    }

                    // Divider
                    Box(modifier = Modifier.width(1.dp).height(32.dp).alpha(0.4f).background(goldBrush))

                    // Settings Button
                    IconButton(
                        onClick = { 
                            gameState = gameState.copy(showSettings = true)
                            vibrate(20)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFFFFD700).copy(alpha = 0.9f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // 5. BALL & GAME LAYER (Trails, Particles, Ball) - Rendered over HUD
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radiusPx = ballRadiusPx
            
            // A. Draw Trail
            gameState.trailPoints.forEachIndexed { index, point ->
                val alphaBase = (index.toFloat() / gameState.trailPoints.size.coerceAtLeast(1))
                drawCircle(
                    color = Color.White.copy(alpha = alphaBase * 0.2f),
                    radius = radiusPx * alphaBase,
                    center = point.position
                )
            }

            // B. Draw Active Particles
            gameState.particles.forEach { particle ->
                if (particle.isActive) {
                    drawCircle(
                        color = particle.color.copy(alpha = particle.alpha),
                        radius = particle.size,
                        center = particle.position
                    )
                }
            }
            
            // C. Ball Drawing
            val ball = gameState.ball
            val drawPos = if (gameState.isDragging && controlSystem == "normal") spawnPoint else ball.position
            
            // Floor Reflection
            val distToFloor = (floorY - drawPos.y).coerceAtLeast(-ballGrounding)
            val reflectionAlpha = (0.15f * (1f - distToFloor / (size.height * 0.4f))).coerceIn(0f, 0.15f)
            
            if (reflectionAlpha > 0.01f) {
                val reflectionY = floorY + distToFloor + ballGrounding * 2f
                rotate(ball.rotation, pivot = Offset(drawPos.x, reflectionY)) {
                    translate(drawPos.x - radiusPx, reflectionY - radiusPx) {
                        with(ballPainter) {
                            draw(size = Size(radiusPx * 2f, radiusPx * 2f), alpha = reflectionAlpha)
                        }
                    }
                }
            }

            // Shadow
            val shadowY = floorY + ballGrounding
            val shadowScale = (1f + distToFloor / 250f).coerceIn(1f, 3f)
            val shadowAlpha = (0.6f / shadowScale).coerceIn(0.1f, 0.6f)
            
            drawOval(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = shadowAlpha), Color.Transparent),
                    center = Offset(drawPos.x, shadowY),
                    radius = radiusPx * 1.4f * shadowScale
                ),
                topLeft = Offset(drawPos.x - radiusPx * 1.4f * shadowScale, shadowY - 10f),
                size = Size(radiusPx * 2.8f * shadowScale, 20f)
            )

            // Main Ball
            rotate(ball.rotation, pivot = drawPos) {
                translate(drawPos.x - radiusPx, drawPos.y - radiusPx) {
                    with(ballPainter) {
                        draw(size = Size(radiusPx * 2f, radiusPx * 2f))
                    }
                }
                
                // Fresnel / Lighting
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    radius = radiusPx,
                    center = drawPos, // Fixed: use drawPos for lighting
                    style = Stroke(width = 1f)
                )

                val isInsideCup = ball.position.y > binY && ball.position.x in binX..(binX + currentCupWidth)
                if (isInsideCup) {
                    drawCircle(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(Color(0xFFFFD700).copy(alpha = 0.2f), Color.Transparent),
                            center = drawPos, // Fixed: use drawPos
                            radius = radiusPx * 1.5f
                        ),
                        radius = radiusPx * 1.5f,
                        center = drawPos
                    )
                }
            }
        }

        // 6. OVERLAYS
        // Message overlay
        gameState.showMessage?.let { msg ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    msg,
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (msg.contains("✓") || msg.contains("🔥") || msg.contains("🎯")) 
                        Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.9f), MaterialTheme.shapes.large)
                        .padding(32.dp)
                )
            }
        }

        // Executive Border & Vignette
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Vignette
            drawRect(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                    center = center,
                    radius = size.maxDimension * 0.8f
                )
            )
            
            // Subtle Gold Border
            drawRect(
                color = Color(0xFFFFD700).copy(alpha = 0.12f),
                style = Stroke(width = 4f)
            )
        }

        // Pause menu
        if (gameState.isPaused) {
            PauseMenu(
                onResume = { gameState = gameState.copy(isPaused = false) },
                onRestart = {
                    gameState = GameState(
                        ball = Ball(position = Offset(screenWidthPx * 0.2f, screenHeightPx * 0.8f)),
                        bin = Bin(position = Offset(
                            if (prefsManager.cupPositionX > 0) prefsManager.cupPositionX else defaultCupX,
                            if (prefsManager.cupPositionY > 0) prefsManager.cupPositionY else defaultCupY
                        )),
                        highScore = prefsManager.highScore,
                        bestStreak = prefsManager.bestStreak
                    )
                },
                onSettings = {
                    gameState = gameState.copy(
                        isPaused = false,
                        showSettings = true
                    )
                },
                onStats = {
                    gameState = gameState.copy(
                        isPaused = false,
                        showStats = true
                    )
                }
            )
        }

        // Quick Settings Button removed (now inside score card)

        // Stats screen
        if (gameState.showStats) {
            StatsScreen(
                highScore = prefsManager.highScore,
                bestStreak = prefsManager.bestStreak,
                totalShots = prefsManager.totalShots,
                totalScores = prefsManager.totalScores,
                accuracy = prefsManager.accuracy,
                onClose = { gameState = gameState.copy(showStats = false) }
            )
        }

        // Settings screen
        if (gameState.showSettings) {
            SettingsScreen(
                soundEnabled = prefsManager.soundEnabled,
                hapticEnabled = prefsManager.hapticEnabled,
                isCupDraggable = isCupDraggable,
                controlSystem = controlSystem,
                ballSizeMult = ballSizeMult,
                cupSizeMult = cupSizeMult,
                onSoundToggle = { prefsManager.soundEnabled = it },
                onHapticToggle = { prefsManager.hapticEnabled = it },
                onCupDraggableToggle = {
                    isCupDraggable = it
                    prefsManager.isCupDraggable = it
                },
                onControlSystemChange = { 
                    controlSystem = it
                    prefsManager.controlSystem = it
                },
                onBallSizeChange = {
                    ballSizeMult = it
                    prefsManager.ballSizeMult = it
                },
                onCupSizeChange = {
                    cupSizeMult = it
                    prefsManager.cupSizeMult = it
                },
                onClose = { gameState = gameState.copy(showSettings = false) }
            )
        }
    }
}

// Helper extensions
private fun Offset.minus(other: Offset) = Offset(x - other.x, y - other.y)
private fun Offset.getDistance() = sqrt(x * x + y * y)
private operator fun Offset.div(scalar: Float) = Offset(x / scalar, y / scalar)
private fun Offset.unit(): Offset {
    val dist = getDistance()
    return if (dist > 0.001f) this / dist else Offset.Zero
}
