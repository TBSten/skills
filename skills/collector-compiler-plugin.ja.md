# Collector Compiler Plugin スキル

[English](./collector-compiler-plugin.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

Gradle モジュールを横断してアノテーション付きクラスをコンパイル時に収集する (コンパイル時 ServiceLoader) Kotlin K2 Compiler Plugin を、**hint 関数パターン** で scaffold する [Claude Code](https://docs.anthropic.com/en/docs/claude-code) スキル。

> このスキルは [TBSten/collector-compiler-plugin-skill](https://github.com/TBSten/collector-compiler-plugin-skill) から移管され、現在はこのリポジトリで管理している。

## クイックスタート

### 1. スキルをインストール:

```bash
gh skill install tbsten/skills collector-compiler-plugin
```

### 2. AI エージェントに依頼:

```
モジュール横断で @Collectable 付きクラスをコンパイル時に集めたい。
```

## 背景

[metro](https://github.com/ZacSweers/metro) や [koin-compiler-plugin](https://github.com/InsertKoinIO/koin-compiler-plugin) は、Kotlin Compiler Plugin で複数モジュールにまたがるアノテーション付きクラスをコンパイル時に収集・集約している。この仕組みは DI フレームワークに限らず汎用的に有用だが、Compiler Plugin の実装は参入障壁が高い。本スキルはこのパターンを汎用化し、自分のプロジェクトに同様の仕組みを導入できるようにする。KSP では実現できない cross-module 収集をカバーする。

## 動作の仕組み

1. **ヒアリング** — プロジェクトタイプ (KMP / Android / JVM)、収集対象、tag によるグルーピング、base package、アノテーション名
2. **設計確認** — アノテーション名と引数、hint パッケージ (`<base-package>.hints`)、モジュール構成、生成ファイル一覧
3. **コード生成** — `references/architecture.md` を読み、`references/templates/*` をパッケージ名・アノテーション名に合わせてカスタマイズ
4. **動作確認ガイド** — `./gradlew :compiler-plugin:test`、`publishToMavenLocal`、サンプルプロジェクトでの確認

### 生成されるモジュール

| モジュール | 説明 |
|---|---|
| `annotations/` | `@Collectable` / `@CollectAll` マーカーアノテーション (KMP、runtime 依存) |
| `compiler-plugin/` | `CompilerPluginRegistrar` + FIR / IR extension (JVM、compile-time only) |
| `gradle-plugin/` | Compiler plugin を適用する Gradle plugin |

テストは `kotlin-compile-testing` を使う。

### ユーザー向け API

```kotlin
@Collectable(tag = "api-handler")
class UserApiHandler : ApiHandler { ... }

// コンパイラが body を生成
@CollectAll(tag = "api-handler")
fun collectApiHandlers(): List<KClass<*>>
```

## 主要コンセプト

### hint 関数パターン

```
[Module A のコンパイル]
  1. FIR: @Collectable 付きクラスを検出 → hint 関数スタブ生成
  2. IR: hint 関数の body 生成 → registerFunctionAsMetadataVisible() で公開
  → JAR のメタデータに hint 関数が含まれる

[Module B のコンパイル (Module A に依存)]
  3. referenceFunctions(CallableId) で hint パッケージを検索
  4. hint 関数のパラメータ型から収集対象クラスを復元
```

| API | 役割 |
|---|---|
| `CompilerPluginRegistrar` | エントリポイント |
| `FirDeclarationGenerationExtension` | FIR で hint 関数スタブを生成 |
| `IrGenerationExtension` | IR で hint body 生成 + discovery |
| `registerFunctionAsMetadataVisible()` | hint 関数を downstream モジュールに公開 |
| `referenceFunctions(CallableId)` | downstream で hint 関数を検索 |

### metro vs koin

| 観点 | metro | koin |
|---|---|---|
| hint パッケージ | `metro.hints` | `org.koin.plugin.hints` |
| 関数名 | scope 名由来 (`appScope`) | prefix + metadata (`componentscan_*_single`) |
| メタデータ格納 | パラメータ型のみ | 関数名 + パラメータ名・型 |
| FIR の役割 | nested interface 生成 (DI 固有) | hint 関数スタブ生成 (汎用的) |
| 集約機能 | 除外・置換あり | なし (validation のみ) |
| 核心 API | **同じ** | **同じ** |

## 制約

- **Kotlin 2.3.x+ (K2 compiler) 必須** — K1 では動作しない
- Compiler Plugin API は minor version 間で binary compatible ではないため、Kotlin バージョンアップへの追従が必要
- `registerFunctionAsMetadataVisible()` / `referenceFunctions()` は stable ではないが、metro / koin の両方で使われている
