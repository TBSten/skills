# KMP スナップショットテスト基盤セットアップ

このプロンプトは [TBSten/skills](https://github.com/TBSten/skills) の `prompts/kmp-snapshot-testing-setup` として配布されている一回限りのプロンプト。KMP + Compose Multiplatform プロジェクトにスナップショットテスト基盤 (convention plugins、Kotest PBT 基底クラス、snapshot-diff.sh) を一式セットアップする。

ファイル配置・置換はリポジトリ同梱の `scripts/install.sh` が一括で行う。AI の責務は、確認事項のヒアリング、script 実行と出力レビュー、script が対応しない「AI フォローアップ」、ビルド確認である。

## 参照ファイルの取得方法

このプロンプトが参照するファイルは GitHub リポジトリ [TBSten/skills](https://github.com/TBSten/skills) にある。ファイル数が多いので sparse clone でまとめて取得する:

```sh
git clone --depth 1 --filter=blob:none --sparse https://github.com/TBSten/skills.git /tmp/tbsten-skills
git -C /tmp/tbsten-skills sparse-checkout set skills/kmp-snapshot-testing-setup
```

以降の手順では、sparse clone 後の `/tmp/tbsten-skills/skills/kmp-snapshot-testing-setup/` を `<SKILL_DIR>` と表記する。

## 確認事項

セットアップ前に以下を確認する。ユーザーの指示から明確に読み取れる項目は確認を省略してよい。

1. **プロジェクト構成** — build-logic (または buildSrc) の有無、既存の convention plugin
   (build-logic が無い場合はこのプロンプトの前に build-logic のセットアップが必要)
2. **テスト基盤モジュールのパス** — デフォルト: `core/testing/snapshot` (値/StateHolder PBT) + `ui/core/testing` (Compose PBT)
3. **パッケージ名** — 既存の構成から推定
4. **セットアップ範囲** — Compose を使わないプロジェクトなら `--skip-compose`
   (Compose テストモジュールと Compose 向け assertion を省く)

## セットアップ手順

### Step 1: install.sh の実行

sparse clone した `<SKILL_DIR>/scripts/install.sh` を **そのまま実行する**。
script を読解・書き換え・再実装してはならない。調整するのは引数のみ。

```bash
bash /tmp/tbsten-skills/skills/kmp-snapshot-testing-setup/scripts/install.sh \
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

テスト基盤のアーキテクチャ詳細は
`https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/skills/kmp-snapshot-testing-setup/references/architecture.md`
(sparse clone 後は `/tmp/tbsten-skills/skills/kmp-snapshot-testing-setup/references/architecture.md`) を参照する。

### Step 3: 対象モジュールへの適用

スナップショットテストを書きたいモジュールの `build.gradle.kts` に適用する:

```kotlin
plugins {
    id("convention-kmp-snapshot-testing")
}
```

### Step 4: ビルド確認

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
