# Kotlin Tuple Skill

[English](./README.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

Kotlin / Kotlin Multiplatform プロジェクトに型安全な Tuple ユーティリティを生成する [Claude Code](https://docs.anthropic.com/en/docs/claude-code) スキルです。

## クイックスタート

### 1. スキルをインストール:

```bash
npx skills add tbsten/skills \
  --skill kotlin-tuple
```

### 2. AI エージェントに依頼:

```
このプロジェクトに型安全な Tuple ユーティリティをデフォルト設定で追加して。
```

これだけで、スキルがプロジェクト構造を検出し、設定を確認した上で Tuple ファイルを生成します。

## 機能

最大 6 ファイルを自動生成します。

| ファイル | 内容 | 必須 |
|---|---|---|
| [`Tuple.kt`](skills/kotlin-tuple/example/src/commonMain/kotlin/com/example/tuple/Tuple.kt) | Tuple0〜TupleN のデータクラス定義 | 必須 |
| [`TupleFactory.kt`](skills/kotlin-tuple/example/src/commonMain/kotlin/com/example/tuple/TupleFactory.kt) | `tupleOf()` ファクトリ関数 (0〜N 引数) | 必須 |
| [`TupleToList.kt`](skills/kotlin-tuple/example/src/commonMain/kotlin/com/example/tuple/TupleToList.kt) | `toList()` 拡張関数 | 選択 |
| [`AbstractTupleSerializer.kt`](skills/kotlin-tuple/example/src/commonMain/kotlin/com/example/tuple/AbstractTupleSerializer.kt) + [`TupleSerializer.kt`](skills/kotlin-tuple/example/src/commonMain/kotlin/com/example/tuple/TupleSerializer.kt) | kotlinx.serialization 用 `KSerializer` 実装 | 選択 |
| [`AwaitAll.kt`](skills/kotlin-tuple/example/src/commonMain/kotlin/com/example/tuple/AwaitAll.kt) | 型安全な `awaitAll()` (Deferred 1〜N 個) | 選択 |
| [`AllNotNullOrNull.kt`](skills/kotlin-tuple/example/src/commonMain/kotlin/com/example/tuple/AllNotNullOrNull.kt) | `allNotNullOrNull()` トップレベル関数 + 拡張関数 | 選択 |

Tuple の最大サイズ (N)、生成先モジュール、どのオプションファイルを生成するかは、生成前に対話的に確認されます。

## 使い方

スキルをインストール後、Claude Code に以下のようなリクエストをすると自動的にスキルが発動します。

- 「Tuple を追加して」
- 「tupleOf を生成して」
- 「型安全な awaitAll が欲しい」
- 「複数の Deferred を型安全に await したい」
- 「nullable な値をまとめて non-null チェックしたい」

パッケージ名と出力先ディレクトリはプロジェクト構造から推測、または確認の上で生成されます。

### Tuple データクラスと `tupleOf()`

```kotlin
import com.example.tuple.*

// tupleOf() でタプルを生成
val empty = tupleOf()                          // Tuple0
val single = tupleOf(42)                       // Tuple1<Int>
val pair = tupleOf("hello", 3.14)              // Tuple2<String, Double> (= Pair)
val triple = tupleOf(1, "a", true)             // Tuple3<Int, String, Boolean> (= Triple)
val quad = tupleOf(1, "a", true, 3.14)         // Tuple4<Int, String, Boolean, Double>

// 分解宣言
val (name, age, active) = tupleOf("Alice", 30, true)

// プロパティ名でアクセス
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

返り値の型は `List<Base>` で、`Base` は全要素の共通スーパータイプです。

### `KSerializer` (kotlinx.serialization)

```kotlin
import com.example.tuple.*
import kotlinx.serialization.json.Json

// Tuple4 は JSON 配列としてシリアライズ: [1, "hello", true, 3.14]
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

> **Note**: `Tuple2` (= `Pair`) と `Tuple3` (= `Triple`) は kotlinx.serialization に組み込みのシリアライザがあるため、カスタムシリアライザは提供されません。

### `awaitAll()` (型安全)

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

`kotlinx.coroutines.awaitAll` は `List<T>` (共通型が必要) を返しますが、このオーバーロードは Tuple を返すことで各要素の型を保持します。

### `allNotNullOrNull()`

```kotlin
import com.example.tuple.*

val name: String? = "Alice"
val age: Int? = 30

// トップレベル関数
val result: Tuple2<String, Int>? = allNotNullOrNull(name, age)
// result: ("Alice", 30)

// nullable な Tuple の拡張関数
val result2: Tuple2<String, Int>? = tupleOf(name, age).allNotNullOrNull()

// いずれかの要素が null の場合
val missing: Tuple2<String, Int>? = allNotNullOrNull("Alice", null)
// missing: null
```

全要素が non-null なら non-nullable な Tuple を返し、いずれかが null なら `null` を返します。

## リポジトリ

このスキルは [TBSten/skills](https://github.com/TBSten/skills) の一部です。

```
skills/kotlin-tuple/
└── SKILL.md          ← `npx skills add` でインストールされるのはこのディレクトリのみ
```

`kotlin-tuple.md` などのリポジトリレベルのファイルはスキルのインストールには含まれません。
