---
name: kmp-snapshot-testing-setup
description: >
  Sets up snapshot testing infrastructure for Kotlin Multiplatform + Compose projects.
  Creates build-logic convention plugins (convention-kmp-test, convention-kmp-snapshot-testing,
  SnapshotReportTask), a core testing module with Kotest PBT base classes
  (StateHolderSnapshotPbtSpec, LogicSnapshotPbtSpec, ComposeSnapshotPbtSpec, shouldMatchSnapshot),
  and shell scripts for snapshot diff workflows (snapshot-diff.sh).
  Use when requested: "スナップショットテストを導入", "snapshot test をセットアップ",
  "PBT でスナップショットテスト", "snapshot-diff.sh を使えるようにして",
  "テスト基盤モジュールを作りたい", "setup snapshot testing".
  For KMP + Compose Multiplatform projects using Gradle with build-logic convention plugins.
---

# KMP Snapshot Testing Setup

KMP + Compose Multiplatform プロジェクトにスナップショットテスト基盤を一式セットアップする。

## Usage

### 確認事項

セットアップ前に以下を確認する。ユーザーの指示から明確に読み取れる項目は確認を省略してよい。

1. **プロジェクト構成** — build-logic ディレクトリの有無、既存の convention plugin
2. **テスト基盤モジュールのパス** — デフォルト: `core/testing/snapshot` (値/StateHolder PBT) + `ui/core/testing` (Compose PBT)
3. **パッケージ名** — 既存の構成から推定
4. **セットアップ範囲** — 以下から選択 (デフォルト: 全て)
   - [x] Convention plugins (convention-kmp-test, convention-kmp-snapshot-testing)
   - [x] Core testing module (shouldMatchSnapshot, StateHolderSnapshotPbtSpec, LogicSnapshotPbtSpec)
   - [x] Compose testing module (ComposeSnapshotPbtSpec, runComposableSnapshotTest)
   - [x] Shell scripts (tools/snapshot-diff.sh + step scripts)

## セットアップ手順

### Step 1: 依存関係の追加

`gradle/libs.versions.toml` に以下を追加する。既存のエントリは追加しない。

```toml
[versions]
kotest = "6.0.0.M1"  # or latest
turbine = "1.2.0"    # or latest

[libraries]
kotestFrameworkEngine = { module = "io.kotest:kotest-framework-engine", version.ref = "kotest" }
kotestAssertionsCore = { module = "io.kotest:kotest-assertions-core", version.ref = "kotest" }
kotestRunnerJunit5 = { module = "io.kotest:kotest-runner-junit5", version.ref = "kotest" }
kotestProperty = { module = "io.kotest:kotest-property", version.ref = "kotest" }
kotestExtensionsHtmlReporter = { module = "io.kotest:kotest-extensions-htmlreporter", version.ref = "kotest" }
kotestExtensionsJunitXml = { module = "io.kotest:kotest-extensions-junitxml", version.ref = "kotest" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
```

### Step 2: Convention Plugins の作成

`example/build-logic/` 内のファイルを `build-logic/src/main/kotlin/` にコピーし、
プロジェクト固有の値を置換する。

```bash
cp <SKILL_DIR>/example/build-logic/*.kt <PROJECT>/build-logic/src/main/kotlin/
cp <SKILL_DIR>/example/build-logic/*.gradle.kts <PROJECT>/build-logic/src/main/kotlin/
```

置換が必要な箇所:
- `kotest.framework.config.fqn` の値 → プロジェクトの ProjectConfig の FQCN
- `:core:testing:snapshot` → テスト基盤モジュールのパス

生成されるファイル:
- `convention-kmp-test.gradle.kts` — commonTest/jvmTest の共通依存を付与
- `convention-kmp-snapshot-testing.gradle.kts` — jvmSnapshotTest ソースセットと Record/Verify/Report タスクを登録
- `SnapshotReportTask.kt` — スナップショット差分レポート生成タスク

### Step 3: テスト基盤モジュールの作成

この手順で作成するモジュールの Kotlin コードは量が多い (~30 ファイル)。
references/architecture.md を参照し、各コンポーネントの役割を理解した上で実装する。

主要コンポーネント:
- **ProjectConfig** — Kotest の設定 (Dispatchers.setMain, PBT 反復数, レポーター)
- **shouldMatchSnapshot** — 値の Kotlin Code 形式スナップショット
- **KotlinCodeFormat / KotlinCodeEncoder** — kotlinx.serialization の Kotlin Code 出力
- **StateHolderSnapshotPbtSpec0-20** — StateHolder/ViewModel PBT テスト基底クラス
- **LogicSnapshotPbtSpec1-20** — ロジック PBT テスト基底クラス
- **ComposeSnapshotPbtSpec0-20** — Compose UI PBT テスト基底クラス
- **PBT ユーティリティ** — Arb.suspendFunction(), Arb.basicString()
- **SnapshotRegistry / OrphanedSnapshotDetector** — スナップショットファイル管理

### Step 4: Shell Scripts の配置

`example/tools/` 内のスクリプトをプロジェクトにコピーする。

```bash
cp -r <SKILL_DIR>/example/tools/ <PROJECT>/tools/
chmod +x <PROJECT>/tools/snapshot-diff.sh
```

### Step 5: ルート build.gradle.kts の設定

```kotlin
tasks.register<Delete>("cleanSnapshotOutputDir") {
    group = "verification"
    description = "Deletes build/snapshots directory"
    delete(layout.projectDirectory.dir("build/snapshots"))
}
```

### Step 6: 対象モジュールへの適用

スナップショットテストを書きたいモジュールの `build.gradle.kts` に適用:

```kotlin
plugins {
    id("convention-kmp-snapshot-testing")
}
```

### Step 7: ビルド確認

```bash
./gradlew compileKotlinJvm
```

## セットアップ完了メッセージ

```
## セットアップ完了

### Convention Plugins
- convention-kmp-test.gradle.kts
- convention-kmp-snapshot-testing.gradle.kts
- SnapshotReportTask.kt

### テスト基盤モジュール
- <module-path> (shouldMatchSnapshot, StateHolderSnapshotPbtSpec, ...)

### Shell Scripts
- tools/snapshot-diff.sh + step1-5

### 依存関係
- [追加: kotest, turbine, ...]

### ビルド結果
- [SUCCESS / FAILED]

### 使い方
./tools/snapshot-diff.sh -before=main
```
