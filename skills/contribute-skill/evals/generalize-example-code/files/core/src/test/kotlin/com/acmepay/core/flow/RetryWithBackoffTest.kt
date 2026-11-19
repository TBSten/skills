package com.acmepay.core.flow

import com.acmepay.core.testing.AcmeTestRule
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RetryWithBackoffTest {
    @Test
    fun retriesIoExceptionThenSucceeds() = runTest {
        var calls = 0
        val result = flow {
            calls++
            if (calls < 3) throw java.io.IOException("boom")
            emit("ok")
        }.retryWithBackoff(maxRetries = 3).toList()
        assertEquals(listOf("ok"), result)
        assertEquals(3, calls)
    }
}
