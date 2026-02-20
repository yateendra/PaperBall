package com.example.paperball.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("paperball_prefs", Context.MODE_PRIVATE)
    
    var highScore: Int
        get() = prefs.getInt("high_score", 0)
        set(value) = prefs.edit().putInt("high_score", value).apply()
    
    var bestStreak: Int
        get() = prefs.getInt("best_streak", 0)
        set(value) = prefs.edit().putInt("best_streak", value).apply()
    
    var totalShots: Int
        get() = prefs.getInt("total_shots", 0)
        set(value) = prefs.edit().putInt("total_shots", value).apply()
    
    var totalScores: Int
        get() = prefs.getInt("total_scores", 0)
        set(value) = prefs.edit().putInt("total_scores", value).apply()
    
    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()
    
    var hapticEnabled: Boolean
        get() = prefs.getBoolean("haptic_enabled", true)
        set(value) = prefs.edit().putBoolean("haptic_enabled", value).apply()
    
    var selectedTheme: String
        get() = prefs.getString("selected_theme", "office") ?: "office"
        set(value) = prefs.edit().putString("selected_theme", value).apply()
    
    var selectedBallColor: String
        get() = prefs.getString("selected_ball_color", "white") ?: "white"
        set(value) = prefs.edit().putString("selected_ball_color", value).apply()
    
    var selectedCupStyle: String
        get() = prefs.getString("selected_cup_style", "white") ?: "white"
        set(value) = prefs.edit().putString("selected_cup_style", value).apply()
    
    var cupPositionX: Float
        get() = prefs.getFloat("cup_position_x", -1f)
        set(value) = prefs.edit().putFloat("cup_position_x", value).apply()
    
    var cupPositionY: Float
        get() = prefs.getFloat("cup_position_y", -1f)
        set(value) = prefs.edit().putFloat("cup_position_y", value).apply()
    
    var hasSeenSplash: Boolean
        get() = prefs.getBoolean("has_seen_splash", false)
        set(value) = prefs.edit().putBoolean("has_seen_splash", value).apply()

    var controlSystem: String
        get() = prefs.getString("control_system", "pull") ?: "pull"
        set(value) = prefs.edit().putString("control_system", value).apply()

    var ballSizeMult: Float
        get() = prefs.getFloat("ball_size_mult", 1.0f)
        set(value) = prefs.edit().putFloat("ball_size_mult", value).apply()

    var cupSizeMult: Float
        get() = prefs.getFloat("cup_size_mult", 1.0f)
        set(value) = prefs.edit().putFloat("cup_size_mult", value).apply()

    var isCupDraggable: Boolean
        get() = prefs.getBoolean("is_cup_draggable", false)
        set(value) = prefs.edit().putBoolean("is_cup_draggable", value).apply()
    
    val accuracy: Float
        get() = if (totalShots > 0) (totalScores.toFloat() / totalShots.toFloat()) * 100f else 0f
}
