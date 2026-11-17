package com.bittupandit.astraquantumai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextIntelligenceTest {
    @Test fun doesNotShareLocalContextWhenOptInIsOff() {
        val result = ContextIntelligence.buildPrompt(
            question = "इसी गणना को आगे बढ़ाओ",
            history = listOf(ContextTurn("You", "Electron barrier E=4 eV, V0=6 eV, L=0.5 nm")),
            memories = "private note",
            projects = "private project",
            includePersonalContext = false
        )
        assertTrue(result == "इसी गणना को आगे बढ़ाओ")
        assertFalse(result.contains("private note"))
    }

    @Test fun retrievesEarlierNumericContextAndAddsContinuityInstructions() {
        val history = buildList {
            add(ContextTurn("You", "Electron tunneling barrier: E=4 eV, V0=6 eV, L=0.5 nm"))
            add(ContextTurn("ASTRA", "The initial kappa calculation is 7.24e9 m^-1"))
            repeat(22) { add(ContextTurn("You", "Unrelated conversation topic number $it")) }
        }
        val result = ContextIntelligence.buildPrompt(
            question = "इलेक्ट्रॉन टनलिंग की वही गणना आगे करो",
            history = history,
            includePersonalContext = true
        )
        assertTrue(result.contains("E=4 eV"))
        assertTrue(result.contains("V0=6 eV"))
        assertTrue(result.contains("L=0.5 nm"))
        assertTrue(result.contains("वही जानकारी फिर न माँगें"))
    }

    @Test fun preservesRecentConversationWhenNoKeywordMatchExists() {
        val history = listOf(
            ContextTurn("You", "पहले की शर्त: तापमान 300 K"),
            ContextTurn("ASTRA", "समझ गया।"),
            ContextTurn("You", "अब आगे बढ़ो")
        )
        val result = ContextIntelligence.buildPrompt(
            question = "जारी रखो",
            history = history,
            includePersonalContext = true
        )
        assertTrue(result.contains("तापमान 300 K"))
    }

    @Test fun boundsOptInContextEvenForVeryLongHistoriesAndMemory() {
        val history = List(100) { index ->
            ContextTurn(if (index % 2 == 0) "You" else "ASTRA", "विषय $index " + "विस्तृत संदर्भ ".repeat(100))
        }
        val result = ContextIntelligence.buildPrompt(
            question = "आगे बढ़ो",
            history = history,
            memories = "निजी स्मृति ".repeat(500),
            projects = "प्रोजेक्ट नोट ".repeat(500),
            includePersonalContext = true
        )
        assertTrue(result.length < 9_000)
        assertTrue(result.contains("आगे बढ़ो"))
    }
}
