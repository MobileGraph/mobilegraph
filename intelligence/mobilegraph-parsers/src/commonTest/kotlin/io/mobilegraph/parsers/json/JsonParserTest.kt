package io.mobilegraph.parsers.json

import io.mobilegraph.parsers.exceptions.MalformedContentException
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Serializable
data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val tags: List<String> = emptyList(),
)

@Serializable
data class Order(
    val orderId: Int,
    val products: List<Product>,
)

class JsonParserTest {
    private val productParser = JsonParser(serializer<Product>())
    private val orderParser = JsonParser(serializer<Order>())

    @Test
    fun testParseSimpleObject() {
        val json = """{"id": "p1", "name": "Phone", "price": 999.99}"""
        val product = productParser.parse(json)

        assertEquals("p1", product.id)
        assertEquals("Phone", product.name)
        assertEquals(999.99, product.price)
        assertEquals(emptyList(), product.tags)
    }

    @Test
    fun testParseNestedObjectAndCollections() {
        val json =
            """
            {
                "orderId": 123,
                "products": [
                    {"id": "p1", "name": "Phone", "price": 999.99, "tags": ["electronics", "mobile"]},
                    {"id": "p2", "name": "Case", "price": 19.99}
                ]
            }
            """.trimIndent()

        val order = orderParser.parse(json)

        assertEquals(123, order.orderId)
        assertEquals(2, order.products.size)
        assertEquals("Phone", order.products[0].name)
        assertEquals(listOf("electronics", "mobile"), order.products[0].tags)
        assertEquals("Case", order.products[1].name)
    }

    @Test
    fun testInvalidJson() {
        val json = """{"id": "p1", "name": "Phone", "price": "invalid"}"""
        assertFailsWith<MalformedContentException> {
            productParser.parse(json)
        }
    }

    @Test
    fun testMalformedJsonStructure() {
        val json = """{"id": "p1", "name": """
        assertFailsWith<MalformedContentException> {
            productParser.parse(json)
        }
    }

    @Test
    fun testIgnoreUnknownKeys() {
        val json = """{"id": "p1", "name": "Phone", "price": 999.99, "unknown": "value"}"""
        val product = productParser.parse(json)
        assertEquals("p1", product.id)
    }
}
