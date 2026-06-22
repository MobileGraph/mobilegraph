package io.mobilegraph.prompts.composition

import io.mobilegraph.prompts.chat.ChatPrompt
import io.mobilegraph.prompts.templates.PromptTemplate

/**
 * Extension functions for composing prompts.
 */

fun PromptTemplate.plus(other: String): PromptTemplate = this.append(other)

fun PromptTemplate.plus(other: PromptTemplate): PromptTemplate = this.append(other)

fun ChatPrompt.plus(other: ChatPrompt): ChatPrompt = this.append(other)
