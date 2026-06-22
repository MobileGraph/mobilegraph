package io.mobilegraph.tools.selection

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JsonEmbeddingStoreTest {
    @Test
    fun testStoreAndRetrieve() {
        var savedData: String? = null
        val store = JsonEmbeddingStore { savedData = it }

        val embedding = listOf(0.1f, 0.2f, 0.3f)
        store.put("key1", embedding)

        assertEquals(embedding, store.get("key1"))
        assertNotNull(savedData)

        // Verify JSON content
        val decoded = Json.decodeFromString<Map<String, List<Float>>>(savedData)
        assertEquals(embedding, decoded["key1"])
    }

    @Test
    fun testInitialData() {
        val initialData = "{\"key1\": [0.1, 0.2, 0.3]}"
        val store = JsonEmbeddingStore(initialData) { }

        assertEquals(listOf(0.1f, 0.2f, 0.3f), store.get("key1"))
    }

    @Test
    fun testMalformedInitialData() {
        val malformedData = "invalid json"
        val store = JsonEmbeddingStore(malformedData) { }

        assertNull(store.get("any"))
        assertEquals(0, store.getAll().size)
    }

    @Test
    fun testGetAll() {
        val store = JsonEmbeddingStore { }
        store.put("k1", listOf(1f))
        store.put("k2", listOf(2f))

        val all = store.getAll()
        assertEquals(2, all.size)
        assertEquals(listOf(1f), all["k1"])
        assertEquals(listOf(2f), all["k2"])
    }
}
