# スナップショットテスト基盤アーキテクチャ

## モジュール構成

```
core/testing/snapshot/          # 値・StateHolder・ロジック PBT テスト基盤
├── ProjectConfig.kt            # Kotest 設定 (Dispatchers, PBT 反復数, レポーター)
├── snapshot/
│   ├── ShouldMatchSnapshot.kt  # shouldMatchSnapshot() — 値スナップショット
│   ├── SnapshotSpec.kt         # FreeSpec 基底クラス
│   ├── StateHolderSnapshotPbtSpec.kt  # StateHolder/ViewModel PBT (0-20 Arb)
│   ├── LogicSnapshotPbtSpec.kt        # ロジック PBT (1-20 Arb)
│   ├── LogicSnapshotOutputScope.kt    # output() DSL
│   ├── PbtActionScope.kt             # actions { } DSL
│   ├── PbtSnapshotReport.kt          # PBT ケースごとのレポート
│   ├── CheckAllSnapshot.kt           # 全スナップショット消費チェック
│   ├── code/
│   │   └── KotlinCodeFormat.kt       # StringFormat ラッパー
│   └── internal/
│       ├── code/
│       │   ├── KotlinCodeEncoder.kt       # AbstractEncoder (Kotlin 構文出力)
│       │   └── ThrowableSerializerForKotlinCodeFormat.kt
│       ├── SnapshotRegistry.kt            # ファイル管理
│       ├── RawSnapshotText.kt             # ファイル読み書き
│       ├── TextDiff.kt                    # Unified diff 生成
│       ├── ImageDiff.kt                   # 画像 diff 可視化
│       └── OrphanedSnapshotDetector.kt    # 孤立スナップショット検出
├── property/
│   ├── SuspendFunction.kt     # Arb.suspendFunction(), FakeSuspendFunction
│   ├── String.kt              # Arb.basicString() (多言語 Unicode)
│   └── SerializeBy.kt         # serializeBy ヘルパー
└── TestSimpleLoader.kt        # SimpleLoader テスト用ラッパー

ui/core/testing/                # Compose UI PBT テスト基盤
├── ComposeSnapshot.kt          # runComposableSnapshotTest()
└── ComposeSnapshotPbtSpec.kt   # Compose PBT (0-20 Arb, Density/ScreenSize/Theme)
```

## ProjectConfig

Kotest の `AbstractProjectConfig` を継承し、プロジェクト全体のテスト設定を行う。

```kotlin
class ProjectConfig : AbstractProjectConfig() {
    private val testDispatcher = StandardTestDispatcher()

    override suspend fun beforeProject() {
        Dispatchers.setMain(testDispatcher)
        PropertyTesting.defaultIterationCount =
            System.getProperty("pbt.iteration.count")?.toIntOrNull() ?: 2000
    }

    override suspend fun afterProject() {
        Dispatchers.resetMain()
    }

    override val extensions: List<Extension> = listOf(
        JunitXmlReporter(includeContainers = false, useTestPathAsName = true),
        HtmlReporter(),
        OrphanedSnapshotDetector(),
    )
}
```

## スナップショット出力形式

Kotlin Code Style で出力する。kotlinx.serialization の `AbstractEncoder` をカスタム実装。

```
Class:      Loaded(data = "x",)
List:       listOf("a", "b",)
Set:        setOf("a", "b",)
Map:        mapOf("key" to value,)
Sealed:     Initial (またはサブクラス名)
null:       null
Empty:      emptyList(), emptySet(), emptyMap()
```

## Record / Verify フロー

convention-kmp-snapshot-testing が以下のタスクを登録:

1. **jvmSnapshotTestRecord** — `snapshot-test-flavor=record` で実行。`*.expected.*` ファイルを生成
2. **jvmSnapshotTestVerify** — `snapshot-test-flavor=verify` で実行。期待値と比較し `*.actual.*` / `*.diff.*` を生成
3. **snapshotReport** — `result.json`, `result.md`, `result.html` を生成

テストコード側では `System.getProperty("snapshot-test-flavor")` で record/verify を判定し、
record なら期待値ファイルを書き込み、verify なら比較する。

## snapshot-diff.sh ワークフロー

```
Step 1: git worktree で before のコピーを作成
Step 2: worktree 内で jvmSnapshotTestRecord を実行
Step 3: スナップショットをメインツリーにコピー、worktree 削除
Step 4: after 側で jvmSnapshotTestVerify を実行
Step 5: snapshotReport タスクでレポート生成
```

## StateHolderSnapshotPbtSpec の仕組み

1. `Arb` で入力値をランダム生成（ラベル付き `Pair<String, A>`）
2. `doSnapshot` で テスト対象を構築、`stateFlow()` / `state()` で監視対象を登録
3. `actions { }` でアクション列を定義
4. PBT エンジンが入力 x アクション列の組み合わせを生成
5. 各組み合わせで:
   - テスト対象を構築
   - アクションを順番に実行
   - 各 stateFlow の遷移を Turbine で記録
   - 各 state の値をスナップショット
6. 結果を `build/snapshots/` に出力

## ComposeSnapshotPbtSpec の仕組み

StateHolderSnapshotPbtSpec と同様だが、追加で:
- Density (0.1x 〜 2.5x), ScreenSize (250dp 〜 1600dp), Theme (Light/Dark) を自動組み合わせ
- 各組み合わせでスクリーンショット (PNG) とセマンティクス木をキャプチャ
