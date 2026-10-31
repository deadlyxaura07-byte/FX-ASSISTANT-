package com.example

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiAssistant
import com.example.analytics.UsageAnalyticsEngine
import com.example.automation.AutomationEngine
import com.example.model.AutomationRecipe
import com.example.model.VibeMode
import com.example.scheduling.PredictiveScheduler
import com.example.service.FxAccessibilityService
import com.example.service.FxVoiceOverlayService
import com.example.voice.FxVoiceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppNavTab(val title: String, val icon: String) {
    CONTROL("Control", "📱"),
    TASKS("Tasks", "⚡"),
    INSIGHTS("Insights", "📊"),
    SCHEDULE("Schedule", "🗓️"),
    CHAT("AI Voice", "💬")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val voiceEngine = FxVoiceEngine(application)
    val automationEngine = AutomationEngine(application, voiceEngine)
    val analyticsEngine = UsageAnalyticsEngine(application)
    val scheduler = PredictiveScheduler()
    val geminiAssistant = GeminiAssistant(application)

    private val _currentTab = MutableStateFlow(AppNavTab.CONTROL)
    val currentTab: StateFlow<AppNavTab> = _currentTab.asStateFlow()

    val screenState = FxAccessibilityService.screenState
    val isAccessibilityActive = FxAccessibilityService.isServiceRunning
    val isBackgroundVoiceActive = FxVoiceOverlayService.isBackgroundServiceActive

    val isSpeaking = voiceEngine.isSpeaking
    val isListening = voiceEngine.isListening
    val vibeMode = voiceEngine.vibeMode

    val automationState = automationEngine.executionState
    val recipes = automationEngine.recipes

    val analytics = analyticsEngine.analytics
    val scheduleState = scheduler.scheduleState

    val chatMessages = geminiAssistant.messages
    val isAiGenerating = geminiAssistant.isGenerating

    private val _floatingOrbEnabled = MutableStateFlow(false)
    val floatingOrbEnabled: StateFlow<Boolean> = _floatingOrbEnabled.asStateFlow()

    init {
        voiceEngine.onSpeechRecognized = { speech ->
            handleVoiceCommand(speech)
        }
    }

    fun selectTab(tab: AppNavTab) {
        _currentTab.value = tab
    }

    fun setVibeMode(mode: VibeMode) {
        voiceEngine.setVibe(mode)
        analyticsEngine.refreshAnalytics(mode)
        voiceEngine.speak("Switched to ${mode.displayName}. ${mode.tagline}", mode)
    }

    fun toggleVoiceListening() {
        if (isListening.value) {
            voiceEngine.stopListening()
        } else {
            voiceEngine.startListening()
        }
    }

    fun toggleBackgroundVoice() {
        val app = getApplication<Application>()
        if (isBackgroundVoiceActive.value) {
            FxVoiceOverlayService.stop(app)
            voiceEngine.speak("Background voice suspended.")
        } else {
            FxVoiceOverlayService.start(app)
            voiceEngine.speak("Background voice active. I can hear you and control your screen from any app!")
        }
    }

    fun toggleFloatingOrb() {
        _floatingOrbEnabled.value = !_floatingOrbEnabled.value
        if (_floatingOrbEnabled.value) {
            voiceEngine.speak("Floating screen orb enabled. Tap it anywhere to automate.")
        }
    }

    fun inspectScreenNow() {
        val currentPkg = screenState.value.packageName
        val appName = screenState.value.appName
        val summary = screenState.value.summary
        voiceEngine.speak("Scanning $appName screen. $summary")
    }

    fun runRecipe(recipe: AutomationRecipe) {
        automationEngine.runRecipe(recipe)
    }

    fun toggleBookSlot(slotId: String) {
        scheduler.toggleBookSlot(slotId)
        voiceEngine.speak("Updated schedule for optimal free time.")
    }

    fun optimizeDayAI() {
        scheduler.optimizeDayAI()
        voiceEngine.speak("Your entire day has been predictively optimized! All free time windows secured.")
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        geminiAssistant.sendMessage(text, screenState.value, vibeMode.value) { reply ->
            voiceEngine.speak(reply)
        }
    }

    fun handleVoiceCommand(spokenText: String) {
        sendChatMessage(spokenText)
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            getApplication<Application>().startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${getApplication<Application>().packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            getApplication<Application>().startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openUsageSettings() {
        analyticsEngine.openUsageSettings()
    }

    fun refreshAnalytics() {
        analyticsEngine.refreshAnalytics(vibeMode.value)
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.destroy()
    }
}
