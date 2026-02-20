package com.example.paperball.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun StatsScreen(
    highScore: Int,
    bestStreak: Int,
    totalShots: Int,
    totalScores: Int,
    accuracy: Float,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp,
            modifier = Modifier
                .padding(32.dp)
                .widthIn(max = 400.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "📊 STATISTICS",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF6B5D3F)
                )
                
                Divider()
                
                StatRow("High Score", highScore.toString())
                StatRow("Best Streak", bestStreak.toString())
                StatRow("Total Shots", totalShots.toString())
                StatRow("Total Scores", totalScores.toString())
                StatRow("Accuracy", String.format("%.1f%%", accuracy))
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF6B5D3F)
        )
    }
}
