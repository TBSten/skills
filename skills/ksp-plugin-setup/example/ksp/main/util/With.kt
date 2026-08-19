package com.example.ksppluginsetup.ksp.util

/**
 * Calls [block] with [a] and [b] as context parameters, avoiding nested `with` calls at the
 * feature → core boundary (where a feature holds a whole `ProcessContext` but core wants only two
 * of its capabilities).
 */
@Suppress("NOTHING_TO_INLINE")
internal inline fun <A, B, R> with(
    a: A,
    b: B,
    block: context(A, B) () -> R,
): R = with(a) { with(b) { block() } }
