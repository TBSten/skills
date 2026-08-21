# スナップショットテスト

## 概要

値・StateHolder・ViewModel の状態を Kotlin Code 形式でスナップショットとして記録し、
回帰テストするテスト。`jvmSnapshotTest` ソースセットに配置する。

## テストの種類

### 1. 値スナップショットテスト (shouldMatchSnapshot)

データモデルや UseCase の出力を Kotlin Code 形式でスナップショットする。

```kt
class GetItemListUseCaseSnapshotTest : FreeSpec({
    "GetItemListUseCaseImpl" - {
        "invoke returns default items" {
            val useCase = GetItemListUseCaseImpl()
            val result = useCase()
            shouldMatchSnapshot("result", result, ListSerializer(String.serializer()))
        }
    }
})
```

**出力形式** (Kotlin Code Style):

```kotlin
listOf(
    "item1",
    "item2",
)
```

### 2. StateHolder/ViewModel PBT スナップショットテスト (StateHolderSnapshotPbtSpec)

ランダムな入力 x アクション列の組み合わせで状態遷移を網羅テストする。

新規作成時は [templates/\_\_Target\_\_PbtSnapshotTest.kt](./templates/__Target__PbtSnapshotTest.kt)
をコピーして TODO を埋める (骨格をゼロから書かない)。

**ポイント**:

- `StateHolderSnapshotPbtSpec0` 〜 `StateHolderSnapshotPbtSpec20` で 0〜20 個の Arb に対応
- 各 Arb は `Gen<Pair<String, A>>` 形式（ラベル + 値）で渡す
- `stateFlow("名前") { flow }` — Flow ベースの状態を Turbine 経由でキャプチャ
- `state("名前") { value }` — 非 Flow の値を直接キャプチャ
- `actions { "名前" { アクション } }` — PBT で実行されるアクション列を定義
- `.forTest()` でテスト用ラッパーを利用（ログ記録・不正遷移検出）

### 3. Loader 単体 PBT

Loader を UseCase の Fake と組み合わせてテストする。

```kt
class SomeLoaderPbtSnapshotTest :
    StateHolderSnapshotPbtSpec1<TestSimpleLoader<String>, suspend () -> String>(
        { Arb.suspendFunction(returns = Arb.basicString()).withSuspendFunctionLabel() },
        actions = {
            "initialLoad" { initialLoad() }
            "refresh" { refresh() }
        },
        doSnapshot = { loadFn ->
            SimpleLoaderFactory.Default.create<String>(coroutineScope = this) { loadFn() }
                .forTest()
                .also {
                    stateFlow("state") { it.state }
                    state("logEntries") { it.logEntries.toList() }
                    state("illegalStateTransitions") { it.illegalStateTransitions.toList() }
                }
        },
    )
```

### 4. ロジック PBT スナップショットテスト (LogicSnapshotPbtSpec)

StateHolder でも Compose でもないロジック・関数の出力をランダム入力でスナップショットする。
UseCase・Cache・ユーティリティ関数などの純粋なロジックに使う。

新規作成時は [templates/\_\_Target\_\_LogicPbtSnapshotTest.kt](./templates/__Target__LogicPbtSnapshotTest.kt)
をコピーして TODO を埋める (骨格をゼロから書かない)。

**ポイント**:

- `LogicSnapshotPbtSpec1` 〜 `LogicSnapshotPbtSpec20` で 1〜20 個の Arb に対応
- 各 Arb は `Gen<Pair<String, A>>` 形式（ラベル + 値）で渡す
- `doSnapshot` は `suspend` — suspend 関数を直接呼び出せる
- `output("名前") { value }` — 出力値を登録
- アクション列 (`actions`) はなし — stateless なロジックのテストに適している

## PBT ユーティリティ

| ユーティリティ                       | 場所                                                      | 用途                                                |
|-------------------------------|---------------------------------------------------------|---------------------------------------------------|
| `Arb.suspendFunction()`       | `core/testing/snapshot/.../property/SuspendFunction.kt` | success/failure をランダム生成する suspend 関数              |
| `.withSuspendFunctionLabel()` | 同上                                                      | ラベル文字列付きの `Pair<String, FakeSuspendFunction>` に変換 |
| `Arb.basicString()`           | `core/testing/snapshot/.../property/String.kt`          | 多言語 Unicode 文字列生成                                 |
| `Arb.throwable()`             | `core/testing/snapshot/.../property/SuspendFunction.kt` | ランダムな例外生成 (軽量 stacktrace)                         |

## スナップショット出力

- 出力先: `build/snapshots/` (git tracked)
- 形式: Kotlin Code Style (`.kt` ファイルに直貼り可能)

## テスト用 Fake パターン

```kt
class HandleErrorForTest : HandleError {
    @Serializable(ThrowableSerializerForKotlinCodeFormat::class)
    val errors = mutableListOf<Throwable>()
    override fun handle(exception: Throwable) {
        errors.add(exception)
    }
}

class HandleWarningForTest : HandleWarning {
    val warnings = mutableListOf<List<String>>()
    override fun invoke(vararg warnings: String) {
        this@HandleWarningForTest.warnings.add(warnings.toList())
    }
}
```

## 実行コマンド

```bash
./tools/snapshot-diff.sh -before=<compare-commit-hash>
```
