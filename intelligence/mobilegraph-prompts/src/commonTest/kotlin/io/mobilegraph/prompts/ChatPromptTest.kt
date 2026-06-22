package io.mobilegraph.prompts

import io.mobilegraph.prompts.chat.ChatPrompt
import io.mobilegraph.prompts.chat.ChatRole
import io.mobilegraph.prompts.variables.Variables
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatPromptTest {
    @Test
    fun testChatPromptRendering() {
        val prompt =
            ChatPrompt
                .system("You are a {role}.")
                .append(ChatPrompt.user("Tell me about {topic}."))

        val variables =
            Variables.Empty
                .with("role", "helpful assistant")
                .with("topic", "KMP")

        val rendered = prompt.render(variables)

        assertEquals(2, rendered.size)
        assertEquals(ChatRole.System, rendered[0].role)
        assertEquals("You are a helpful assistant.", rendered[0].content)
        assertEquals(ChatRole.User, rendered[1].role)
        assertEquals("Tell me about KMP.", rendered[1].content)
    }

    @Test
    fun testComposition() {
        val system = ChatPrompt.system("System")
        val user = ChatPrompt.user("User")
        val assistant = ChatPrompt.assistant("Assistant")

        val combined = system.append(user).append(assistant)

        assertEquals(3, combined.messages.size)
        assertEquals(ChatRole.System, combined.messages[0].role)
        assertEquals(ChatRole.User, combined.messages[1].role)
        assertEquals(ChatRole.Assistant, combined.messages[2].role)
    }
}
