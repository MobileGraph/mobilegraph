package io.mobilegraph.core.capability

/**
 * Represents a capability supported by a component (e.g., a Model).
 */
sealed interface Capability {
    val name: String

    data object Streaming : Capability {
        override val name = "Streaming"
    }

    data object FunctionCalling : Capability {
        override val name = "FunctionCalling"
    }

    data object StructuredOutput : Capability {
        override val name = "StructuredOutput"
    }

    data object Vision : Capability {
        override val name = "Vision"
    }

    data object Embedding : Capability {
        override val name = "Embedding"
    }

    data object SearchGrounding : Capability {
        override val name = "SearchGrounding"
    }

    data object Reasoning : Capability {
        override val name = "Reasoning"
    }
}
