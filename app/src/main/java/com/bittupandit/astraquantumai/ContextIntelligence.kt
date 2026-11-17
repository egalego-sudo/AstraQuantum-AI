package com.bittupandit.astraquantumai

/**
 * Builds bounded context from the current local chat on every turn.
 * Saved Personal Memory and Projects are included only when the user's opt-in is enabled.
 */
internal data class ContextTurn(val role: String, val text: String)

internal object ContextIntelligence {
    private const val MAX_TURNS = 12
    private const val MAX_TOTAL_CHARS = 5600
    private const val MAX_RECENT_TURN_CHARS = 1500
    private const val MAX_OLDER_TURN_CHARS = 320
    private const val MAX_PERSONAL_CHARS = 900

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
        val cleanHistory = history.filter { it.text.isNotBlank() }
        val personalMemory = if (includePersonalContext) memories.trim().take(MAX_PERSONAL_CHARS) else ""
        val projectNotes = if (includePersonalContext) projects.trim().take(MAX_PERSONAL_CHARS) else ""
        if (cleanHistory.isEmpty() && personalMemory.isBlank() && projectNotes.isBlank()) return question

        val queryTokens = tokens(question)
        // Reserve room for both recent continuity and older, relevant facts. Previously,
        // selecting 16 recent turns and then taking the last 12 silently discarded every
        // retrieved older turn. Keep at most 8 recent turns and fill the remaining slots
        // with the strongest older matches.
        val recentStart = (cleanHistory.size - 8).coerceAtLeast(0)
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
            .take((MAX_TURNS - selected.size).coerceAtLeast(0))
        rankedOlder.forEach { selected.add(it.first) }

        val ordered = selected.sorted().takeLast(MAX_TURNS)
        val lastHistoryIndex = cleanHistory.lastIndex
        val transcript = buildString {
            ordered.forEach { index ->
                val turn = cleanHistory[index]
                val role = if (turn.role.equals("ASTRA", ignoreCase = true)) "QUANTUM AI" else "उपयोगकर्ता"
                // Preserve the latest exchange in much greater detail so follow-ups can
                // resolve references to unfinished answers without asking the user again.
                val limit = if (index >= lastHistoryIndex - 1) MAX_RECENT_TURN_CHARS else MAX_OLDER_TURN_CHARS
                append(role).append(": ")
                append(turn.text.take(limit))
                append("\n")
            }
        }.takeLast(MAX_TOTAL_CHARS)

        return buildString {
            append(question.trim())
            append("\n\n[QUANTUM AI CONTEXT INTELLIGENCE · वर्तमान बातचीत का संदर्भ स्वतः शामिल]")
            if (personalMemory.isNotBlank()) append("\nSaved Memory:\n").append(personalMemory)
            if (projectNotes.isNotBlank()) append("\nSaved Projects / checkpoints:\n").append(projectNotes)
            if (transcript.isNotBlank()) append("\nचयनित बातचीत संदर्भ (पुराने से नए क्रम में):\n").append(transcript)
            append(
                "\nसंदर्भ-पालन के अनिवार्य नियम: दिए गए संदर्भ में पहले से मौजूद संख्याएँ, इकाइयाँ, नाम, शर्तें और निर्णय दोबारा उपयोग करें। " +
                    "उपयोगकर्ता से वही जानकारी फिर न माँगें यदि वह संदर्भ में मौजूद है। 'इसे जारी रखें', 'वह हिस्सा', 'किस प्रश्न की बात' जैसे अनुवर्ती वाक्य हों तो सबसे हाल के प्रासंगिक उपयोगकर्ता और QUANTUM AI संदेशों से संदर्भ पहचानें। " +
                    "यदि पिछला QUANTUM AI उत्तर अधूरा दिखता है, तो उसी बिंदु से आगे बढ़ें; उपयोगकर्ता से प्रश्न दोबारा न पूछें। " +
                    "संदर्भ में कोई मान न मिले तो केवल वही विशिष्ट कमी बताएँ; यह दावा न करें कि उपयोगकर्ता ने कभी मान नहीं दिए। विरोधाभासी मान हों तो दोनों का उल्लेख करके स्पष्टता माँगें। " +
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
