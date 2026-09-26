# Kotlin Compiler Plugin Setup スキル

[English](./kotlin-compiler-plugin-setup.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

Kotlin Compiler Plugin のマルチモジュールプロジェクトを buildSrc、ユニットテスト、インテグレーションテストを含めて一式セットアップする [Claude Code](https://docs.anthropic.com/en/docs/claude-code) スキル。

## クイックスタート

### 1. スキルをインストール:

```bash
gh skill install tbsten/skills kotlin-compiler-plugin-setup
```

### 2. AI エージェントに依頼:

```
Kotlin compiler plugin のプロジェクトをセットアップして。
```

## 動作の仕組み

スキルは `scripts/scaffold.sh` を実行する。この script は `example/` の完全なプロジェクト skeleton — build ファイルに加え、実パッケージ `com.example.compilerpluginsetup` + `Example` クラス prefix で書かれた Kotlin ソース (CommandLineProcessor、CompilerPluginRegistrar、IR/FIR extension、no-op transformer、kctfork テストヘルパー、integration test のエントリポイント) — をコピーし、プロジェクト名・パッケージ・Group ID・Plugin ID に置換・rename する。エージェントは script をそのまま実行し (ドキュメントからのコード転記はしない)、生成結果をレビューしてビルドを確認する。

## セットアップされるもの

### プロジェクト構成

| モジュール | 説明 |
|---|---|
| `buildSrc/` | Convention plugins: `kotlin-jvm` (JDK 21 toolchain で Java 17 bytecode、JUnit5 + テストログ設定) と `publish` (vanniktech maven-publish、鍵がある時だけ署名) |
| `compiler-plugin/` | Compiler plugin 本体 (AutoService + KSP、FIR + IR extension、`enabled` CLI オプションを配線済み) |
| `gradle-plugin/` | Gradle plugin ラッパー (KotlinCompilerPluginSupportPlugin)。`Property` + `convention()` の DSL、fail-fast ガード、利用側 Kotlin 版ガード、生成 `BuildConfig` |
| `runtime/` | Kotlin Multiplatform API 宣言 (placeholder の marker 1 つ) |
| `integration-test/test-jvm/` | JVM 単体のエンドツーエンドテスト (`kotlinCompilerPluginClasspath`) |
| `integration-test/test-kmp/` | KMP (JVM + JS) のエンドツーエンドテスト (`kotlinCompilerPluginClasspath`) |
| `integration-test/test-gradle-plugin/` | Gradle TestKit E2E。fixture ビルドが `plugins { id(...) }` で適用し、`includeBuild` + `dependencySubstitution` で本リポジトリから解決する |
| `.github/workflows/` | `ci.yml` (PR のみ cancel、paths-ignore、失敗時 reports upload、`final` 集約 job) と `release.yml` (tag push、tag と版の一致チェック、Maven Central、GitHub Release) |

### ビルド設定

| ファイル | 説明 |
|---|---|
| `gradle.properties` | `GROUP` / `VERSION_NAME` / plugin ID / 対応 Kotlin 範囲 / `POM_*` の SSoT と、ビルド高速化設定 (caching、configuration cache、parallel、KT-82395 回避) |
| `build.gradle.kts` | gradle.properties から `allprojects { group; version }` を設定 |
| `settings.gradle.kts` | マルチモジュール構成 + foojay toolchain resolver |
| `gradle/libs.versions.toml` | Version catalog (Kotlin, KSP, AutoService, kctfork, Kotest, maven-publish) |
| `buildSrc/build.gradle.kts` | kotlin-dsl + 共有 version catalog |

### テスト基盤

| コンポーネント | 説明 |
|---|---|
| kctfork (KotlinCompilation) | インメモリコンパイルによるユニットテスト |
| Kotest (FunSpec) | テストフレームワーク (JUnit5 ランナー) |
| Integration test (JVM / KMP) | `kotlinCompilerPluginClasspath` を使うモジュール |
| Gradle plugin sanity | `ProjectBuilder` による DSL 既定値・runtime 依存の配線テスト |
| Gradle plugin E2E | Gradle TestKit + fixture プロジェクト (利用者と同じ書き方のビルド。publish 不要) |

## 主要コンセプト

### Plugin 登録

Compiler plugin は `META-INF/services/` ファイルで登録される (`@AutoService` で自動生成):
- `CommandLineProcessor` — Plugin ID を宣言
- `CompilerPluginRegistrar` — FIR + IR extension を登録

### FIR vs IR

- **FIR (Frontend)** — バリデーション、正確な行番号でのエラー報告
- **IR (Backend)** — バイトコード/JS/Native 出力に影響する実際のコード変換

### Unit Test パターン

kctfork でインメモリコンパイルし、リフレクションで結果を検証:
- `compile(source)` — プラグイン登録済みでコンパイル
- `shouldCompileOk()` — コンパイル成功を検証
- `loadTopLevelField(name)` — クラスローダー経由でフィールド値を取得

## 関連

- [kitakkun/kotlin-compiler-plugin-skills](https://github.com/kitakkun/kotlin-compiler-plugin-skills) (MIT, kitakkun 作) — scaffold 後、bootstrap・公式テストインフラ (diagnostic / box テスト。kctfork と併用)・Gradle plugin 連携・複数バージョン対応の guide を参照する。内容はコピーせず raw URL で参照し、ローカルにインストール済みならそちらを優先する
- [kotlin-compiler-plugin-dev](./kotlin-compiler-plugin-dev.ja.md) — 30+ プラグインの前例調査、FIR/IR Extension 向け kitakkun guide の全トピック対応表、サポート Kotlin バージョンの追加・削除
