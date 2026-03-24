# AllNotNullOrNull.kt Generation Rules

Returns a Tuple if all arguments are non-null, or null if any is null.
Define 2 functions for each Tuple size:

1. **Top-level function**: `allNotNullOrNull(first, second, ...) → TupleN?`
2. **Extension function**: `TupleN<A0?, A1?, ...>.allNotNullOrNull() → TupleN<A0, A1, ...>?`

All extension functions use the `val x = x ?: return null` early-return pattern for consistency and readability.

```kotlin
// Tuple1
fun <A0 : Any> allNotNullOrNull(first: A0?): Tuple1<A0>? =
    tupleOf(first).allNotNullOrNull()

fun <A0 : Any> Tuple1<A0?>.allNotNullOrNull(): Tuple1<A0>? {
    val first = first ?: return null
    return tupleOf(first)
}

// Tuple2 (= Pair)
fun <A0 : Any, A1 : Any> Tuple2<A0?, A1?>.allNotNullOrNull(): Tuple2<A0, A1>? {
    val first = first ?: return null
    val second = second ?: return null
    return tupleOf(first, second)
}

// Tuple3 (= Triple)
fun <A0 : Any, A1 : Any, A2 : Any> Tuple3<A0?, A1?, A2?>.allNotNullOrNull(): Tuple3<A0, A1, A2>? {
    val first = first ?: return null
    val second = second ?: return null
    val third = third ?: return null
    return tupleOf(first, second, third)
}

// Tuple4 example
fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any> Tuple4<A0?, A1?, A2?, A3?>.allNotNullOrNull(): Tuple4<A0, A1, A2, A3>? {
    val first = first ?: return null
    val second = second ?: return null
    val third = third ?: return null
    val fourth = fourth ?: return null
    return tupleOf(first, second, third, fourth)
}

// ... up to TupleN (same pattern)
```
