# Kotlin Compiler Plugin Setup スキル

[English](./kotlin-compiler-plugin-setup.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

Kotlin Compiler Plugin のマルチモジュールプロジェクトを buildSrc、ユニットテスト、インテグレーションテストを含めて一式セットアップする [Claude Code](https://docs.anthropic.com/en/docs/claude-code) スキル。

## クイックスタート

### 1. スキルをインストール:

```bash
npx skills add tbsten/skills \
  --skill kotlin-compiler-plugin-setup
```

### 2. AI エージェントに依頼:

```
Kotlin compiler plugin のプロジェクトをセットアップして。
```

## セットアップされるもの

### プロジェクト構成

| モジュール | 説明 |
|---|---|
| `buildSrc/` | Convention plugins (kotlin-jvm、JUnit5 + テストログ設定) |
| `compiler-plugin/` | Compiler plugin 本体 (AutoService + KSP、FIR + IR extension) |
| `gradle-plugin/` | Gradle plugin ラッパー (KotlinCompilerPluginSupportPlugin) |
| `runtime/` | Kotlin Multiplatform API 宣言 |
| `integration-test/test-jvm/` | JVM 単体のエンドツーエンドテスト (`kotlinCompilerPluginClasspath`) |
| `integration-test/test-kmp/` | KMP (JVM + JS) のエンドツーエンドテスト (`kotlinCompilerPluginClasspath`) |

### ビルド設定

| ファイル | 説明 |
|---|---|
| `settings.gradle.kts` | マルチモジュール構成 + foojay toolchain resolver |
| `gradle/libs.versions.toml` | Version catalog (Kotlin, KSP, AutoService, kctfork, Kotest) |
| `buildSrc/build.gradle.kts` | kotlin-dsl + 共有 version catalog |

### テスト基盤

| コンポーネント | 説明 |
|---|---|
| kctfork (KotlinCompilation) | インメモリコンパイルによるユニットテスト |
| Kotest (FunSpec) | テストフレームワーク (JUnit5 ランナー) |
| Integration test (JVM) | JVM 単体の `kotlinCompilerPluginClasspath` application モジュール |
| Integration test (KMP) | KMP (JVM + JS) の `kotlinCompilerPluginClasspath` モジュール |

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
