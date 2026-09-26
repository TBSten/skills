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
| `--kotlin-version` | Kotlin バージョン (default: example の値)。Gradle plugin の対応 Kotlin 範囲 (`SUPPORTED_KOTLIN_*`) もここから導出 |
| `--skip-gradle-plugin` / `--skip-integration-test` / `--skip-test` | 確認事項 4 でスコープを絞った場合に使用 |
| `--dry-run` | 生成予定一覧のみ表示 |
| `--force` | 既存ファイルを上書き (デフォルトでは上書きしない) |

script は `example/` (実パッケージ `com.example.compilerpluginsetup` + `Example` クラス prefix で書かれた skeleton) をコピーし、`--name` から導出した PascalCase prefix とパッケージ・ID 群に置換・rename する。生成される構成:

```
<project-root>/
├── buildSrc/                  # convention plugins (kotlin-jvm: JDK 21 で Java 17 bytecode / publish: vanniktech)
├── compiler-plugin/           # 登録クラス群 (`enabled` オプション配線済み) + CompilerTest (kctfork + Kotest)
├── gradle-plugin/             # KotlinCompilerPluginSupportPlugin + DSL + Kotlin 版ガード + BuildConfig 生成 + ProjectBuilder テスト
├── runtime/                   # KMP API 宣言用モジュール (placeholder の Marker 1 つ)
├── integration-test/
│   ├── test-jvm/              # JVM 単体の E2E テスト (Main.kt)
│   ├── test-kmp/              # KMP (JVM + JS) の E2E テスト (Main.kt)
│   └── test-gradle-plugin/    # Gradle TestKit E2E (fixture + includeBuild。`plugins { id(...) }` の実利用形態)
├── .github/workflows/         # ci.yml (PR) / release.yml (tag push → Maven Central + GitHub Release)
├── gradle.properties          # GROUP / VERSION_NAME / plugin id / 対応 Kotlin 範囲 / POM_* の SSoT + ビルド高速化設定
├── gradle/libs.versions.toml
├── build.gradle.kts           # allprojects { group / version } (gradle.properties から)
└── settings.gradle.kts
```

### Step 2: 生成結果のレビュー

script の stdout (末尾 1 行の JSON を含む) と生成ファイルを確認する:

1. `settings.gradle.kts` の `rootProject.name` とモジュール構成が意図どおりか
2. `gradle.properties` の `GROUP` / `COMPILER_PLUGIN_ID` / `SUPPORTED_KOTLIN_*`、`CommandLineProcessor.pluginId` / `Registrar.pluginId` が指定値か
3. `gradle/libs.versions.toml` の Kotlin バージョン
4. 置換漏れが無いか: `grep -rn "compilerpluginsetup\|Example[A-Z]" <project-root>` がヒットしないこと (script も自動チェック済み)
5. `gradle.properties` の `POM_*` (`TODO-owner`) は公開前にユーザーの GitHub リポジトリへ直すよう伝える

各生成ファイルの設計解説は references を参照:

| ファイル群 | 解説 |
|---|---|
| CommandLineProcessor / Registrar / IrExtension / FirExtensionRegistrar | references/plugin-registration.md |
| ExampleGradlePlugin / Extension / KotlinVersionGuard / BuildConfig 生成 | references/gradle-plugin-impl.md |
| CompilerTest (kctfork) / integration-test / ProjectBuilder・TestKit テスト | references/testing-patterns.md |
| publish convention / gradle.properties / CI・release workflow | references/publish-convention.md |

### Step 3: ビルド確認

scaffold は Gradle wrapper を生成しない。`gradlew` が無ければ先に `gradle wrapper --gradle-version <最新安定版>` を実行する。

```bash
./gradlew :compiler-plugin:test :gradle-plugin:test :runtime:jvmTest
./gradlew :integration-test:test-jvm:run :integration-test:test-kmp:jvmRun
./gradlew :integration-test:test-gradle-plugin:test   # TestKit E2E (数分かかる)
./gradlew publishToMavenLocal --no-configuration-cache # 公開設定の確認 (plugin marker も出る)
```

### 外部 guide: scaffold 後の実装・テスト・配布

