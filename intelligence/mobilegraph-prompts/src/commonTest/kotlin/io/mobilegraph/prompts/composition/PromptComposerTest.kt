/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.prompts.composition

import io.mobilegraph.models.HumanMessage
import io.mobilegraph.models.SystemMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PromptComposerTest {
    @Test
    fun testBasicComposition() {
        val prompt =
            promptComposer {
                system("You are a bot.")
                human("Hello")
            }.compose()

        assertEquals(2, prompt.messages.size)
        assertTrue(prompt.messages[0] is SystemMessage)
        assertEquals("You are a bot.", prompt.messages[0].content)
        assertTrue(prompt.messages[1] is HumanMessage)
        assertEquals("Hello", prompt.messages[1].content)
    }

    @Test
    fun testTokenBudget() {
        val composer =
            promptComposer {
                tokenBudget = TokenBudget(total = 2000)
            }

        assertNotNull(composer)
    }
}
