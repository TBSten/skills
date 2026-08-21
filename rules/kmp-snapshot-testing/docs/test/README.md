# スナップショットテスト方針

## 基本方針

- テストフレームワークは [Kotest](https://kotest.io/) (FreeSpec スタイル) を使用する。
- 非同期 Flow のテストには [Turbine](https://github.com/cashapp/turbine) を使用する。
- プロパティベーステスト (PBT) には Kotest Property を使用する。
- テストは JVM 上で実行する (`./gradlew jvmTest`)。

## テストの種類

| 種類                                                   | ソースセット            | 詳細ドキュメント                               |
|------------------------------------------------------|-------------------|----------------------------------------|
| [スナップショットテスト](./snapshot-test.md)                    | `jvmSnapshotTest` | 値・StateHolder・ViewModel の PBT スナップショット |
| [Compose UI スナップショットテスト](./compose-snapshot-test.md) | `jvmSnapshotTest` | Compose UI コンポーネントの画像・セマンティクス スナップショット |

## テストコマンド

```bash
# スナップショット差分の確認
./tools/snapshot-diff.sh -before=<compare-commit-hash> -Ppbt.iteration.count=10

# PBT 反復数を指定して実行
./gradlew jvmTest -Ppbt.iteration.count=100
```

`tools/snapshot-diff.sh` が無い場合は `kmp-snapshot-testing-setup` skill (`gh skill install tbsten/skills kmp-snapshot-testing-setup`) で導入する。

## テスト骨格テンプレート

新規テストは [templates/](./templates/) の骨格をコピーして TODO を埋める (ゼロから書かない):

| テンプレート | 用途 |
|---|---|
| `templates/__Target__PbtSnapshotTest.kt` | StateHolder / ViewModel の PBT スナップショット |
| `templates/__Target__LogicPbtSnapshotTest.kt` | ロジック・関数の PBT スナップショット |
| `templates/__Target__ComposeSnapshotPbt.kt` | Compose UI の PBT スナップショット |

## テストモジュール構成

```
core/testing/snapshot/   ... スナップショットテスト基盤 (shouldMatchSnapshot, StateHolderSnapshotPbtSpec, PBT ユーティリティ)
ui/core/testing/         ... Compose UI テスト基盤 (runComposableSnapshotTest, ComposeSnapshotPbtSpec)
```

## テスト設定 (ProjectConfig)

`core/testing/snapshot` の `ProjectConfig.kt` で以下を設定:

- `Dispatchers.setMain(StandardTestDispatcher())` でテスト用メインディスパッチャを設定
- PBT デフォルト反復数: 2000 (`-Ppbt.iteration.count` でオーバーライド可)
- JUnit XML / HTML レポートの自動生成
- 孤立スナップショットの検出 (OrphanedSnapshotDetector)

## Convention Plugin

- `convention-kmp-snapshot-testing` — jvmSnapshotTest ソースセットと Record/Verify タスクを自動登録
