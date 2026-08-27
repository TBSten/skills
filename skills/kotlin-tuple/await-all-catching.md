# AwaitAllCatching.kt Generation Rules

Requires `kotlinx-coroutines` dependency.

Run several blocks concurrently and capture each outcome in a `Result`, so that a failure in one
block neither cancels the others nor propagates out of the call.

## Why `suspend () -> T`, not `Deferred<T>`

`awaitAll` takes `Deferred` values, but `awaitAllCatching` deliberately does not. Under structured
concurrency a `Deferred` created by the caller's `async` cancels its parent scope as soon as it
fails, so wrapping `await()` in `runCatching` cannot save the sibling coroutines — the whole scope
is already dying. Creating the coroutines *inside* `awaitAllCatching` is what makes per-element
recovery possible.

`CancellationException` is always rethrown so that cancelling the surrounding coroutine still works;
that is what `runCatchingCancellable` (private to the file) exists for.

```kotlin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private suspend fun <T> runCatchingCancellable(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

suspend fun <A0, A1> awaitAllCatching(
    first: suspend () -> A0,
    second: suspend () -> A1,
): Tuple2<Result<A0>, Result<A1>> = coroutineScope {
    val firstDeferred = async { runCatchingCancellable(first) }
    val secondDeferred = async { runCatchingCancellable(second) }
    tupleOf(firstDeferred.await(), secondDeferred.await())
}

suspend fun <A0, A1> Tuple2<suspend () -> A0, suspend () -> A1>.awaitAllCatching(): Tuple2<Result<A0>, Result<A1>> =
    awaitAllCatching(first, second)

// ... up to TupleN
```

## Usage

```kotlin
val (name, age) = awaitAllCatching(
    { fetchName() },
    { fetchAge() },
)
name.onFailure { log(it) }
val resolvedAge = age.getOrDefault(0)
```

The Tuple receiver form needs `suspend {}` literals: a bare `{ ... }` passed to `tupleOf` has no
expected type and is inferred as a non-suspending function type.

```kotlin
val (name, age) = tupleOf(
    suspend { fetchName() },
    suspend { fetchAge() },
).awaitAllCatching()
```

Pairs with [tuple-result.md](./tuple-result.md), which collapses the returned `TupleN<Result<..>>`
back into a single value.
