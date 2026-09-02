---
name: kotlin-compiler-plugin-setup
description: >
  Sets up a Kotlin Compiler Plugin project with multi-module Gradle structure.
  Creates buildSrc convention plugins (kotlin-jvm), compiler-plugin module with
  AutoService + KSP for plugin registration, Gradle plugin wrapper
  (KotlinCompilerPluginSupportPlugin), runtime API module (Kotlin Multiplatform),
  unit tests with kctfork (KotlinCompilation testing) + Kotest, and integration
  test module using kotlinCompilerPluginClasspath.
  Targets Kotlin 2.x K2 compiler with FIR + IR extension architecture.
  Use when requested: "Kotlin compiler plugin を作りたい", "compiler plugin のプロジェクトをセットアップ",
  "setup kotlin compiler plugin", "KotlinCompilation でテストしたい",
  "compiler plugin の unit test を書きたい", "FIR/IR extension のプロジェクト構成",
  "Gradle plugin で compiler plugin をラップ", "compiler plugin の integration test をセットアップ".
metadata:
  status: Experimental
  group: Kotlin ライブラリ/ツール開発
---

# Kotlin Compiler Plugin Setup

Kotlin Compiler Plugin のマルチモジュールプロジェクトを一式セットアップする。

## Usage

### 確認事項

セットアップ前に以下を確認する。ユーザーの指示から明確に読み取れる項目は確認を省略してよい。

1. **プロジェクト名** — ルートプロジェクト名 (kebab-case)
2. **Group ID** — Maven artifact の groupId (例: `com.example.myplugin`)
3. **Plugin ID** — Kotlin compiler plugin の ID (例: `com.example.myplugin`)。通常 groupId と同じ
4. **セットアップ範囲** — 以下から選択 (デフォルト: 全て)
   - [x] buildSrc convention plugins
   - [x] compiler-plugin module (FIR + IR)
   - [x] gradle-plugin module (KotlinCompilerPluginSupportPlugin)
   - [x] runtime module (Kotlin Multiplatform)
   - [x] Unit tests (kctfork + Kotest)
   - [x] Integration test module
5. **Kotlin バージョン** — デフォルト: 最新安定版
6. **Java toolchain バージョン** — デフォルト: 21

## セットアップ手順

### Step 1: scaffold script の実行

`scripts/scaffold.sh` がプロジェクト一式 (build ファイル + Kotlin ソース skeleton) を生成する。
**script を読解・書き換え・再実装せず、そのまま実行する。**

```bash
bash <skill-dir>/scripts/scaffold.sh \
  --dest <project-root> \
  --name <project-name> \
  --group-id <group-id> \
  --plugin-id <plugin-id> \
  --kotlin-version <kotlin-version>
```

オプション (詳細は `scaffold.sh --help`):

| オプション | 説明 |
|---|---|
| `--dest` / `--name` / `--group-id` | 必須。生成先 / rootProject.name (kebab-case) / Maven groupId |
| `--plugin-id` | compiler plugin ID (default: `--group-id`) |
| `--package` | Kotlin パッケージ (default: `--group-id` から `-` を除去) |
| `--kotlin-version` | Kotlin バージョン (default: example の値) |
| `--skip-gradle-plugin` / `--skip-integration-test` / `--skip-test` | 確認事項 4 でスコープを絞った場合に使用 |
| `--dry-run` | 生成予定一覧のみ表示 |
| `--force` | 既存ファイルを上書き (デフォルトでは上書きしない) |

script は `example/` (実パッケージ `com.example.compilerpluginsetup` + `Example` クラス prefix で書かれた skeleton) をコピーし、`--name` から導出した PascalCase prefix とパッケージ・ID 群に置換・rename する。生成される構成:

```
<project-root>/
├── buildSrc/                  # convention plugins (kotlin-jvm)
├── compiler-plugin/           # 登録クラス群 + ExampleCompilerTest (kctfork + Kotest)
├── gradle-plugin/             # KotlinCompilerPluginSupportPlugin
├── runtime/                   # KMP API 宣言用モジュール
├── integration-test/
│   ├── test-jvm/              # JVM 単体の E2E テスト (Main.kt)
│   └── test-kmp/              # KMP (JVM + JS) の E2E テスト (Main.kt)
├── gradle/libs.versions.toml
└── settings.gradle.kts
```

### Step 2: 生成結果のレビュー

script の stdout (末尾 1 行の JSON を含む) と生成ファイルを確認する:

1. `settings.gradle.kts` の `rootProject.name` とモジュール構成が意図どおりか
2. `CommandLineProcessor.pluginId` / `gradlePlugin { create(...) { id = ... } }` が指定した Plugin ID か
3. `gradle/libs.versions.toml` の Kotlin バージョン
4. 置換漏れが無いか: `grep -rn "compilerpluginsetup\|Example[A-Z]" <project-root>` がヒットしないこと (script も自動チェック済み)

各生成ファイルの設計解説は references を参照:

| ファイル群 | 解説 |
|---|---|
| CommandLineProcessor / Registrar / IrExtension / FirExtensionRegistrar | references/plugin-registration.md |
| ExampleGradlePlugin (KotlinCompilerPluginSupportPlugin) | references/gradle-plugin-impl.md |
| ExampleCompilerTest (compile ヘルパー) / integration-test | references/testing-patterns.md |

publish-convention が必要な場合は references/publish-convention.md を参照。

### Step 3: ビルド確認

```bash
./gradlew jvmTest
./gradlew :integration-test:test-jvm:run
./gradlew :integration-test:test-kmp:jvmRun
```

### Step 4: Multi-Kotlin Version Support (上級、任意)

1 つの JAR で複数の Kotlin バージョン (例: 2.0.0 〜 2.4.x) をサポートしたい場合に実施する。

**戦略の選択**:
- **タンデムリリース** — プラグインバージョン = Kotlin バージョン。最もシンプル (kotlinx.serialization 方式)
- **独立リリース** — 1 JAR で多バージョン対応。以下 2 つのアーキテクチャがある:
  - **A: Source Set Separation** — Gradle がビルド時にソースディレクトリを切り替え。K1/K2 断絶の吸収に最適
  - **B: Compat Module Layer (metro スタイル)** — ServiceLoader で実装を動的選択。K2+ のパッチ差異の吸収に最適

詳細なセットアップ手順と全コード例は `references/multi-version-setup.md` を参照。
バージョンの追加・削除の継続的な作業は `kotlin-compiler-plugin-dev` スキル (Step 6) を使用する。

## セットアップ完了メッセージ

```
## セットアップ完了

### プロジェクト構成
- buildSrc/ (convention plugins)
- compiler-plugin/ (FIR + IR extensions)
- gradle-plugin/ (KotlinCompilerPluginSupportPlugin)
- runtime/ (Multiplatform API declarations)
- integration-test/test-jvm/ (JVM E2E test)
- integration-test/test-kmp/ (KMP E2E test)

### 依存関係
- kotlin-compiler-embeddable: <version>
- auto-service + KSP
- kctfork: <version>
- kotest: <version>

### ビルド結果
- jvmTest: [SUCCESS / FAILED]
- integration-test: [SUCCESS / FAILED]

### 次のステップ
1. compiler-plugin/src/main/kotlin/ の <Prefix>Transformer (no-op skeleton) に IR 変換を実装、必要なら <Prefix>FirExtensionRegistrar に FIR checker を登録
2. runtime/src/commonMain/kotlin/ に公開 API を宣言
3. compiler-plugin/src/test/ の <Prefix>CompilerTest にテストケースを追加
```
