package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VibeMode
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonLime
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HeaderBar(
    isAccessibilityActive: Boolean,
    isBackgroundVoiceActive: Boolean,
    isSpeaking: Boolean,
    isListening: Boolean,
    vibeMode: VibeMode,
    onVibeSelected: (VibeMode) -> Unit,
    onAccessibilityClicked: () -> Unit,
    onBackgroundVoiceToggle: () -> Unit
) {
    var showVibeMenu by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CyberDarkSurface,
                        CyberDarkSurface.copy(alpha = 0.95f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Branding & Pulsing Status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(NeonCyan, NeonViolet)
                            )
                        )
                        .then(
                            if (isSpeaking || isListening) Modifier.scale(pulseScale) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "FX",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "FX ASSISTANT",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeonCyan.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "AI 3.8",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = if (isSpeaking) "🔊 Speaking to you..." else if (isListening) "🎙️ Listening..." else "Autonomous Screen & Task Controller",
                        color = if (isSpeaking || isListening) NeonCyan else TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // Vibe Mode Selector Pill
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(CyberDarkSurface)
                        .border(1.dp, NeonViolet.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .clickable { showVibeMenu = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("vibe_selector_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = vibeMode.emoji, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = vibeMode.displayName.substringBefore(" "),
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                DropdownMenu(
                    expanded = showVibeMenu,
                    onDismissRequest = { showVibeMenu = false },
                    modifier = Modifier.background(CyberDarkSurface)
                ) {
                    VibeMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = "${mode.emoji} ${mode.displayName}",
                                        color = if (mode == vibeMode) NeonCyan else TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = mode.tagline,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            onClick = {
                                onVibeSelected(mode)
                                showVibeMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // System Services Quick Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Accessibility Service Badge
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isAccessibilityActive) NeonLime.copy(alpha = 0.12f)
                        else Color(0xFFFFAB00).copy(alpha = 0.12f)
                    )
                    .border(
                        1.dp,
                        if (isAccessibilityActive) NeonLime.copy(alpha = 0.3f)
                        else Color(0xFFFFAB00).copy(alpha = 0.4f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onAccessibilityClicked() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("accessibility_status_badge"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isAccessibilityActive) NeonLime else Color(0xFFFFAB00))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAccessibilityActive) "Screen Control: ACTIVE" else "Enable Accessibility",
                    color = if (isAccessibilityActive) NeonLime else Color(0xFFFFAB00),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Background Voice Service Badge
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isBackgroundVoiceActive) NeonCyan.copy(alpha = 0.15f)
                        else CyberCardBorder.copy(alpha = 0.4f)
                    )
                    .border(
                        1.dp,
                        if (isBackgroundVoiceActive) NeonCyan.copy(alpha = 0.4f)
                        else CyberCardBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onBackgroundVoiceToggle() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("background_voice_toggle_badge"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isBackgroundVoiceActive) Icons.Default.VolumeUp else Icons.Default.Mic,
                    contentDescription = "Voice Mode",
                    tint = if (isBackgroundVoiceActive) NeonCyan else TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBackgroundVoiceActive) "Voice in BG: ON" else "Voice in BG: OFF",
                    color = if (isBackgroundVoiceActive) NeonCyan else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
