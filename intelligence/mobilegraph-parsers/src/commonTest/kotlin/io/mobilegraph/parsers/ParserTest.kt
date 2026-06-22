package io.mobilegraph.parsers

import io.mobilegraph.models.AssistantMessage
import io.mobilegraph.models.ModelOutput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParserTest {
    @Test
    fun testParserDsl() {
        val customParser =
            parser { output ->
                val text = output.asText()
                if (text.contains("OK")) {
                    ParseResult.Success(true)
                } else {
                    ParseResult.Failure(ParseError("Not OK"))
                }
            }

        val successOutput = ModelOutput.ChatOutput(AssistantMessage("Everything is OK"))
        val failureOutput = ModelOutput.ChatOutput(AssistantMessage("Something went wrong"))

        assertTrue(customParser.parse(successOutput) is ParseResult.Success)
        assertEquals(true, (customParser.parse(successOutput) as ParseResult.Success).value)

        assertTrue(customParser.parse(failureOutput) is ParseResult.Failure)
        assertEquals("Not OK", (customParser.parse(failureOutput) as ParseResult.Failure).error.message)
    }

    @Test
    fun testParseResultVariants() {
        val success = ParseResult.Success("data")
        assertEquals("data", success.value)

        val error = ParseError("err", path = "field1")
        val failure = ParseResult.Failure(error)
        assertEquals("err", failure.error.message)
        assertEquals("field1", failure.error.path)

        val partial = ParseResult.Partial("partial", listOf(error))
        assertEquals("partial", partial.partialValue)
        assertEquals(1, partial.errors.size)
    }
}
