package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.automation.AutomationEngine
import com.example.model.VibeMode
import com.example.voice.FxVoiceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FX Assistant", appName)
    }

    @Test
    fun `verify initial automation recipes count`() {
        val recipes = AutomationEngine.getInitialRecipes()
        assertTrue(recipes.isNotEmpty())
        assertTrue(recipes.any { it.id == "doomscroll_breaker" })
    }

    @Test
    fun `verify vibe formatting for Gen Z`() {
        val formatted = FxVoiceEngine.formatTextForVibe("Task is complete", VibeMode.GEN_Z)
        assertTrue(formatted.isNotEmpty())
    }
}
