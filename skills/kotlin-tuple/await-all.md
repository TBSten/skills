# AwaitAll.kt Generation Rules

Requires `kotlinx-coroutines` dependency.

Await multiple `Deferred` values with type safety, returning a Tuple.

```kotlin
import kotlinx.coroutines.Deferred

suspend fun <A0> awaitAll(
    first: Deferred<A0>,
): Tuple1<A0> = tupleOf(first.await())

suspend fun <A0, A1> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
): Tuple2<A0, A1> = tupleOf(first.await(), second.await())

// ... up to TupleN
```
