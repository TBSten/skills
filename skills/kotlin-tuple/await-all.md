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

Every overload also has a Tuple receiver form, so a Tuple of `Deferred` can be awaited directly:

```kotlin
suspend fun <A0, A1> Tuple2<Deferred<A0>, Deferred<A1>>.awaitAll(): Tuple2<A0, A1> = awaitAll(first, second)

// ... up to TupleN
```

```kotlin
val (name, age, active) = tupleOf(
    async { fetchName() },
    async { fetchAge() },
    async { fetchActive() },
).awaitAll()
```

Note that `awaitAll` is fail-fast: if one `Deferred` fails, structured concurrency cancels the
surrounding scope and the exception is rethrown. To keep going and inspect each outcome
individually, use `awaitAllCatching` ([await-all-catching.md](./await-all-catching.md)) instead.
