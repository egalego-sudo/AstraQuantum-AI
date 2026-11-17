package com.bittupandit.astraquantumai

import org.junit.Assert.assertTrue
import org.junit.Test

class ScientificIntelligenceTest {
    @Test
    fun enhancedPromptContainsAllSevenReasoningLayers() {
        val prompt = ScientificIntelligence.enhancePrompt("Explain a difficult problem")
        val expected = listOf(
            "DEEP REASONING",
            "MULTI-LAYER THINKING",
            "SELF-CORRECTION",
            "SCIENTIFIC & SOURCE VERIFICATION",
            "CREATIVE INTELLIGENCE",
            "CONTEXT & MEMORY REASONING",
            "STRATEGIC PLANNING"
        )
        expected.forEach { assertTrue("Missing reasoning layer: $it", prompt.contains(it)) }
        assertTrue(prompt.contains("[CURRENT USER REQUEST]\nExplain a difficult problem"))
    }

    @Test
    fun enhancedPromptDoesNotClaimGuaranteedCorrectness() {
        val prompt = ScientificIntelligence.enhancePrompt("Check this claim")
        assertTrue(prompt.contains("Do not claim a human IQ score"))
        assertTrue(prompt.contains("Never invent citations"))
        assertTrue(prompt.contains("Do not reveal private hidden chain-of-thought"))
    }

    @Test
    fun keepsCurrentQuestionFirstSoBudgetCompactionCannotDropIt() {
        val question = "यदि भविष्य पहले से तय है, तो क्या इंसान के पास स्वतंत्र इच्छा है?"
        val prompt = ScientificIntelligence.enhancePrompt(question)
        assertTrue(prompt.startsWith("[CURRENT USER REQUEST]\n$question"))
    }

    @Test
    fun directsAssistantToAnswerClearShortQuestions() {
        val prompt = ScientificIntelligence.enhancePrompt("2 + 2 कितना है?")
        assertTrue(prompt.contains("Never ask the user to complete or rewrite a clear question"))
        assertTrue(prompt.contains("For short questions, give a short answer"))
    }
}
