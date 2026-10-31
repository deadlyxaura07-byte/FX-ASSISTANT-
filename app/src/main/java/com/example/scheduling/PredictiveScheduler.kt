package com.example.scheduling

import com.example.model.PredictiveSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScheduleState(
    val totalOptimizedMinutes: Int = 145,
    val freeTimeReclaimedScore: Int = 85,
    val currentEnergyWindow: String = "High Focus Mode",
    val slots: List<PredictiveSlot> = emptyList(),
    val quickRecommendations: List<String> = emptyList()
)

class PredictiveScheduler {

    private val _scheduleState = MutableStateFlow(
        ScheduleState(
            totalOptimizedMinutes = 140,
            freeTimeReclaimedScore = 88,
            currentEnergyWindow = "⚡ High Creative Energy (Peak 2-5 PM)",
            slots = listOf(
                PredictiveSlot(
                    id = "slot_1",
                    timeRange = "12:30 PM - 1:15 PM",
                    durationMinutes = 45,
                    title = "Post-Lunch Active Recharge",
                    category = "Gym/Health",
                    whyRecommended = "Prevents food coma & doomscroll slump. 45m window detected between study/work classes.",
                    vibeTag = "Touch Grass & Move 🏃",
                    isBooked = false
                ),
                PredictiveSlot(
                    id = "slot_2",
                    timeRange = "4:45 PM - 5:30 PM",
                    durationMinutes = 45,
                    title = "Hyperfocus Skill Sprint",
                    category = "Deep Work",
                    whyRecommended = "Peak second-wind cognitive cycle. Ideal for coding, video editing, or Duolingo.",
                    vibeTag = "Cook Something Fire 💻",
                    isBooked = true
                ),
                PredictiveSlot(
                    id = "slot_3",
                    timeRange = "7:15 PM - 8:00 PM",
                    durationMinutes = 45,
                    title = "Guilt-Free Chill & Gaming",
                    category = "Guilt-free Chill",
                    whyRecommended = "Scheduled dopamine reward: Zero guilt gaming or catching up on videos.",
                    vibeTag = "Earned Dopamine 🎮",
                    isBooked = false
                ),
                PredictiveSlot(
                    id = "slot_4",
                    timeRange = "10:15 PM - 10:45 PM",
                    durationMinutes = 30,
                    title = "Night Wind-Down & Soundscape",
                    category = "Reset",
                    whyRecommended = "Screen dimming and lo-fi audio session to guarantee 8h quality sleep.",
                    vibeTag = "Recharge Aura 🌙",
                    isBooked = false
                )
            ),
            quickRecommendations = listOf(
                "⚡ Swap 30m Instagram doomscroll for 30m gym session to boost aura by +200",
                "🎯 Next free slot starts at 4:45 PM (45m window) — ready to lock in?",
                "💤 Wind-down starts at 10:15 PM — FX Assistant will mute notifications automatically"
            )
        )
    )
    val scheduleState: StateFlow<ScheduleState> = _scheduleState.asStateFlow()

    fun toggleBookSlot(slotId: String) {
        val updated = _scheduleState.value.slots.map {
            if (it.id == slotId) it.copy(isBooked = !it.isBooked) else it
        }
        val bookedCount = updated.count { it.isBooked }
        _scheduleState.value = _scheduleState.value.copy(
            slots = updated,
            freeTimeReclaimedScore = (70 + (bookedCount * 8)).coerceAtMost(100)
        )
    }

    fun optimizeDayAI() {
        val updated = _scheduleState.value.slots.map { it.copy(isBooked = true) }
        _scheduleState.value = _scheduleState.value.copy(
            slots = updated,
            freeTimeReclaimedScore = 98,
            quickRecommendations = listOf(
                "🔥 All free time windows scheduled! Your day is 100% optimized.",
                "🚀 Automation triggers armed for Gym (12:30 PM) and Skill Sprint (4:45 PM)."
            )
        )
    }
}
