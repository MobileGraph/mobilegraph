# Skills System (Declarative AI)

The **Skills System** in MobileGraph allows you to define complex AI capabilities using Markdown files. This separates "Expertise" (instructions) from "Implementation" (Kotlin code).

---

## 1. Defining a Skill (`skill.md`)

A skill file defines the identity, instructions, and required tools for a specific domain.

```markdown
# Travel Management
A skill for searching flights, booking hotels, and managing itineraries.

## Instructions
- Always confirm the user's destination and dates before searching.
- If no direct flights are found, suggest one alternative with a layover.
- Use a friendly, professional tone.

## Tools
- `search_flights`: Fetches available flights.
- `book_hotel`: Reserves a room at the selected location.
```

---

## 2. Loading a Skill

You can load a skill from any string source (Assets, URL, Local File) and bind it to Kotlin tool implementations.

```kotlin
val travelTools = listOf(FlightSearchTool(), HotelBookingTool())
val markdownContent = context.assets.open("skills/travel.md").bufferedReader().use { it.readText() }

val result = SkillLoader.fromMarkdown(
    content = markdownContent,
    tools = travelTools
)

// Check for warnings (e.g. Markdown mentions a tool you didn't provide)
result.warnings.forEach { println("Skill Warning: $it") }

val travelSkill = result.skill
```

### Metadata Priority
- **Name**: Extracted from the `# H1` header in Markdown.
- **Description**: Extracted from the text immediately following the H1 header.

---

## 3. Equipping an Agent with Skills

Skills are attached to Agents. When the agent runs, its system prompt is automatically augmented with the skill's instructions, and its registry is expanded with the skill's tools.

```kotlin
class TravelAgent(
    override val model: ChatModel,
    override val skills: List<Skill>
) : Agent {
    override val name = "Travel Specialist"
    override val rolePrompt = SystemMessage("You are a helpful travel assistant.")
    // ...
}
```

### Prompt Merging
The `AgentNode` automatically formats the prompt like this:
```text
You are a helpful travel assistant.

=== ACTIVE SKILL: Travel Management ===
Description: A skill for searching flights...
- Always confirm the user's destination...
====================================
```

---

## 4. Key Benefits

- **Markdown-First**: Prompt engineers can update behavior in `.md` files without touching Kotlin code.
- **Explicit Binding**: Tools are scoped to the skill, preventing global registry pollution.
- **Lenient Validation**: The SDK warns you if your Markdown and Kotlin code are out of sync but stays functional.
