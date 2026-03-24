# TupleToList.kt Generation Rules

Converts a Tuple to a `List<Base>` where `Base` is a common supertype of all elements.
Uses a `Base` type parameter with upper-bound constraints on each element type (`A0 : Base, A1 : Base, ...`), so the returned list is type-safe.

- Tuple0 returns `emptyList<Nothing>()` (compatible with any `List<T>`)
- Tuple2/Tuple3 are typealiases for Pair/Triple — use `first/second/third` to access properties (Pair already has `toList()` in stdlib, but we define our own for type-safe base-typed return)

```kotlin
// Tuple0
fun Tuple0.toList(): List<Nothing> = emptyList()

// Tuple1
fun <Base, A0 : Base> Tuple1<A0>.toList(): List<Base> = listOf(first)

// Tuple2 (= Pair)
fun <Base, A0 : Base, A1 : Base> Tuple2<A0, A1>.toList(): List<Base> = listOf(first, second)

// Tuple3 (= Triple)
fun <Base, A0 : Base, A1 : Base, A2 : Base> Tuple3<A0, A1, A2>.toList(): List<Base> = listOf(first, second, third)

// Tuple4
fun <Base, A0 : Base, A1 : Base, A2 : Base, A3 : Base> Tuple4<A0, A1, A2, A3>.toList(): List<Base> = listOf(first, second, third, fourth)

// ... up to TupleN
```
