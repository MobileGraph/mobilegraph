package io.mobilegraph.skills.parser

/**
 * A lightweight parser that extracts skill components from a Markdown string.
 */
internal class MarkdownSkillParser {
    data class ParsedMarkdown(
        val name: String?,
        val description: String?,
        val instructions: String,
        val declaredTools: List<String>,
    )

    fun parse(content: String): ParsedMarkdown {
        val lines = content.lines()
        var name: String? = null
        var description: String? = null
        val instructionsBuilder = StringBuilder()
        val declaredTools = mutableListOf<String>()

        var currentSection: String? = null
        var foundH1 = false

        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.isEmpty()) {
                if (currentSection == "instructions") instructionsBuilder.append("\n")
                continue
            }

            when {
                line.startsWith("# ") && !foundH1 -> {
                    name = line.substring(2).trim()
                    foundH1 = true
                    currentSection = "metadata"
                }

                line.startsWith("## Instructions") || line.startsWith("## Rules") -> {
                    currentSection = "instructions"
                }

                line.startsWith("## Tools") || line.startsWith("## Capabilities") -> {
                    currentSection = "tools"
                }

                line.startsWith("## ") -> {
                    currentSection = "other"
                }

                else -> {
                    when (currentSection) {
                        "metadata" -> {
                            if (description == null) {
                                description = line
                            } else {
                                description += "\n$line"
                            }
                        }

                        "instructions" -> {
                            instructionsBuilder.append(line).append("\n")
                        }

                        "tools" -> {
                            // Extract tool name from backticks or list items
                            // Pattern: - `tool_name` or * tool_name
                            val toolName = extractToolName(line)
                            if (toolName != null) {
                                declaredTools.add(toolName)
                            }
                        }
                    }
                }
            }
        }

        return ParsedMarkdown(
            name = name,
            description = description?.trim(),
            instructions = instructionsBuilder.toString().trim(),
            declaredTools = declaredTools,
        )
    }

    private fun extractToolName(line: String): String? {
        // Look for content inside backticks first
        val backtickRegex = "`([^`]+)`".toRegex()
        val match = backtickRegex.find(line)
        if (match != null) {
            return match.groupValues[1]
        }

        // Fallback: extract first word after a list bullet
        val listRegex = "^[\\-*]\\s+([a-zA-Z0-9_]+)".toRegex()
        val listMatch = listRegex.find(line)
        return listMatch?.groupValues[1]
    }
}
