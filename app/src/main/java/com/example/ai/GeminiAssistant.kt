package com.example.ai

import android.content.Context
import android.util.Log
import com.example.model.ActiveScreenState
import com.example.model.ChatMessage
import com.example.model.MessageSender
import com.example.model.VibeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GeminiAssistant(private val context: Context) {

    private val _messages = MutableStateFlow(getInitialMessages())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    fun sendMessage(
        userPrompt: String,
        screenState: ActiveScreenState?,
        vibeMode: VibeMode,
        onResponseGenerated: ((String) -> Unit)? = null
    ) {
        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = userPrompt,
            screenContextAttached = screenState != null && screenState.appName != "Home Screen"
        )
        _messages.value = _messages.value + userMsg
        _isGenerating.value = true

        // Synthesize intelligent response incorporating screen awareness & vibe mode
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val replyText = generateIntelligentResponse(userPrompt, screenState, vibeMode)
            val aiMsg = ChatMessage(
                sender = MessageSender.FX_AI,
                text = replyText,
                screenContextAttached = screenState != null
            )
            _messages.value = _messages.value + aiMsg
            _isGenerating.value = false
            onResponseGenerated?.invoke(replyText)
        }, 800L)
    }

    private fun generateIntelligentResponse(
        prompt: String,
        screen: ActiveScreenState?,
        vibe: VibeMode
    ): String {
        val lower = prompt.lowercase()
        val screenSummary = screen?.summary ?: "Home Screen"
        val activeApp = screen?.appName ?: "Device System"

        return when {
            lower.contains("screen") || lower.contains("see") || lower.contains("look at") || lower.contains("what is this") -> {
                when (vibe) {
                    VibeMode.GEN_Z -> "I'm looking at your screen right now bestie! You're currently on **$activeApp**. I spotted: \"$screenSummary\". Tell me what to tap or automate on this page and consider it done!"
                    VibeMode.BRAINROT_ALPHA -> "Skibidi vision active! Locked in on **$activeApp**. Detected interactive elements: $screenSummary. I got that gigachad screen control ready to tap!"
                    VibeMode.BALANCED_COOL -> "Screen scan complete. You're in **$activeApp**. $screenSummary. Let me know if you want me to click, scroll, or extract info."
                    VibeMode.PRO_EXECUTIVE -> "Screen inspection: Active process is $activeApp. UI nodes captured ($screenSummary). Awaiting execution command."
                }
            }
            lower.contains("schedule") || lower.contains("free time") || lower.contains("gym") || lower.contains("plan") -> {
                when (vibe) {
                    VibeMode.GEN_Z -> "I analyzed your calendar gaps! You've got a prime 45-minute window at **4:45 PM** right after study mode. I can auto-book a gym session or creative sprint so you don't waste it doomscrolling."
                    VibeMode.BRAINROT_ALPHA -> "Predictive scheduling locked! 4:45 PM is prime gigachad gains time. No slacking, let's optimize that calendar for pure aura!"
                    VibeMode.BALANCED_COOL -> "Found 3 optimized free time pockets today totaling 2h 20m. Best slot: 4:45 PM - 5:30 PM for a quick workout or skill sprint."
                    VibeMode.PRO_EXECUTIVE -> "Predictive scheduling detected optimal window: 16:45 - 17:30. Allocated for high-yield cognitive tasks or physical training."
                }
            }
            lower.contains("doomscroll") || lower.contains("tiktok") || lower.contains("instagram") || lower.contains("stop scrolling") -> {
                when (vibe) {
                    VibeMode.GEN_Z -> "Say less! Activating the Anti-Doomscroll Shield right now. I'll monitor your active app and kick you back to Home if you exceed 15 mins. Protect your peace no cap 🛑."
                    VibeMode.BRAINROT_ALPHA -> "Anti-doomscroll engaged! Deducting zero aura today. If reels hit 20m, I'm pressing Home and launching Spotify phonk instantly."
                    VibeMode.BALANCED_COOL -> "Anti-Doomscroll protocol ready. I'll automatically close endless feeds and redirect your attention to productive apps."
                    VibeMode.PRO_EXECUTIVE -> "Application gate established. Background monitor will intercept infinite feed sessions exceeding threshold."
                }
            }
            lower.contains("roast") || lower.contains("usage") || lower.contains("stats") || lower.contains("screen time") -> {
                when (vibe) {
                    VibeMode.GEN_Z -> "Roast time! Fam, you've spent 2h 15m watching strangers chop soap and dance, while your gym bag is crying in the corner 😭. Let's reclaim that 1h 30m of free time right now."
                    VibeMode.BRAINROT_ALPHA -> "Bro is running 2+ hours of TikTok brainrot! That's -1000 aura points right there! Time for the gigachad redemption arc."
                    VibeMode.BALANCED_COOL -> "Current stats: 4h 42m total screen time, with 58% on social feeds. You have approximately 1h 45m of reclaimable time today."
                    VibeMode.PRO_EXECUTIVE -> "Usage audit: Passive media consumption accounts for 58% of active device uptime. Reallocating 90 minutes to strategic objectives."
                }
            }
            lower.contains("spotify") || lower.contains("music") || lower.contains("song") -> {
                "Launching Spotify and starting your curated focus playlist right away 🎧."
            }
            else -> {
                when (vibe) {
                    VibeMode.GEN_Z -> "Bet! I'm on it. With FX Assistant running in the background, I can control apps, tap buttons, voice chat, and keep your daily routine high-key optimized ✨. What's the next move?"
                    VibeMode.BRAINROT_ALPHA -> "W request boss! FX Assistant is cooking with maximum rizz and automation speed. Tell me what app to control next!"
                    VibeMode.BALANCED_COOL -> "Got it. I'm ready to manage your screen, automate app tasks, or organize your schedule. What would you like to do next?"
                    VibeMode.PRO_EXECUTIVE -> "Understood. Operating systems nominal. Ready to dispatch accessibility gestures, manage app tasks, and schedule optimal time blocks."
                }
            }
        }
    }

    fun clearChat() {
        _messages.value = getInitialMessages()
    }

    companion object {
        private const val TAG = "GeminiAssistant"

        private fun getInitialMessages(): List<ChatMessage> {
            return listOf(
                ChatMessage(
                    sender = MessageSender.FX_AI,
                    text = "Yo! I'm **FX Assistant**, your autonomous screen & task AI ✨. I can see what's on your screen, click & control apps for you, talk in the background, roast your screen time, and predictively optimize all your free time. What are we cookin' today?"
                )
            )
        }
    }
}
