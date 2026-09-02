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
metadata:
  status: Experimental
  group: Kotlin / Android アプリ開発
---

# KMP Snapshot Testing Setup

KMP + Compose Multiplatform プロジェクトにスナップショットテスト基盤を一式セットアップする。

ファイル配置・置換は `scripts/install.sh` が一括で行う。AI の責務は、確認事項のヒアリング、
script 実行と出力レビュー、script が対応しない下記「AI フォローアップ」、ビルド確認である。

## Usage

### 確認事項

セットアップ前に以下を確認する。ユーザーの指示から明確に読み取れる項目は確認を省略してよい。

1. **プロジェクト構成** — build-logic (または buildSrc) の有無、既存の convention plugin
   (build-logic が無い場合はこのスキルの前に build-logic のセットアップが必要)
2. **テスト基盤モジュールのパス** — デフォルト: `core/testing/snapshot` (値/StateHolder PBT) + `ui/core/testing` (Compose PBT)
3. **パッケージ名** — 既存の構成から推定
4. **セットアップ範囲** — Compose を使わないプロジェクトなら `--skip-compose`
   (Compose テストモジュールと Compose 向け assertion を省く)

## セットアップ手順

### Step 1: install.sh の実行

`${CLAUDE_SKILL_DIR}/scripts/install.sh` を **そのまま実行する**。
script を読解・書き換え・再実装してはならない。調整するのは引数のみ。

```bash
bash "${CLAUDE_SKILL_DIR}/scripts/install.sh" \
  --project <PROJECT_DIR> \
  --package <your.package>
```

| オプション | 必須 | 説明 |
|---|---|---|
| `--project <dir>` | ✔ | 対象プロジェクトのルート |
| `--package <pkg>` | ✔ | テスト基盤のパッケージ (`com.example.snapshot` を置換) |
| `--module-path <path>` | | テスト基盤モジュール (default: `core/testing/snapshot`) |
| `--ui-module-path <path>` | | Compose テストモジュール (default: `ui/core/testing`) |
| `--skip-compose` | | Compose 関連 (ui モジュール + Compose 向け assertion + Compose 依存) を省く |
| `--dry-run` | | 書き込みせず実行内容を表示 |
| `--force` | | 既存ファイルを上書き (無指定時は conflict として skip される) |

script がやること:

1. `gradle/libs.versions.toml` へ versions (kotest / turbine / kotlinx-serialization) と
   libraries (kotest 6 種, turbine, kotlinx-coroutines-test, kotlinxSerializationCore) を冪等追記
   (`kotlinx-coroutines` version が未定義なら TODO 付きで追加して警告)
2. build-logic へ convention plugin 3 ファイル (convention-kmp-test.gradle.kts,
   convention-kmp-snapshot-testing.gradle.kts, SnapshotReportTask.kt) をコピーし、
   ProjectConfig の FQCN とテスト基盤モジュールパスを置換
3. テスト基盤モジュール 2 つの `build.gradle.kts` (テンプレート) を配置し、
   `settings.gradle(.kts)` へ include を冪等追記
4. Kotlin ソース (core 26 ファイル + Compose 2 ファイル) をパッケージパスへコピーし、
   `com.example.snapshot` → `--package` に置換 (置換漏れは script が検出して fail する)
5. `tools/` (snapshot-diff.sh + step1-5) をコピーして chmod +x
6. ルート `build.gradle.kts` へ `cleanSnapshotOutputDir` タスクを冪等追記

末尾に結果 JSON を 1 行出力する。`conflicts` / `warnings` / `manual_followups` を必ずレビューし、
warnings (特に version の TODO) と followups を次の Step で解消する。
再実行は安全 (冪等)。既存ファイルと差分がある場合は `--force` を付けない限り上書きしない。

### Step 2: AI フォローアップ (script がやらないこと)

プロジェクトごとに判断が必要なため、以下は AI が行う。

1. **build-logic への serialization plugin classpath 追加** —
   `convention-kmp-snapshot-testing` は `org.jetbrains.kotlin.plugin.serialization` を適用するため、
   build-logic (または buildSrc) の `build.gradle.kts` の dependencies に serialization plugin を追加する。
   プロジェクトの流儀 (catalog 経由 / 直書き) に合わせること。

   ```kotlin
   dependencies {
       implementation("org.jetbrains.kotlin:kotlin-serialization:<kotlin-version>")
   }
   ```

2. **`convention-kmp` の読み替え** — convention plugin のコメントにある
   「convention-kmp 適用後に使用すること」の `convention-kmp` はこのスキルには含まれない。
   プロジェクトの KMP 共通 convention plugin に読み替える。
   配置されるモジュール `build.gradle.kts` テンプレートは KMP 設定 (`jvm()` 等) を自前で行っているため、
   相当する convention が無ければそのままで動く。ある場合は plugins ブロックをそちらに寄せる。

3. **catalog / plugins 方式の整合** — テンプレートは `id("org.jetbrains.kotlin.multiplatform")` 等の
   素の plugin ID と `libs.kotestFrameworkEngine` 等の alias を前提にしている。
   プロジェクトが `alias(libs.plugins.*)` 方式なら合わせて書き換える。
   script が `skipped_aliases` を報告した場合は既存 alias に合わせて依存参照を修正する。

4. **convention plugin の `libs` アクセサ** — build-logic で `libs.findLibrary` が解決できない場合は、
   各 convention `.gradle.kts` の冒頭に以下を追加する。

   ```kotlin
   val libs = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")
   ```

5. **Compose テストモジュールのプロジェクト固有 seam** (`--skip-compose` 時は不要) —
   `ComposeSnapshot.kt` の `AppTheme.Provider` / `WithTestGraph` はプロジェクトの theme / DI 前提の
   参照なので、プロジェクトの実装に合わせて書き換える (不要なら wrapper を外す)。
   併せて `ui` モジュール `build.gradle.kts` の TODO コメント (theme モジュールへの依存) を解消する。

テスト基盤のアーキテクチャ詳細は references/architecture.md を参照。

#### core-testing-snapshot の構成 (26 ファイル)

- **ProjectConfig.kt** — Kotest 設定 (Dispatchers.setMain, PBT 反復数, レポーター)
- **core/PrintOnlyDebug.kt** — デバッグ時のみ出力するログヘルパー
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

### Step 3: 対象モジュールへの適用

スナップショットテストを書きたいモジュールの `build.gradle.kts` に適用:

```kotlin
plugins {
    id("convention-kmp-snapshot-testing")
}
```

### Step 4: ビルド確認

```bash
./gradlew compileKotlinJvm
```

## セットアップ完了メッセージ

```
## セットアップ完了

### install.sh の実行結果
- installed: <N> / conflicts: [...] / warnings: [...]

### Convention Plugins
- convention-kmp-test.gradle.kts
- convention-kmp-snapshot-testing.gradle.kts
- SnapshotReportTask.kt

### テスト基盤モジュール
- <module-path> (shouldMatchSnapshot, StateHolderSnapshotPbtSpec, ...)
- <ui-module-path> (ComposeSnapshotPbtSpec, runComposableSnapshotTest)

### Shell Scripts
- tools/snapshot-diff.sh + step1-5

### AI フォローアップの対応状況
- [serialization plugin classpath / convention-kmp 読み替え / AppTheme 調整 など]

### ビルド結果
- [SUCCESS / FAILED]

### 使い方
./tools/snapshot-diff.sh -before=main
```
