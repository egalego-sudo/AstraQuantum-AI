package com.bittupandit.astraquantumai

/**
 * Builds a bounded, relevance-ranked prompt context from the current local chat.
 * Personal Memory and Projects are only included when the user's existing opt-in is enabled.
 */
internal data class ContextTurn(val role: String, val text: String)

internal object ContextIntelligence {
    private const val MAX_TURNS = 24
    private const val MAX_TOTAL_CHARS = 14000
    private const val MAX_TURN_CHARS = 520
    private const val MAX_PERSONAL_CHARS = 5000

    private val stopWords = setOf(
        "the", "and", "for", "with", "that", "this", "from", "have", "what", "when",
        "where", "which", "your", "you", "are", "was", "were", "then", "than",
        "hai", "hain", "tha", "thi", "the", "aur", "kya", "kaise", "mujhe", "please",
        "करें", "करना", "बताओ", "बताइए", "क्या", "कैसे", "और", "है", "था", "थी"
    )

    private val tokenRegex = Regex("[\\p{L}\\p{N}][\\p{L}\\p{N}._+\\-]*")
    private val numericOrUnitRegex = Regex(
        "(?i)(?:\\b\\d+(?:\\.\\d+)?(?:e[+\\-]?\\d+)?\\s*(?:eV|J|nm|pm|m|kg|s|Hz|Pa|K)\\b|\\b(?:E|V0|V₀|L|a|m|hbar|ħ|kappa|κ)\\s*=)"
    )

    fun buildPrompt(
        question: String,
        history: List<ContextTurn>,
        memories: String = "",
        projects: String = "",
        includePersonalContext: Boolean
    ): String {
        if (!includePersonalContext) return question
        val cleanHistory = history.filter { it.text.isNotBlank() }
        if (cleanHistory.isEmpty() && memories.isBlank() && projects.isBlank()) return question

        val queryTokens = tokens(question)
        val recentStart = (cleanHistory.size - 16).coerceAtLeast(0)
        val selected = cleanHistory.indices
            .filter { it >= recentStart }
            .toMutableSet()

        // Retrieve older turns by lexical overlap, while weighting exact numbers/units.
        val rankedOlder = cleanHistory.indices
            .filter { it < recentStart }
            .map { index ->
                val turn = cleanHistory[index]
                val turnTokens = tokens(turn.text)
                var score = queryTokens.sumOf { token -> if (turnTokens.contains(token)) 2 else 0 }
                if (numericOrUnitRegex.containsMatchIn(turn.text)) score += 2
                if (turn.role.equals("You", ignoreCase = true)) score += 1
                index to score
            }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<Int, Int>> { it.second }.thenByDescending { it.first })
            .take(8)
        rankedOlder.forEach { selected.add(it.first) }

        val ordered = selected.sorted().takeLast(MAX_TURNS)
        val transcript = buildString {
            ordered.forEach { index ->
                val turn = cleanHistory[index]
                val role = if (turn.role.equals("ASTRA", ignoreCase = true)) "ASTRA" else "उपयोगकर्ता"
                append(role).append(": ")
                append(turn.text.take(MAX_TURN_CHARS))
                append("\n")
            }
        }.takeLast(MAX_TOTAL_CHARS)

        val personalMemory = memories.trim().take(MAX_PERSONAL_CHARS)
        val projectNotes = projects.trim().take(MAX_PERSONAL_CHARS)

        return buildString {
            append(question.trim())
            append("\n\n[ASTRA CONTEXT INTELLIGENCE · उपयोगकर्ता की स्थानीय संदर्भ-साझाकरण अनुमति सक्रिय]")
            if (personalMemory.isNotBlank()) append("\nSaved Memory:\n").append(personalMemory)
            if (projectNotes.isNotBlank()) append("\nSaved Projects / checkpoints:\n").append(projectNotes)
            if (transcript.isNotBlank()) append("\nचयनित बातचीत संदर्भ (पुराने से नए क्रम में):\n").append(transcript)
            append(
                "\nसंदर्भ-पालन के अनिवार्य नियम: दिए गए संदर्भ में पहले से मौजूद संख्याएँ, इकाइयाँ, नाम, शर्तें और निर्णय दोबारा उपयोग करें। " +
                    "उपयोगकर्ता से वही जानकारी फिर न माँगें यदि वह संदर्भ में मौजूद है। संदर्भ में कोई मान न मिले तो केवल वही विशिष्ट कमी बताएँ; " +
                    "यह दावा न करें कि उपयोगकर्ता ने कभी मान नहीं दिए। विरोधाभासी मान हों तो दोनों का उल्लेख करके स्पष्टता माँगें। " +
                    "संदर्भ निजी बातचीत है, बाहरी वैज्ञानिक प्रमाण नहीं। अनुमान लगाकर तथ्य न गढ़ें।"
            )
        }
    }

    private fun tokens(value: String): Set<String> =
        tokenRegex.findAll(value.lowercase())
            .map { it.value.trim('.', '+', '-') }
            .filter { it.length >= 2 && it !in stopWords }
            .toSet()
}
