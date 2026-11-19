package com.acmepay.core.flow

import com.acmepay.core.annotation.AcmeInternal
import com.acmepay.core.logging.AcmeLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen

/**
 * Retries the upstream flow with exponential backoff.
 * initialDelayMs * 2^attempt, capped at maxDelayMs.
 */
@AcmeInternal
fun <T> Flow<T>.retryWithBackoff(
    maxRetries: Int = 3,
    initialDelayMs: Long = 500,
    maxDelayMs: Long = 8_000,
    shouldRetry: (Throwable) -> Boolean = { it is java.io.IOException },
): Flow<T> = retryWhen { cause, attempt ->
    if (attempt >= maxRetries || !shouldRetry(cause)) return@retryWhen false
    val wait = (initialDelayMs shl attempt.toInt()).coerceAtMost(maxDelayMs)
    AcmeLogger.w("retryWithBackoff", "attempt=${attempt + 1} wait=${wait}ms cause=$cause")
    delay(wait)
    true
}
