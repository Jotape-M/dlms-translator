package com.jotapem.dlmstranslator.services

object DlmsFrameNormalizer {

    sealed class Result {
        data class Success(val normalized: String) : Result()
        enum class ErrorType { INVALID_CONTENT, ODD_LENGTH }
        data class Error(val type: ErrorType) : Result()
    }

    private val TX_RX_PREFIX = Regex("(?im)^\\s*(TX|RX)\\s*:\\s*")
    private val HEX_PREFIX = Regex("(?i)0x")
    private val SEPARATOR = Regex("[\\s,;:\\-]+")
    private val HEX_TOKEN = Regex("[0-9A-Fa-f]+")

    fun normalize(input: String): Result {
        val stripped = TX_RX_PREFIX.replace(input, "")
        val noPrefixes = HEX_PREFIX.replace(stripped, "")
        val tokens = SEPARATOR.split(noPrefixes).filter { it.isNotEmpty() }

        if (tokens.isEmpty()) return Result.Error(Result.ErrorType.INVALID_CONTENT)

        for (token in tokens) {
            if (!HEX_TOKEN.matches(token)) {
                return Result.Error(Result.ErrorType.INVALID_CONTENT)
            }
        }

        val joined = tokens.joinToString("").uppercase()

        if (joined.isEmpty()) return Result.Error(Result.ErrorType.INVALID_CONTENT)
        if (joined.length % 2 != 0) return Result.Error(Result.ErrorType.ODD_LENGTH)

        return Result.Success(joined)
    }
}
