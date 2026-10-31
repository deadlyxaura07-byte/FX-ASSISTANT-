package com.example.model

enum class VibeMode(val displayName: String, val tagline: String, val emoji: String) {
    GEN_Z("Gen Z Vibe", "No cap, high-key productive & based", "⚡"),
    BRAINROT_ALPHA("Alpha Rizz", "Skibidi grindset & gigachad speed", "🔥"),
    BALANCED_COOL("Modern Cool", "Punchy, witty, and smooth", "✨"),
    PRO_EXECUTIVE("Pro Mode", "Zero fluff, laser-focused execution", "🎯")
}

data class ScreenElement(
    val id: String,
    val text: String,
    val contentDescription: String? = null,
    val className: String,
    val packageName: String,
    val bounds: ScreenRect,
    val isClickable: Boolean,
    val isEditable: Boolean
)

data class ScreenRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2
}

data class ActiveScreenState(
    val packageName: String = "com.android.launcher",
    val appName: String = "Home Screen",
    val activityTitle: String = "Launcher",
    val elements: List<ScreenElement> = emptyList(),
    val summary: String = "Idle on Home Screen",
    val timestamp: Long = System.currentTimeMillis()
)

enum class StepType {
    LAUNCH_APP,
    CLICK_ELEMENT,
    CLICK_COORDINATES,
    TYPE_TEXT,
    SWIPE_GESTURE,
    SPEAK_VOICE,
    PRESS_SYSTEM_KEY,
    WAIT_DELAY
}

data class TaskStep(
    val stepNumber: Int,
    val type: StepType,
    val description: String,
    val target: String = "", // package name, text query, or voice line
    val x: Float = 0f,
    val y: Float = 0f,
    val endX: Float = 0f,
    val endY: Float = 0f,
    val delayMs: Long = 800L
)

data class AutomationRecipe(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String, // "Social", "Focus", "Daily", "Media"
    val icon: String,
    val targetApp: String,
    val steps: List<TaskStep>,
    val vibeQuote: String,
    val isEnabled: Boolean = true
)

data class DailyAppUsage(
    val packageName: String,
    val appName: String,
    val category: String, // "Social & Doomscroll", "Productivity", "Entertainment", "Games", "Utility"
    val durationMinutes: Int,
    val launchCount: Int,
    val colorHex: Long
)

data class PredictiveSlot(
    val id: String,
    val timeRange: String,
    val durationMinutes: Int,
    val title: String,
    val category: String, // "Gym/Health", "Deep Work", "Guilt-free Chill", "Reset"
    val whyRecommended: String,
    val vibeTag: String,
    val isBooked: Boolean = false
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val screenContextAttached: Boolean = false,
    val voiceAudioSpoken: Boolean = false
)

enum class MessageSender {
    USER,
    FX_AI
}
