# TupleResult.kt Generation Rules

No extra dependency (stdlib `Result` only).

Collapse a Tuple of `Result` values into a single value. Designed to pair with `awaitAllCatching`
([await-all-catching.md](./await-all-catching.md)), but works with any Tuple of `Result` — for
example one built from `runCatching`.

`getOrElse { return ... }` is used rather than `getOrNull()` so that a *successful* `null` value is
preserved: only `Result.isFailure` decides whether the whole Tuple collapses.

```kotlin
fun <A0, A1> Tuple2<Result<A0>, Result<A1>>.allSuccessOrNull(): Tuple2<A0, A1>? {
    return tupleOf(
        first.getOrElse { return null },
        second.getOrElse { return null },
    )
}

fun <A0, A1> Tuple2<Result<A0>, Result<A1>>.allSuccessOrFailure(): Result<Tuple2<A0, A1>> {
    return Result.success(
        tupleOf(
            first.getOrElse { return Result.failure(it) },
            second.getOrElse { return Result.failure(it) },
        ),
    )
}

// ... up to TupleN
```

## Usage

```kotlin
val results = awaitAllCatching({ fetchName() }, { fetchAge() })

val values: Tuple2<String, Int>? = results.allSuccessOrNull()
val single: Result<Tuple2<String, Int>> = results.allSuccessOrFailure()  // first failure wins
```

`allSuccessOrNull()` is the `Result` counterpart of `allNotNullOrNull()`
([all-not-null-or-null.md](./all-not-null-or-null.md)).
