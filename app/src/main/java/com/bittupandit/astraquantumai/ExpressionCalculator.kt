package com.bittupandit.astraquantumai

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.pow

/** A bounded arithmetic parser. It never evaluates Kotlin, JavaScript, or arbitrary code. */
object ExpressionCalculator {
    data class Result(val expression: String, val value: Double, val displayValue: String)

    fun evaluate(expression: String): Result {
        val input = expression.trim()
        require(input.isNotEmpty()) { "गणितीय expression लिखें।" }
        require(input.length <= 256) { "Expression 256 अक्षरों से छोटा रखें।" }
        val parser = Parser(input)
        val value = parser.parse()
        require(value.isFinite()) { "परिणाम सीमित संख्या नहीं है।" }
        val rounded = BigDecimal.valueOf(value).round(MathContext(12, RoundingMode.HALF_UP)).stripTrailingZeros()
        return Result(input, value, rounded.toPlainString())
    }

    private class Parser(private val input: String) {
        private var index = 0
        private var operations = 0

        fun parse(): Double {
            val value = expression()
            skipSpaces()
            require(index == input.length) { "असमर्थित चिन्ह या अतिरिक्त पाठ: ${input.substring(index).take(20)}" }
            return value
        }

        private fun expression(): Double {
            var value = term()
            while (true) {
                skipSpaces()
                value = when {
                    consume('+') -> checked(value + term())
                    consume('-') -> checked(value - term())
                    else -> return value
                }
                tick()
            }
        }

        private fun term(): Double {
            var value = unary()
            while (true) {
                skipSpaces()
                value = when {
                    consume('*') -> checked(value * unary())
                    consume('/') -> {
                        val divisor = unary()
                        require(divisor != 0.0) { "शून्य से भाग नहीं दिया जा सकता।" }
                        checked(value / divisor)
                    }
                    consume('%') -> {
                        val divisor = unary()
                        require(divisor != 0.0) { "शून्य से remainder नहीं निकाला जा सकता।" }
                        checked(value % divisor)
                    }
                    else -> return value
                }
                tick()
            }
        }

        private fun unary(): Double {
            skipSpaces()
            return when {
                consume('+') -> unary()
                consume('-') -> checked(-unary())
                else -> power()
            }
        }

        private fun power(): Double {
            val base = primary()
            skipSpaces()
            if (!consume('^')) return base
            tick()
            val exponent = unary()
            require(exponent.isFinite() && kotlin.math.abs(exponent) <= 100.0) {
                "घात का absolute मान 100 से अधिक नहीं हो सकता।"
            }
            return checked(base.pow(exponent))
        }

        private fun primary(): Double {
            skipSpaces()
            if (consume('(')) {
                val value = expression()
                skipSpaces()
                require(consume(')')) { "कोष्ठक ')' बंद करना बाकी है।" }
                return value
            }
            val start = index
            var digits = 0
            while (index < input.length && input[index].isDigit()) { index++; digits++ }
            if (index < input.length && input[index] == '.') {
                index++
                while (index < input.length && input[index].isDigit()) { index++; digits++ }
            }
            require(digits > 0) { "अंक या '(' अपेक्षित था।" }
            if (index < input.length && (input[index] == 'e' || input[index] == 'E')) {
                val exponentStart = index++
                if (index < input.length && (input[index] == '+' || input[index] == '-')) index++
                val exponentDigits = index
                while (index < input.length && input[index].isDigit()) index++
                require(index > exponentDigits) { "वैज्ञानिक notation में exponent अधूरा है।" }
                require(index - exponentStart <= 5) { "वैज्ञानिक notation का exponent बहुत बड़ा है।" }
            }
            val number = input.substring(start, index).toDoubleOrNull()
                ?: throw IllegalArgumentException("संख्या पढ़ी नहीं जा सकी।")
            return checked(number)
        }

        private fun consume(char: Char): Boolean {
            if (index < input.length && input[index] == char) { index++; return true }
            return false
        }

        private fun skipSpaces() {
            while (index < input.length && input[index].isWhitespace()) index++
        }

        private fun tick() {
            operations++
            require(operations <= 100) { "Expression में 100 से अधिक गणितीय operations नहीं हो सकते।" }
        }

        private fun checked(value: Double): Double {
            require(value.isFinite()) { "गणना overflow या invalid operation के कारण संभव नहीं है।" }
            return value
        }
    }
}
