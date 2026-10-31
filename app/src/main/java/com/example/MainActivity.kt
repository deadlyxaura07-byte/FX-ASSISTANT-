package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.BottomNavBar
import com.example.ui.components.FloatingAssistantOrb
import com.example.ui.components.HeaderBar
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ControlScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.CyberObsidian
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FxAssistantApp()
            }
        }
    }
}

@Composable
fun FxAssistantApp(viewModel: MainViewModel = viewModel()) {
    val currentTab by viewModel.currentTab.collectAsState()
    val screenState by viewModel.screenState.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()
    val isBackgroundVoiceActive by viewModel.isBackgroundVoiceActive.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val vibeMode by viewModel.vibeMode.collectAsState()
    val automationState by viewModel.automationState.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    val analytics by viewModel.analytics.collectAsState()
    val scheduleState by viewModel.scheduleState.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isAiGenerating by viewModel.isAiGenerating.collectAsState()

    // Audio Permission Launcher for Voice Recognition
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleVoiceListening()
        }
    }

    // Back handler to navigate back to Control tab
    BackHandler(enabled = currentTab != AppNavTab.CONTROL) {
        viewModel.selectTab(AppNavTab.CONTROL)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberObsidian),
        containerColor = CyberObsidian,
        topBar = {
            HeaderBar(
                isAccessibilityActive = isAccessibilityActive,
                isBackgroundVoiceActive = isBackgroundVoiceActive,
                isSpeaking = isSpeaking,
                isListening = isListening,
                vibeMode = vibeMode,
                onVibeSelected = { viewModel.setVibeMode(it) },
                onAccessibilityClicked = { viewModel.openAccessibilitySettings() },
                onBackgroundVoiceToggle = { viewModel.toggleBackgroundVoice() }
            )
        },
        bottomBar = {
            BottomNavBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppNavTab.CONTROL -> {
                    ControlScreen(
                        viewModel = viewModel,
                        screenState = screenState,
                        isAccessibilityActive = isAccessibilityActive,
                        isBackgroundVoiceActive = isBackgroundVoiceActive,
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        recipes = recipes,
                        onRunRecipe = { viewModel.runRecipe(it) }
                    )
                }
                AppNavTab.TASKS -> {
                    TasksScreen(
                        executionState = automationState,
                        recipes = recipes,
                        onRunRecipe = { viewModel.runRecipe(it) },
                        onAddRecipe = { viewModel.automationEngine.addCustomRecipe(it) }
                    )
                }
                AppNavTab.INSIGHTS -> {
                    InsightsScreen(
                        analytics = analytics,
                        onSpeakRoast = { viewModel.voiceEngine.speak(it) },
                        onRefresh = { viewModel.refreshAnalytics() },
                        onOpenUsageSettings = { viewModel.openUsageSettings() },
                        onTriggerAction = { actionType ->
                            when (actionType) {
                                "DOOMSCROLL" -> {
                                    val doomscrollRecipe = recipes.find { it.id == "doomscroll_breaker" }
                                    if (doomscrollRecipe != null) viewModel.runRecipe(doomscrollRecipe)
                                }
                                "SCHEDULE" -> viewModel.selectTab(AppNavTab.SCHEDULE)
                                "SPOTIFY" -> {
                                    val spotifyRecipe = recipes.find { it.id == "spotify_lockin" }
                                    if (spotifyRecipe != null) viewModel.runRecipe(spotifyRecipe)
                                }
                            }
                        }
                    )
                }
                AppNavTab.SCHEDULE -> {
                    ScheduleScreen(
                        scheduleState = scheduleState,
                        onToggleBookSlot = { viewModel.toggleBookSlot(it) },
                        onOptimizeDayAI = { viewModel.optimizeDayAI() }
                    )
                }
                AppNavTab.CHAT -> {
                    ChatScreen(
                        messages = chatMessages,
                        isAiGenerating = isAiGenerating,
                        isListening = isListening,
                        vibeMode = vibeMode,
                        screenState = screenState,
                        onSendMessage = { viewModel.sendChatMessage(it) },
                        onToggleMic = {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onSpeakMessage = { viewModel.voiceEngine.speak(it) }
                    )
                }
            }

            // Floating Assistant Orb (Dynamic screen companion)
            Box(
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                FloatingAssistantOrb(
                    screenState = screenState,
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    onInspectScreen = { viewModel.inspectScreenNow() },
                    onToggleMic = {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onTriggerQuickAction = {
                        val doomscroll = recipes.find { it.id == "doomscroll_breaker" }
                        if (doomscroll != null) viewModel.runRecipe(doomscroll)
                    },
                    onCloseOrb = {}
                )
            }
        }
    }
}
