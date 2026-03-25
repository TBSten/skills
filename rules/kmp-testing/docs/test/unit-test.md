# ユニットテスト

## 概要

ビジネスロジック（Loader、UseCase 等）の状態遷移・振る舞いを検証するテスト。
`commonTest` ソースセットに配置し、Kotlin Multiplatform の全ターゲットで実行可能。

## フレームワーク

- **Kotest FreeSpec** — DSL スタイルのテスト構文
- **Turbine** — `Flow.test {}` で非同期 Flow の値遷移を検証
- **kotlinx-coroutines-test** — `TestDispatcher` によるコルーチン制御

## テストの書き方

### 基本構造

```kt
class SomeLoaderTest : FreeSpec({
    "valid scenario" - {
        "initialLoad -> success" {
            // Arrange
            val loader = createLoader(load = { "result" })

            // Act & Assert (Turbine)
            loader.state.test {
                awaitItem().shouldBeInstanceOf<State.Initial>()
                loader.initialLoad()
                awaitItem().shouldBeInstanceOf<State.InitialLoading>()
                awaitItem().shouldBeInstanceOf<State.Loaded<*>>()
            }
        }
    }

    "error scenario" - {
        "initialLoad -> error" {
            val loader = createLoader(load = { throw RuntimeException("fail") })

            loader.state.test {
                awaitItem().shouldBeInstanceOf<State.Initial>()
                loader.initialLoad()
                awaitItem().shouldBeInstanceOf<State.InitialLoading>()
                awaitItem().shouldBeInstanceOf<State.InitialError>()
            }
        }
    }
})
```

### ポイント

- テストケースは `"カテゴリ" - { "ケース名" { ... } }` の階層構造で整理する
- `Flow.test {}` で `awaitItem()` を使い、状態遷移を順番に検証する
- 正常系・エラー系・不正遷移（例: `Initial` で `refresh()` を呼ぶ）を網羅する

## テスト対象

| 対象 | 配置場所 | 例 |
|------|---------|-----|
| SimpleLoader | `ui/core/src/commonTest/` | `SimpleLoaderImplTest` |
| InfiniteLoader | `ui/core/src/commonTest/` | `InfiniteLoaderImplTest` |
| UseCase | `domain/*/src/commonTest/` | (必要に応じて作成) |

## 実行コマンド

```bash
./gradlew jvmTest
```