scaffold 後に FIR / IR の実装・テスト・Gradle plugin・multi-version を進めるときは、外部 skill [kitakkun/kotlin-compiler-plugin-skills](https://github.com/kitakkun/kotlin-compiler-plugin-skills) (MIT, kitakkun, Kotlin 2.4.x 基準) の guide を参照する。利用可能 skill 一覧に `kotlin-compiler-plugin` がある、または `~/.claude/plugins/cache/` 配下にインストール済みならそちらを優先し、なければ raw URL を WebFetch / `curl -fsSL` で取得する。

| 用途 | guide (raw URL) |
|---|---|
| 登録・プロジェクト構成の全体像 | https://raw.githubusercontent.com/kitakkun/kotlin-compiler-plugin-skills/main/skills/kotlin-compiler-plugin/references/compiler-plugin-bootstrap/guide.md |
| 公式テストインフラ (diagnostic / box テスト)。本 skill の kctfork テストと併用 | https://raw.githubusercontent.com/kitakkun/kotlin-compiler-plugin-skills/main/skills/kotlin-compiler-plugin/references/compiler-plugin-testing/guide.md |
| Gradle plugin (`KotlinCompilerPluginSupportPlugin`) | https://raw.githubusercontent.com/kitakkun/kotlin-compiler-plugin-skills/main/skills/kotlin-compiler-plugin/references/gradle-plugin-integration/guide.md |
| 複数 Kotlin バージョン対応 (Step 4 と併読) | https://raw.githubusercontent.com/kitakkun/kotlin-compiler-plugin-skills/main/skills/kotlin-compiler-plugin/references/multi-version-kotlin-support/guide.md |

FIR / IR の個別 Extension (checker・宣言生成・call rewriting 等) の guide 一覧と使い分けは [external-kitakkun-skills.md](https://raw.githubusercontent.com/TBSten/skills/main/skills/kotlin-compiler-plugin-dev/references/external-kitakkun-skills.md) (`kotlin-compiler-plugin-dev` skill の reference) を参照。

### Step 4: Multi-Kotlin Version Support (上級、任意)

1 つの JAR で複数の Kotlin バージョン (例: 2.0.0 〜 2.4.x) をサポートしたい場合に実施する。

**戦略の選択**:
- **タンデムリリース** — プラグインバージョン = Kotlin バージョン。最もシンプル (kotlinx.serialization 方式)
- **独立リリース** — 1 JAR で多バージョン対応。以下 2 つのアーキテクチャがある:
  - **A: Source Set Separation** — Gradle がビルド時にソースディレクトリを切り替え。K1/K2 断絶の吸収に最適
  - **B: Compat Module Layer (metro スタイル)** — ServiceLoader で実装を動的選択。K2+ のパッチ差異の吸収に最適

詳細なセットアップ手順と全コード例は `references/multi-version-setup.md` を参照。API 差分の吸収方針は上記外部 guide の `multi-version-kotlin-support` を併読する。
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
- integration-test/test-gradle-plugin/ (Gradle TestKit E2E)
- .github/workflows/ (ci.yml / release.yml)

### 依存関係
- kotlin-compiler-embeddable: <version>
- auto-service + KSP
- kctfork: <version>
- kotest: <version>
- vanniktech maven-publish: <version>

### ビルド結果
- unit (compiler-plugin / gradle-plugin / runtime): [SUCCESS / FAILED]
- integration-test (jvm / kmp / gradle-plugin TestKit): [SUCCESS / FAILED]
- publishToMavenLocal: [SUCCESS / FAILED]

### 次のステップ
1. compiler-plugin/src/main/kotlin/ の <Prefix>Transformer (no-op skeleton) に IR 変換を実装、必要なら <Prefix>FirExtensionRegistrar に FIR checker を登録
2. runtime/src/commonMain/kotlin/ の placeholder (<Prefix>Marker) を公開 API に置き換える
3. compiler-plugin/src/test/ の <Prefix>CompilerTest と TestKit fixture にテストケースを追加
4. 公開前: gradle.properties の POM_* を埋め、Secrets を登録 (release.yml 冒頭コメント参照) して `vX.Y.Z` tag を push
```
