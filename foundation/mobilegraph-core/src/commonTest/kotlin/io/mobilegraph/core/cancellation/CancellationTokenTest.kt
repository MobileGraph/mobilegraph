package io.mobilegraph.core.cancellation

import io.mobilegraph.core.exceptions.CancellationException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class CancellationTokenTest {
    @Test
    fun testCancellationTokenNone() {
        val token = CancellationToken.None
        assertFalse(token.isCancelled)
        token.throwIfCancelled() // Should not throw
    }

    @Test
    fun testThrowIfCancelled() {
        val cancelledToken =
            object : CancellationToken {
                override val isCancelled: Boolean = true
            }
        assertFailsWith<CancellationException> {
            cancelledToken.throwIfCancelled()
        }
    }
}
