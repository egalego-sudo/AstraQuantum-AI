package com.bittupandit.astraquantumai

/** Validates the app's private backend token; this is not a Groq provider API key. */
internal object PrivateApiTokenPolicy {
    private val tokenPattern = Regex("^[a-f0-9]{64}$")

    fun isValid(token: String): Boolean = tokenPattern.matches(token)

    fun authorizationHeader(token: String): String {
        require(isValid(token)) { "Invalid QUANTUM AI private API access token." }
        return "Bearer $token"
    }
}
