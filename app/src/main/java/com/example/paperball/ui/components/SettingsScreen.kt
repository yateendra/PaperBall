package com.example.paperball.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    soundEnabled: Boolean,
    hapticEnabled: Boolean,
    isCupDraggable: Boolean,
    controlSystem: String,
    ballSizeMult: Float,
    cupSizeMult: Float,
    onSoundToggle: (Boolean) -> Unit,
    onHapticToggle: (Boolean) -> Unit,
    onCupDraggableToggle: (Boolean) -> Unit,
    onControlSystemChange: (String) -> Unit,
    onBallSizeChange: (Float) -> Unit,
    onCupSizeChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            color = Color(0xFF1E1E1E),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.3f)),
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 400.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    "⚙️ SETTINGS",
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color(0xFFFFD700),
                        fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Sound & Haptic
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sound Effects", color = Color.White)
                    Switch(checked = soundEnabled, onCheckedChange = onSoundToggle, 
                           colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700)))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Haptic Feedback", color = Color.White)
                    Switch(checked = hapticEnabled, onCheckedChange = onHapticToggle,
                           colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700)))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Allow Cup Repositioning", color = Color.White)
                    Switch(checked = isCupDraggable, onCheckedChange = onCupDraggableToggle,
                           colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700)))
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Control System
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Control System", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = controlSystem == "pull",
                            onClick = { onControlSystemChange("pull") },
                            label = { Text("Pull-to-Shoot") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFD700),
                                selectedLabelColor = Color.Black
                            )
                        )
                        FilterChip(
                            selected = controlSystem == "normal",
                            onClick = { onControlSystemChange("normal") },
                            label = { Text("Normal/Flick") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFD700),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Sizing
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Ball Size: ${"%.1f".format(ballSizeMult)}x", color = Color.White)
                    Slider(
                        value = ballSizeMult,
                        onValueChange = onBallSizeChange,
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                    )
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Cup Size: ${"%.1f".format(cupSizeMult)}x", color = Color.White)
                    Slider(
                        value = cupSizeMult,
                        onValueChange = onCupSizeChange,
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                    )
                }
                
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Done", color = Color.Black, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
        }
    }
}
