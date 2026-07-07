/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.models

import kotlin.test.Test
import kotlin.test.assertEquals

class ChatMessageTest {
    @Test
    fun testHumanMessage() {
        val msg = UserMessage("hello", id = "id1", sessionId = "s1")
        assertEquals("hello", msg.content)
        assertEquals("id1", msg.id)
        assertEquals("s1", msg.sessionId)
        assertEquals(SyncState.PENDING, msg.syncState)

        val copied = msg.copyWith(content = "hi")
        assertEquals("hi", copied.content)
        assertEquals("id1", copied.id)
    }

    @Test
    fun testAssistantMessage() {
        val msg = AssistantMessage("resp", toolCallId = "t1", id = "id2", sessionId = "s2")
        assertEquals("resp", msg.content)
        assertEquals("t1", msg.toolCallId)
        assertEquals("id2", msg.id)

        val legacy = msg.toMessage()
        assertEquals(Role.Assistant, legacy.role)
    }

    @Test
    fun testToolMessage() {
        val msg = ToolMessage("result", toolCallId = "t1", id = "id3")
        assertEquals("result", msg.content)
        assertEquals("t1", msg.toolCallId)
    }

    @Test
    fun testLegacyConversion() {
        val legacy = Message(Role.User, "test")
        val modern = ChatMessage.fromMessage(legacy)

        assertEquals("test", modern.content)
        assertEquals("global", modern.sessionId)
    }
}
