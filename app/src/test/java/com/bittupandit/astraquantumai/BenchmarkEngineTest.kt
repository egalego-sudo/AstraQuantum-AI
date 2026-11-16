package com.bittupandit.astraquantumai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BenchmarkEngineTest {
    @Test
    fun questionBankContainsTwentyQuestionsInEachOfSixModules() {
        assertEquals(6, BQuestionBank.moduleNames.size)
        assertEquals(120, BQuestionBank.all.size)
        BQuestionBank.moduleNames.forEach { module ->
            assertEquals(20, BQuestionBank.all.count { it.module == module })
        }
    }

    @Test
    fun brierScoreUsesHumanReviewAsOutcome() {
        val rows = listOf(
            BRecord("1", "q1", "m", "a", 0.9, 4, 1L),
            BRecord("2", "q2", "m", "b", 0.8, 2, 2L)
        )
        assertEquals(0.325, BMetrics.brier(rows)!!, 0.000001)
    }

    @Test
    fun falseConfidenceAveragesConfidenceOnlyForIncorrectReviewedAnswers() {
        val rows = listOf(
            BRecord("1", "q1", "m", "a", 0.9, 4, 1L),
            BRecord("2", "q2", "m", "b", 0.8, 2, 2L),
            BRecord("3", "q3", "m", "c", 0.6, 1, 3L)
        )
        assertEquals(0.7, BMetrics.falseConfidence(rows)!!, 0.000001)
    }

    @Test
    fun eceIsCalculatedForReviewedRows() {
        val rows = listOf(
            BRecord("1", "q1", "m", "a", 0.9, 4, 1L),
            BRecord("2", "q2", "m", "b", 0.8, 2, 2L)
        )
        assertNotNull(BMetrics.ece(rows))
        assertEquals(0.35, BMetrics.ece(rows)!!, 0.000001)
    }
}
