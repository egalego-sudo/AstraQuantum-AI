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

    @Test
    fun addsRootCauseAndSoftwareChecksForDebuggingRequests() {
        val prompt = ScientificIntelligence.enhancePrompt("Android app error का root cause खोजकर code fix करो")
        assertTrue(prompt.contains("ROOT-CAUSE CHECK"))
        assertTrue(prompt.contains("SOFTWARE CHECK"))
        assertTrue(prompt.contains("regression check"))
    }

    @Test
    fun addsResearchAndExperimentChecksForScientificQuestions() {
        val prompt = ScientificIntelligence.enhancePrompt("इस hypothesis पर research करके experiment design करो")
        assertTrue(prompt.contains("RESEARCH CHECK"))
        assertTrue(prompt.contains("EXPERIMENT CHECK"))
        assertTrue(prompt.contains("UNVERIFIED"))
    }

    @Test
    fun addsAiEngineeringAndEducationChecks() {
        val aiPrompt = ScientificIntelligence.enhancePrompt("Transformer foundation model with RAG बनाना है")
        val learningPrompt = ScientificIntelligence.enhancePrompt("मुझे calculus शुरुआत से समझाइए")
        assertTrue(aiPrompt.contains("AI ENGINEERING CHECK"))
        assertTrue(aiPrompt.contains("trained model capability"))
        assertTrue(learningPrompt.contains("EDUCATION CHECK"))
        assertTrue(learningPrompt.contains("worked example"))
    }

    @Test
    fun capsTaskSpecificChecksAtFourAndKeepsGeneralFallback() {
        val complex = ScientificIntelligence.enhancePrompt("AI research hypothesis experiment code debugging physics decision strategy")
        val specialized = complex.lines().count { it.contains(" CHECK:") }
        assertTrue(specialized <= 4)
        val general = ScientificIntelligence.enhancePrompt("नमस्ते")
        assertTrue(general.contains("GENERAL CHECK"))
    }


    @Test
    fun recognizesTheRequestedProgrammingLanguageNamesOffline() {
        val languages = listOf(
            "Python", "JavaScript", "TypeScript", "Java", "Kotlin", "C", "C++", "C#",
            "Go", "Golang", "Rust", "PHP", "Swift", "SQL", "HTML", "CSS",
            "Verilog", "SystemVerilog", "Bash"
        )
        languages.forEach { language ->
            val prompt = ScientificIntelligence.enhancePrompt(language)
            assertTrue("Language not recognized offline: $language", prompt.contains("SOFTWARE CHECK"))
        }
    }

}
