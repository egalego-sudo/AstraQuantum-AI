package com.bittupandit.astraquantumai

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityCoreFlowTest {
    companion object {
        @BeforeClass
        @JvmStatic
        fun disableDeviceLockForAutomatedUiTest() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.getSharedPreferences("quantum_app_security", android.content.Context.MODE_PRIVATE)
                .edit()
                .putBoolean("app_lock_enabled", false)
                .commit()
        }
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun chatNavigationAndPrivateMemorySurviveActivityRecreation() {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Memory", substring = false).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("AI Chat", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("AI CONVERSATION").assertExists()

        composeRule.onNodeWithText("Memory", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("ADVANCED PRIVATE MEMORY").assertExists()

        // Personal context must be opt-in, not silently enabled by default.
        composeRule.onAllNodes(isToggleable()).onFirst().assertIsOff()

        composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("QUANTUM-AI emulator persistence check")
        composeRule.onNodeWithText("Memory में सेव करें").performClick()
        composeRule.onNodeWithText("QUANTUM-AI emulator persistence check", substring = true).assertExists()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Memory", substring = false).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Memory", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("ADVANCED PRIVATE MEMORY").assertExists()
        composeRule.onNodeWithText("QUANTUM-AI emulator persistence check", substring = true).assertExists()

        // Leave the emulator's app data clean for any following tests.
        composeRule.onNodeWithText("सभी Memory मिटाएँ और उपयोग बंद करें").performClick()
    }
}
