# テスト方針

## 基本方針

- テストフレームワークは [Kotest](https://kotest.io/) (FreeSpec スタイル) を使用する。
- 非同期 Flow のテストには [Turbine](https://github.com/cashapp/turbine) を使用する。
- プロパティベーステスト (PBT) には Kotest Property を使用する。
- テストは JVM 上で実行する (`./gradlew jvmTest`)。

## テストの種類

| 種類                                                   | ソースセット            | 詳細ドキュメント                               |
|------------------------------------------------------|-------------------|----------------------------------------|
| [ユニットテスト](./unit-test.md)                            | `commonTest`      | ビジネスロジックの状態遷移・振る舞いテスト                  |
| [スナップショットテスト](./snapshot-test.md)                    | `jvmSnapshotTest` | 値・StateHolder・ViewModel の PBT スナップショット |
| [Compose UI スナップショットテスト](./compose-snapshot-test.md) | `jvmSnapshotTest` | Compose UI コンポーネントの画像・セマンティクス スナップショット |

## テストコマンド

```bash
# ユニットテスト + スナップショット検証
./gradlew jvmTest

# スナップショット差分の確認
./tools/snapshot-diff.sh -before=<compare-commit-hash> -Ppbt.iteration.count=10

# PBT 反復数を指定して実行
./gradlew jvmTest -Ppbt.iteration.count=100
```

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

- `convention-kmp-test` — commonTest / jvmTest の依存を自動付与
- `convention-kmp-snapshot-testing` — jvmSnapshotTest ソースセットと Record/Verify タスクを自動登録
