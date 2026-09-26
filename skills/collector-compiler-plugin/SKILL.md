---
name: collector-compiler-plugin
description: >
  Kotlin Compiler Plugin でモジュールを超えたオブジェクト収集 (コンパイル時 ServiceLoader) を
  実装するための scaffold を生成する。
  「Compiler Plugin でクラスを収集したい」「ServiceLoader のコンパイル時版を実装したい」
  「モジュール横断でアノテーション付きクラスを集めたい」「cross-module discovery を実装したい」
  「hint 関数パターンで compile-time aggregation を実装したい」といった要望に対応。
  Kotlin Compiler Plugin, KSP では実現できない cross-module 収集, FIR/IR Extension の実装が必要な場合に使用。
  Use when requested: "Compiler Plugin でクラスを収集したい", "ServiceLoader のコンパイル時版を実装したい",
  "モジュール横断でアノテーション付きクラスを集めたい", "cross-module discovery を実装したい",
  "hint 関数パターンで compile-time aggregation を実装したい".
metadata:
  status: Experimental
  group: Kotlin ライブラリ/ツール開発
---

## 概要

Kotlin K2 Compiler Plugin の「hint 関数パターン」を使い、複数モジュールにまたがるアノテーション付きクラスをコンパイル時に収集する仕組みの scaffold を生成する skill。

metro (ZacSweers/metro) と koin-compiler-plugin (InsertKoinIO/koin-compiler-plugin) で実績のあるパターンを汎用化している。

## 前提条件

- Kotlin 2.3.x+ (K2 compiler 必須)
- Gradle ベースのプロジェクト

## ワークフロー

### Step 1: ヒアリング

ユーザーに以下を確認する:

1. **プロジェクトタイプ**: KMP / Android / JVM
2. **収集対象**: どんなクラスを収集するか (例: API ハンドラ, プラグイン, Feature モジュール)
3. **グルーピング**: tag によるカテゴリ分けが必要か
4. **パッケージ名**: Plugin の base package (例: `com.example.collector`)
5. **アノテーション名**: マーカーアノテーションの名前 (デフォルト: `@Collectable` / `@CollectAll`)

### Step 2: 設計確認

ヒアリング結果を元に、以下の設計をユーザーに提示して確認:

- アノテーション名と引数
- hint 関数のパッケージ名 (`<base-package>.hints`)
- Gradle module 構成 (annotations / compiler-plugin / gradle-plugin)
- 生成ファイル一覧

### Step 3: コード生成

`references/architecture.md` を読み、アーキテクチャを理解した上で、`references/templates/` 内のテンプレートを参考にコードを生成する。

**重要:** テンプレートはそのままコピーせず、ユーザーのパッケージ名・アノテーション名・要件に合わせてカスタマイズすること。

#### 生成するモジュールとファイル

**annotations モジュール** (runtime dependency, KMP commonMain):
- マーカーアノテーション (参考: `references/templates/annotations.kt`)

**compiler-plugin モジュール** (compile-time only, JVM):
- `CompilerPluginRegistrar` (参考: `references/templates/registrar.kt`)
- FIR Extension: hint 関数スタブ生成 (参考: `references/templates/fir-generator.kt`)
- IR Extension: hint body 生成 + discovery + 集約 (参考: `references/templates/ir-extension.kt`)
- `resources/META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar`

**gradle-plugin モジュール**:
- Gradle Plugin (参考: `references/templates/gradle-plugin.kt`)
- `resources/META-INF/gradle-plugins/<plugin-id>.properties`

**ビルド設定**:
- 各モジュールの `build.gradle.kts` (参考: `references/templates/build-scripts.md`)

**テストコード**:
- `kotlin-compile-testing` による基本テスト (参考: `references/templates/test.kt`)

### Step 4: 動作確認ガイド

生成後、以下を案内する:

1. `./gradlew :compiler-plugin:test` でテストを実行
2. `./gradlew publishToMavenLocal` でローカルに publish
3. サンプルプロジェクトで `@Collectable` / `@CollectAll` を使って動作確認

## 技術的背景

詳細なアーキテクチャは `references/architecture.md` を参照。

### hint 関数パターンの要点

- FIR フェーズで `FirDeclarationGenerationExtension` を使い、固定パッケージに hint 関数のスタブを生成
- IR フェーズで hint 関数に body を追加し、`registerFunctionAsMetadataVisible()` で downstream モジュールに公開
- 消費側モジュールは `referenceFunctions(CallableId)` で hint 関数を検索し、パラメータ型から収集対象クラスを復元

## 注意事項

- Kotlin Compiler Plugin API は minor version 間で binary compatible ではないため、Kotlin バージョンアップ時に追従が必要
- K1 compiler では動作しない (K2 専用)
- `registerFunctionAsMetadataVisible()` と `referenceFunctions()` は Kotlin 公式に stable とマークされていないが、metro/koin の両方で使用されている実績あり
