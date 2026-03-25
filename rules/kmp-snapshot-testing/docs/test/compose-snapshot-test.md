# Compose UI スナップショットテスト

## 概要

Compose UI コンポーネントのスクリーンショット (PNG) とセマンティクス木をスナップショットとして記録し、
回帰テストするテスト。`jvmSnapshotTest` ソースセットに配置する。

## テストの種類

### 1. 単体 Compose スナップショットテスト (runComposableSnapshotTest)

個別の UI コンポーネントを固定パラメータでスナップショットする。

```kt
class AppButtonSnapshotTest : FreeSpec({
    "AppButton" - {
        AppButtonStyle.entries.forEach { style ->
            "${style.name} - Enable" {
                runComposableSnapshotTest {
                    AppButton(
                        action = AppButtonAction.Enable(onClick = {}),
                        style = style,
                    ) {
                        Text("Button")
                    }
                }
            }
        }
    }
})
```

**注意**: `runComposeUiTest` を直接使わず、必ず `runComposableSnapshotTest` ヘルパーを使う。

### 2. Compose PBT スナップショットテスト (ComposeSnapshotPbtSpec)

Density・ScreenSize・SystemTheme の組み合わせと PBT 入力で網羅テストする。

```kt
class AppConfigScreenSnapshotPbt : ComposeSnapshotPbtSpec1<Unit, suspend () -> MergedAppConfig>(
    genA = { Arb.suspendFunction(returns = Arb.mergedAppConfig()).withSuspendFunctionLabel() },
    content = { getAppConfig ->
        AppConfigScreen(
            viewModel = remember {
                AppConfigViewModel(
                    appConfigLoader = object :
                        AppConfigLoader by SimpleLoaderFactory.Default.create(
                            this,
                            load = { getAppConfig() },
                        ) {},
                )
            },
        )
    },
)
```

**ポイント**:

- `ComposeSnapshotPbtSpec0` 〜 `ComposeSnapshotPbtSpec20` で 0〜20 個の Arb に対応
- 各 Arb は `Gen<Pair<String, A>>` 形式（ラベル + 値）で渡す
- 自動的に以下の環境パラメータを組み合わせる:
    - **Density**: 0.1x 〜 2.5x
    - **ScreenSize**: 250dp 〜 1600dp
    - **SystemTheme**: Light / Dark
- `actions { }` でアクション列も定義可能

## スナップショット出力

各テストケースごとに以下のファイルが生成される:

```
build/snapshots/<TestClassName>/<テスト名>/
├── case_NNNN/
│   └── _inputs.expected.txt           # PBT 入力パラメータ
├── case_NNNN__density=X__fontScale=Y__size=WxH__theme=T/
│   ├── screenshot.expected.png        # レンダリング結果 (2x スケール)
│   ├── semantics.expected.txt         # セマンティクス木テキスト
│   └── semantics-layout.expected.png  # セマンティクス木の視覚化
```

## 実行コマンド

```bash
./tools/snapshot-diff.sh -before=<compare-commit-hash>
```
