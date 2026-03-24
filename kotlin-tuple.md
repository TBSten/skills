# Kotlin Tuple Skill

[日本語](./kotlin-tuple.ja.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

A [Claude Code](https://docs.anthropic.com/en/docs/claude-code) skill that generates type-safe Tuple utilities (Tuple0–Tuple20) for Kotlin and Kotlin Multiplatform projects.

## Quick Start

### 1. Install the skill:

```bash
npx skills add tbsten/skills --skill kotlin-tuple
```

### 2. Ask your AI agent:

```
Add type-safe Tuple utilities to this project with default settings.
```

That's it! The skill will detect your project structure, confirm settings, and generate all Tuple files.

## Features

This skill auto-generates up to 6 files:

| File | Description | Required |
|---|---|---|
| [`Tuple.kt`](skills/kotlin-tuple/example/src/commonMain/kotlin/com/example/tuple/Tuple.kt) | Data class definitions for Tuple0–TupleN | Required |
| [`TupleFactory.kt`](skills/kotlin-tuple/example/src/commonMain/kotlin/com/example/tuple/TupleFactory.kt) | `tupleOf()` factory functions (0–N arguments) | Required |
| [`TupleToList.kt`](skills/kotlin-tuple/example/src/commonMain/kotlin/com/example/tuple/TupleToList.kt) | `toList()` extension functions | Optional |
| [`AbstractTupleSerializer.kt`](skills/kotlin-tuple/example/src/commonMain/kotlin/com/example/tuple/AbstractTupleSerializer.kt) + [`TupleSerializer.kt`](skills/kotlin-tuple/example/src/commonMain/kotlin/com/example/tuple/TupleSerializer.kt) | `KSerializer` implementations for kotlinx.serialization | Optional |
| [`AwaitAll.kt`](skills/kotlin-tuple/example/src/commonMain/kotlin/com/example/tuple/AwaitAll.kt) | Type-safe `awaitAll()` for 1–N `Deferred` values | Optional |
| [`AllNotNullOrNull.kt`](skills/kotlin-tuple/example/src/commonMain/kotlin/com/example/tuple/AllNotNullOrNull.kt) | `allNotNullOrNull()` top-level and extension functions | Optional |

The max Tuple size (N), target module, and which optional files to generate are all confirmed interactively before generation.

## Usage

After installing the skill, Claude Code will automatically activate it when you make requests like:

- "Add Tuple utilities"
- "Generate tupleOf"
- "I need a type-safe awaitAll"
- "Await multiple Deferred values with type safety"
- "Check multiple nullable values for non-null at once"

The package name and output directory will be inferred from your project structure or confirmed interactively.

### Tuple data classes & `tupleOf()`

```kotlin
import com.example.tuple.*

// Create tuples with tupleOf()
val empty = tupleOf()                          // Tuple0
val single = tupleOf(42)                       // Tuple1<Int>
val pair = tupleOf("hello", 3.14)              // Tuple2<String, Double> (= Pair)
val triple = tupleOf(1, "a", true)             // Tuple3<Int, String, Boolean> (= Triple)
val quad = tupleOf(1, "a", true, 3.14)         // Tuple4<Int, String, Boolean, Double>

// Destructuring
val (name, age, active) = tupleOf("Alice", 30, true)

// Access by property name
val t = tupleOf("x", 1, false, 2.0)
println(t.first)   // "x"
println(t.second)  // 1
println(t.fourth)  // 2.0
```

### `toList()`

```kotlin
import com.example.tuple.*

val list: List<Int> = tupleOf(1, 2, 3).toList()          // [1, 2, 3]
val mixed: List<Any> = tupleOf(1, "a", true).toList()    // [1, "a", true]
```

The return type is `List<Base>` where `Base` is the common supertype of all elements.

### `KSerializer` (kotlinx.serialization)

```kotlin
import com.example.tuple.*
import kotlinx.serialization.json.Json

// Tuple4 serializes as a JSON array: [1, "hello", true, 3.14]
val serializer = Tuple4Serializer(
    Int.serializer(),
    String.serializer(),
    Boolean.serializer(),
    Double.serializer(),
)
val json = Json.encodeToString(serializer, tupleOf(1, "hello", true, 3.14))
// json: [1,"hello",true,3.14]

val decoded = Json.decodeFromString(serializer, json)
// decoded: (1, hello, true, 3.14)
```

> **Note**: `Tuple2` (= `Pair`) and `Tuple3` (= `Triple`) already have built-in serializers in kotlinx.serialization, so no custom serializer is provided for them.

### `awaitAll()` (type-safe)

```kotlin
import com.example.tuple.*
import kotlinx.coroutines.*

coroutineScope {
    val (name, age, active) = awaitAll(
        async { fetchName() },    // Deferred<String>
        async { fetchAge() },     // Deferred<Int>
        async { fetchActive() },  // Deferred<Boolean>
    )
    // name: String, age: Int, active: Boolean
}
```

Unlike `kotlinx.coroutines.awaitAll` which returns `List<T>` (requiring a common type), these overloads preserve each element's distinct type by returning a Tuple.

### `allNotNullOrNull()`

```kotlin
import com.example.tuple.*

val name: String? = "Alice"
val age: Int? = 30

// Top-level function
val result: Tuple2<String, Int>? = allNotNullOrNull(name, age)
// result: ("Alice", 30)

// Extension function on nullable Tuple
val result2: Tuple2<String, Int>? = tupleOf(name, age).allNotNullOrNull()

// When any element is null
val missing: Tuple2<String, Int>? = allNotNullOrNull("Alice", null)
// missing: null
```

Returns a non-nullable Tuple if all elements are non-null, or `null` if any element is null.

## Repository

This skill is part of [TBSten/skills](https://github.com/TBSten/skills).

```
skills/kotlin-tuple/
└── SKILL.md          ← Only this directory is installed by `npx skills add`
```

`kotlin-tuple.md` and other repo-level files are not included when installing the skill.
