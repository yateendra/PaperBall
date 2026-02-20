package com.example.paperball.models

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Ball(
    val position: Offset = Offset.Zero,
    val velocity: Offset = Offset.Zero,
    val radius: Dp = 24.dp,
    val inHand: Boolean = true,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val color: String = "white" // white, yellow, pink, blue
)

data class Bin(
    val position: Offset,
    val width: Dp = 80.dp,
    val height: Dp = 60.dp,
    val style: String = "white" // white, blue, red, basket
)

data class Particle(
    val position: Offset,
    val velocity: Offset,
    val color: androidx.compose.ui.graphics.Color,
    val alpha: Float = 1f,
    val size: Float = 8f,
    val isActive: Boolean = true
)


data class TrailPoint(
    val position: Offset,
    val alpha: Float = 1f
)

enum class GameMode {
    CLASSIC,
    TIME_ATTACK,
    PERFECT_STREAK,
    DISTANCE_CHALLENGE
}

data class GameState(
    val ball: Ball = Ball(),
    val bin: Bin = Bin(Offset(0f, 0f)),
    val score: Int = 0,
    val highScore: Int = 0,
    val attempts: Int = 0,
    val combo: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val isAnimating: Boolean = false,
    val showMessage: String? = null,
    val particles: List<Particle> = emptyList(),
    val isDragging: Boolean = false,
    val dragStart: Offset = Offset.Zero,
    val trailPoints: List<TrailPoint> = emptyList(),
    val gameMode: GameMode = GameMode.CLASSIC,
    val timeRemaining: Int = 60, // for time attack
    val isPaused: Boolean = false,
    val showStats: Boolean = false,
    val showSettings: Boolean = false,
    val theme: String = "office", // office, classroom, home
    val perfectShots: Int = 0,
    val hudGlow: Float = 0f
)