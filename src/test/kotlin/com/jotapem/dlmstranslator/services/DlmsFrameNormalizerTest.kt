package com.jotapem.dlmstranslator.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DlmsFrameNormalizerTest {

    private fun assertSuccess(input: String, expected: String) {
        val result = DlmsFrameNormalizer.normalize(input)
        assertTrue("Expected Success for input: [$input], got: $result", result is DlmsFrameNormalizer.Result.Success)
        assertEquals(expected, (result as DlmsFrameNormalizer.Result.Success).normalized)
    }

    private fun assertError(input: String, expectedType: DlmsFrameNormalizer.Result.ErrorType) {
        val result = DlmsFrameNormalizer.normalize(input)
        assertTrue("Expected Error for input: [$input], got: $result", result is DlmsFrameNormalizer.Result.Error)
        assertEquals(expectedType, (result as DlmsFrameNormalizer.Result.Error).type)
    }

    @Test fun `space-separated hex`() = assertSuccess("7E A0 1E 03 21 93 7E", "7EA01E0321937E")
    @Test fun `TX prefix`() = assertSuccess("TX: 7E A0 1E 03 21 93 7E", "7EA01E0321937E")
    @Test fun `RX prefix`() = assertSuccess("RX: 7E A0 1E 03 21 93 7E", "7EA01E0321937E")
    @Test fun `lowercase tx prefix`() = assertSuccess("tx: 7E A0 1E 03 21 93 7E", "7EA01E0321937E")
    @Test fun `hyphen-separated hex`() = assertSuccess("7E-A0-1E-03-21-93-7E", "7EA01E0321937E")
    @Test fun `0x prefixes`() = assertSuccess("0x7E 0xA0 0x1E 0x03 0x21 0x93 0x7E", "7EA01E0321937E")
    @Test fun `colon-separated hex`() = assertSuccess("7E:A0:1E", "7EA01E")
    @Test fun `lowercase hex converted to uppercase`() = assertSuccess("7e a0 1e 03", "7EA01E03")
    @Test fun `multiline input`() = assertSuccess("7E A0\n1E 03\n21 93 7E", "7EA01E0321937E")
    @Test fun `comma-separated hex`() = assertSuccess("7E,A0,1E,03", "7EA01E03")
    @Test fun `empty string`() = assertError("", DlmsFrameNormalizer.Result.ErrorType.INVALID_CONTENT)
    @Test fun `blank whitespace only`() = assertError("   \n\t  ", DlmsFrameNormalizer.Result.ErrorType.INVALID_CONTENT)
    @Test fun `odd hex length`() = assertError("7E A0 F", DlmsFrameNormalizer.Result.ErrorType.ODD_LENGTH)
    @Test fun `non-hex word`() = assertError("hello world", DlmsFrameNormalizer.Result.ErrorType.INVALID_CONTENT)
    @Test fun `mixed valid hex and invalid word`() = assertError("7E INVALID 03", DlmsFrameNormalizer.Result.ErrorType.INVALID_CONTENT)
    @Test fun `TX prefix with valid hex multiline`() {
        val input = "TX: 7E A0 1E 03\nRX: 21 93 7E 00"
        assertSuccess(input, "7EA01E032193" + "7E00")
    }
}
