package io.mobilegraph.core.metadata

import kotlin.test.Test
import kotlin.test.assertEquals

class MetadataTest {
    @Test
    fun testMetadata() {
        val metadata = Metadata(mapOf("key" to "value"))
        assertEquals("value", metadata["key"])

        val newMetadata = metadata.plus("key2", 42)
        assertEquals("value", newMetadata["key"])
        assertEquals(42, newMetadata["key2"])
    }
}
