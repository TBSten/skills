# KMP スナップショットテスト基盤セットアップ

このプロンプトは [TBSten/skills](https://github.com/TBSten/skills) の `prompts/kmp-snapshot-testing-setup` として配布されている一回限りのプロンプト。KMP + Compose Multiplatform プロジェクトにスナップショットテスト基盤 (convention plugins、Kotest PBT 基底クラス、snapshot-diff.sh) を一式セットアップする。

## 参照ファイルの取得方法

このプロンプトが参照するファイルは GitHub リポジトリ [TBSten/skills](https://github.com/TBSten/skills) にある。

- 単一ファイル: `https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/<パス>` を WebFetch や curl で取得する
- ディレクトリの一覧: `https://api.github.com/repos/TBSten/skills/contents/<パス>` で取得する
- このプロンプトはファイル数が多いので sparse clone でまとめて取得するのが速い:

```sh
git clone --depth 1 --filter=blob:none --sparse https://github.com/TBSten/skills.git /tmp/tbsten-skills
git -C /tmp/tbsten-skills sparse-checkout set skills/kmp-snapshot-testing-setup
```

以降の手順では、sparse clone 後の `/tmp/tbsten-skills/skills/kmp-snapshot-testing-setup/` を `<SKILL_DIR>` と表記する。

## 確認事項

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

sparse clone した `<SKILL_DIR>/example/build-logic/` 内のファイルを `build-logic/src/main/kotlin/` にコピーし、
プロジェクト固有の値を置換する。

```bash
cp /tmp/tbsten-skills/skills/kmp-snapshot-testing-setup/example/build-logic/*.kt <PROJECT>/build-logic/src/main/kotlin/
cp /tmp/tbsten-skills/skills/kmp-snapshot-testing-setup/example/build-logic/*.gradle.kts <PROJECT>/build-logic/src/main/kotlin/
```

置換が必要な箇所:
- `kotest.framework.config.fqn` の値 → プロジェクトの ProjectConfig の FQCN
- `:core:testing:snapshot` → テスト基盤モジュールのパス

生成されるファイル:
- `convention-kmp-test.gradle.kts` — commonTest/jvmTest の共通依存を付与
- `convention-kmp-snapshot-testing.gradle.kts` — jvmSnapshotTest ソースセットと Record/Verify/Report タスクを登録
- `SnapshotReportTask.kt` — スナップショット差分レポート生成タスク

### Step 3: テスト基盤モジュールの作成

sparse clone した `<SKILL_DIR>/example/core-testing-snapshot/` と `<SKILL_DIR>/example/ui-core-testing/` のファイルをプロジェクトにコピーし、
パッケージ名を置換する。

```bash
# core/testing/snapshot モジュール
cp -r /tmp/tbsten-skills/skills/kmp-snapshot-testing-setup/example/core-testing-snapshot/ <PROJECT>/core/testing/snapshot/src/jvmMain/kotlin/<package-path>/

# ui/core/testing モジュール (Compose PBT)
cp -r /tmp/tbsten-skills/skills/kmp-snapshot-testing-setup/example/ui-core-testing/ <PROJECT>/ui/core/testing/src/jvmMain/kotlin/<package-path>/

# パッケージ名を置換
find <TARGET_DIRS> -name "*.kt" -exec sed -i '' 's/com\.example\.snapshot/<your-package>/g' {} +
```

テスト基盤のアーキテクチャ詳細は
`https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/skills/kmp-snapshot-testing-setup/references/architecture.md`
(sparse clone 後は `/tmp/tbsten-skills/skills/kmp-snapshot-testing-setup/references/architecture.md`) を参照する。

#### core-testing-snapshot の構成 (25 ファイル)

- **ProjectConfig.kt** — Kotest 設定 (Dispatchers.setMain, PBT 反復数, レポーター)
- **testing/snapshot/**
  - `ShouldMatchSnapshot.kt` — 値の Kotlin Code 形式スナップショット
  - `StateHolderSnapshotPbtSpec.kt` — StateHolder/ViewModel PBT (0-20 Arb)
  - `LogicSnapshotPbtSpec.kt` — ロジック PBT (1-20 Arb)
  - `SnapshotSpec.kt`, `PbtActionScope.kt`, `PbtSnapshotReport.kt` 等
  - `code/KotlinCodeFormat.kt` — kotlinx.serialization の Kotlin Code 出力
  - `internal/` — SnapshotRegistry, TextDiff, ImageDiff, OrphanedSnapshotDetector 等
- **testing/property/**
  - `SuspendFunction.kt` — `Arb.suspendFunction()`, FakeSuspendFunction
  - `String.kt` — `Arb.basicString()` (多言語 Unicode)

#### ui-core-testing の構成 (2 ファイル)

- `ComposeSnapshot.kt` — `runComposableSnapshotTest()` ヘルパー
- `ComposeSnapshotPbtSpec.kt` — Compose PBT (0-20 Arb, Density/ScreenSize/Theme)

### Step 4: Shell Scripts の配置

sparse clone した `<SKILL_DIR>/example/tools/` 内のスクリプトをプロジェクトにコピーする。

```bash
cp -r /tmp/tbsten-skills/skills/kmp-snapshot-testing-setup/example/tools/ <PROJECT>/tools/
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

スナップショットテストを書きたいモジュールの `build.gradle.kts` に適用する:

```kotlin
plugins {
    id("convention-kmp-snapshot-testing")
}
```

### Step 7: ビルド確認

```bash
./gradlew compileKotlinJvm
```

## 後片付け

セットアップ完了後、sparse clone を削除する。

```bash
rm -rf /tmp/tbsten-skills
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
