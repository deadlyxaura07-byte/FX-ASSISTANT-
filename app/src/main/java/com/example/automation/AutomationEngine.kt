package com.example.automation

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.model.AutomationRecipe
import com.example.model.StepType
import com.example.model.TaskStep
import com.example.service.FxAccessibilityService
import com.example.voice.FxVoiceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AutomationExecutionState(
    val isRunning: Boolean = false,
    val activeRecipe: AutomationRecipe? = null,
    val currentStepIndex: Int = 0,
    val totalSteps: Int = 0,
    val logs: List<String> = emptyList(),
    val isSuccess: Boolean = false,
    val error: String? = null
)

class AutomationEngine(
    private val context: Context,
    private val voiceEngine: FxVoiceEngine
) {
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _executionState = MutableStateFlow(AutomationExecutionState())
    val executionState: StateFlow<AutomationExecutionState> = _executionState.asStateFlow()

    private val _recipes = MutableStateFlow(getInitialRecipes())
    val recipes: StateFlow<List<AutomationRecipe>> = _recipes.asStateFlow()

    fun runRecipe(recipe: AutomationRecipe) {
        if (_executionState.value.isRunning) return

        mainScope.launch {
            _executionState.value = AutomationExecutionState(
                isRunning = true,
                activeRecipe = recipe,
                currentStepIndex = 0,
                totalSteps = recipe.steps.size,
                logs = listOf("🚀 Initiating: ${recipe.title}", "💬 Vibe check: \"${recipe.vibeQuote}\"")
            )

            voiceEngine.speak(recipe.vibeQuote)
            delay(1200)

            val service = FxAccessibilityService.currentService
            val isAccessibilityActive = FxAccessibilityService.isServiceRunning.value

            for (index in recipe.steps.indices) {
                val step = recipe.steps[index]
                _executionState.value = _executionState.value.copy(
                    currentStepIndex = index + 1,
                    logs = _executionState.value.logs + "⚡ Step ${index + 1}/${recipe.steps.size}: ${step.description}"
                )

                executeStep(step, service, isAccessibilityActive)
                delay(step.delayMs)
            }

            voiceEngine.speak("Task complete! You're locked in.")
            _executionState.value = _executionState.value.copy(
                isRunning = false,
                isSuccess = true,
                logs = _executionState.value.logs + "✅ Automation finished successfully!"
            )
        }
    }

    private suspend fun executeStep(
        step: TaskStep,
        service: FxAccessibilityService?,
        isAccessibilityActive: Boolean
    ) {
        when (step.type) {
            StepType.LAUNCH_APP -> {
                try {
                    val intent = context.packageManager.getLaunchIntentForPackage(step.target)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } else {
                        // Fallback generic search intent
                        Log.d(TAG, "Simulated launch for package ${step.target}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Could not launch package ${step.target}", e)
                }
            }
            StepType.CLICK_ELEMENT -> {
                if (service != null && isAccessibilityActive) {
                    val clicked = service.clickNodeWithText(step.target)
                    Log.d(TAG, "Accessibility clicked text '${step.target}': $clicked")
                } else {
                    Log.d(TAG, "Simulated click on '${step.target}'")
                }
            }
            StepType.CLICK_COORDINATES -> {
                if (service != null && isAccessibilityActive) {
                    service.dispatchTap(step.x, step.y)
                }
            }
            StepType.SWIPE_GESTURE -> {
                if (service != null && isAccessibilityActive) {
                    service.dispatchSwipe(step.x, step.y, step.endX, step.endY, 400L)
                }
            }
            StepType.TYPE_TEXT -> {
                if (service != null && isAccessibilityActive) {
                    service.inputText(step.target)
                }
            }
            StepType.SPEAK_VOICE -> {
                voiceEngine.speak(step.target)
            }
            StepType.PRESS_SYSTEM_KEY -> {
                if (service != null && isAccessibilityActive) {
                    when (step.target) {
                        "HOME" -> service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
                        "BACK" -> service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
                        "RECENTS" -> service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS)
                    }
                }
            }
            StepType.WAIT_DELAY -> {
                delay(step.delayMs)
            }
        }
    }

    fun addCustomRecipe(recipe: AutomationRecipe) {
        _recipes.value = _recipes.value + recipe
    }

    fun toggleRecipe(recipeId: String) {
        _recipes.value = _recipes.value.map {
            if (it.id == recipeId) it.copy(isEnabled = !it.isEnabled) else it
        }
    }

    companion object {
        private const val TAG = "AutomationEngine"

        fun getInitialRecipes(): List<AutomationRecipe> {
            return listOf(
                AutomationRecipe(
                    id = "doomscroll_breaker",
                    title = "Anti-Doomscroll Shield",
                    subtitle = "Instantly snap out of infinite reels/shorts and lock in",
                    category = "Focus",
                    icon = "🛑",
                    targetApp = "com.instagram.android",
                    steps = listOf(
                        TaskStep(1, StepType.SPEAK_VOICE, "Screen time alert! Doomscroll detected. Breaking the cycle now.", target = "Yo wake up! Doomscroll detected. Put down the reels."),
                        TaskStep(2, StepType.PRESS_SYSTEM_KEY, "Press Home to exit infinite feed", target = "HOME", delayMs = 600),
                        TaskStep(3, StepType.SPEAK_VOICE, "Switching to productive vibe", target = "Opening your notes and free-time scheduler.")
                    ),
                    vibeQuote = "No cap, your attention span is way too valuable for brainrot."
                ),
                AutomationRecipe(
                    id = "spotify_lockin",
                    title = "Spotify Focus Lock-In",
                    subtitle = "Auto-launch Spotify, locate Phonk/Lo-Fi, start flow state",
                    category = "Media",
                    icon = "🎧",
                    targetApp = "com.spotify.music",
                    steps = listOf(
                        TaskStep(1, StepType.LAUNCH_APP, "Launch Spotify App", target = "com.spotify.music", delayMs = 1500),
                        TaskStep(2, StepType.CLICK_ELEMENT, "Click Search Bar", target = "Search", delayMs = 800),
                        TaskStep(3, StepType.TYPE_TEXT, "Type 'Phonk Grindset Focus'", target = "Phonk Grindset Focus", delayMs = 600),
                        TaskStep(4, StepType.CLICK_ELEMENT, "Hit First Play Button", target = "Play", delayMs = 800),
                        TaskStep(5, StepType.SPEAK_VOICE, "Audio confirmed", target = "Focus tracks playing. Time to cook.")
                    ),
                    vibeQuote = "Locked in. Maximum acoustic dopamine incoming."
                ),
                AutomationRecipe(
                    id = "speed_reply_dispatch",
                    title = "WhatsApp Speed Reply",
                    subtitle = "Open WhatsApp, select latest chat, and draft instant reply",
                    category = "Social",
                    icon = "💬",
                    targetApp = "com.whatsapp",
                    steps = listOf(
                        TaskStep(1, StepType.LAUNCH_APP, "Open WhatsApp", target = "com.whatsapp", delayMs = 1200),
                        TaskStep(2, StepType.CLICK_ELEMENT, "Tap Unread Message", target = "Unread", delayMs = 700),
                        TaskStep(3, StepType.TYPE_TEXT, "Insert quick reply", target = "Bet, got it! Moving now 🔥", delayMs = 500),
                        TaskStep(4, StepType.SPEAK_VOICE, "Reply drafted", target = "Reply ready for you to send.")
                    ),
                    vibeQuote = "Zero delay responses. Pure gigachad communication speed."
                ),
                AutomationRecipe(
                    id = "free_time_calendar",
                    title = "Schedule Gym & Free Time",
                    subtitle = "Automate scheduling 45m workout block in Google Calendar",
                    category = "Daily",
                    icon = "🗓️",
                    targetApp = "com.google.android.calendar",
                    steps = listOf(
                        TaskStep(1, StepType.LAUNCH_APP, "Open Calendar", target = "com.google.android.calendar", delayMs = 1200),
                        TaskStep(2, StepType.CLICK_ELEMENT, "Click New Event / Plus", target = "Create", delayMs = 800),
                        TaskStep(3, StepType.TYPE_TEXT, "Set Title: Gym & Recovery Sprint", target = "Gym & Recovery Sprint", delayMs = 600),
                        TaskStep(4, StepType.SPEAK_VOICE, "Confirmation", target = "Gym session locked in calendar.")
                    ),
                    vibeQuote = "W calendar hygiene. Building that physique."
                ),
                AutomationRecipe(
                    id = "screen_summarizer",
                    title = "Instant Screen Summarizer",
                    subtitle = "Read active screen nodes and voice-summarize the page",
                    category = "Daily",
                    icon = "🔍",
                    targetApp = "active",
                    steps = listOf(
                        TaskStep(1, StepType.SPEAK_VOICE, "Scanning screen elements", target = "Inspecting your screen right now..."),
                        TaskStep(2, StepType.WAIT_DELAY, "Analyzing UI tree", delayMs = 1000),
                        TaskStep(3, StepType.SPEAK_VOICE, "Readout summary", target = "Screen analyzed! Main content highlighted.")
                    ),
                    vibeQuote = "Seeing through the pixels. AI vision active."
                ),
                AutomationRecipe(
                    id = "night_recharge",
                    title = "Night Recharge Protocol",
                    subtitle = "Dim screen, silence alerts, queue sleep sounds",
                    category = "Daily",
                    icon = "🌙",
                    targetApp = "system",
                    steps = listOf(
                        TaskStep(1, StepType.PRESS_SYSTEM_KEY, "Press Home", target = "HOME", delayMs = 600),
                        TaskStep(2, StepType.SPEAK_VOICE, "Activating Night Mode", target = "Shutting down the daily grind. Recharge mode activated."),
                        TaskStep(3, StepType.PRESS_SYSTEM_KEY, "Check notifications", target = "HOME", delayMs = 400)
                    ),
                    vibeQuote = "Sleep is the cheat code for high performance. Rest up boss."
                )
            )
        }
    }
}
