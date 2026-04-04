---
name: kotlin-compiler-plugin-dev
description: >
  Kotlin Compiler Plugin の開発・レビューを支援するスキル。
  30+ の既存プラグインの調査データ (Extension Point、設計パターン、ソースコード URL) を参照し、
  やりたいことに最適な Extension Point の選択、設計パターンの提案、前例のソースコード参照を行う。
  deepwiki MCP を使って最新のソースコードを深掘りする機能も持つ。
  Use when requested: "compiler plugin をレビュー", "compiler plugin の実装を評価",
  "review compiler plugin", "FIR/IR の設計をチェック",
  "Extension Point の選択は正しいか", "compiler plugin の前例を探して",
  "compiler plugin を開発したい", "この機能は FIR と IR どっちでやるべき？",
  "既存プラグインでこういうことやってるのある？",
  "compiler plugin の設計を相談したい".
---

# Kotlin Compiler Plugin Dev

Kotlin Compiler Plugin の開発・レビューを、既存プラグインの調査データを基に支援する。

## Step 0: deepwiki MCP の利用可否チェック

スキル起動時にまず deepwiki MCP が利用可能かチェックする。

```
ToolSearch で mcp__deepwiki__ask_question を検索
```

- **利用可能な場合**: 以降のステップで deepwiki MCP を詳細調査に使用する
- **利用不可の場合**: ユーザーに以下を案内する:

> deepwiki MCP が利用できません。deepwiki MCP を設定すると、既存プラグインのソースコードをリアルタイムで深掘りできます。
> 設定方法: https://deepwiki.com のドキュメントを参照してください。
> deepwiki なしでも references/ 内の調査データで十分な情報が得られますので、このまま続行します。

deepwiki がなくても references/ 内のデータで作業を続行する。

## Step 1: ユーザーの要件を把握

以下のいずれかを特定する:

1. **開発モード**: 新しい compiler plugin を作りたい / 既存プラグインに機能を追加したい
2. **レビューモード**: 既存の compiler plugin 実装をレビュー・評価したい
3. **調査モード**: やりたいことに対する前例を探したい

## Step 2: overview.md で前例を検索

`references/overview.md` を読み込み、ユーザーの要件に合致する前例を検索する。

### 検索の観点

- **やりたい機能** → テーブルの「実装されている機能」列でマッチするものを探す
- **使いたい Extension Point** → テーブルの「FIR or IR」列でフィルタ
- **同じパターンのプラグイン** → `references/patterns.md` のパターン分類を参照

該当するプラグインが見つかったら、ユーザーに前例一覧を提示する。

## Step 3: details/ で詳細を確認

Step 2 で見つかった前例について、`references/details/` 内の対応ファイルを読み込む。

| 対象プラグイン | 読むファイル |
|---|---|
| noarg, allopen, sam-with-receiver | `references/details/01-noarg-allopen-sam-with-receiver.md` |
| KSP, atomicfu | `references/details/02-ksp-atomicfu.md` |
| kotlinx.serialization | `references/details/03-kotlinx-serialization.md` |
| Koin, Anvil, Metro, MoshiX, Redacted | `references/details/04-koin-anvil-metro-moshix-redacted.md` |
| DebugLog, Zipline, Arrow Optics, back-in-time, Kondition, suspend-kontext, AspectK | `references/details/05-debuglog-zipline-arrow-kitakkun.md` |
| assign-plugin, jvm-abi-gen, kotlin-dataframe, scripting | `references/details/06-assign-jvm-abi-gen-dataframe-scripting.md` |
| lombok, compose | `references/details/07-lombok-compose.md` |
| parcelize, power-assert, kapt, js-plain-objects | `references/details/08-parcelize-power-assert-kapt-js-plain-objects.md` |

details ファイルには以下が含まれる:
- CompilerPluginRegistrar の登録内容
- 各 Extension の継承クラス・オーバーライドメソッド
- 具体的な動作の詳細
- 診断メッセージの一覧

## Step 4: deepwiki MCP で最新ソースコードを深掘り (任意)

deepwiki MCP が利用可能な場合、details/ の情報だけでは不十分なとき、deepwiki で最新のソースコードを確認する。

### 使い方

overview.md の Source code url 列にある GitHub リポジトリ情報から、deepwiki に質問する:

```
mcp__deepwiki__ask_question
  repo_name: "JetBrains/kotlin"  (または "google/ksp" 等)
  question: "How does FirNoArgConstructorGenerator implement getCallableNamesForClass and generateConstructors?"
```

### deepwiki を使うべきケース

- details/ に含まれる情報が古い可能性がある場合
- 特定のメソッドの最新実装を正確に確認したい場合
- details/ にない周辺コード (テスト、ユーティリティ等) を確認したい場合

### deepwiki を使わなくてよいケース

- Extension Point の選択や設計パターンの判断 → references/ で十分
- どのプラグインが参考になるかの特定 → overview.md で十分

## Step 5: 結果の出力

モードに応じて出力する。

### 開発モードの出力

```markdown
## Compiler Plugin 設計提案

### やりたいこと
- ...

### 推奨する Extension Point
| Extension Point | 役割 | 前例 |
|---|---|---|

### 推奨する設計パターン
- パターン: (1〜4 のいずれか)
- 理由: ...

### 最も参考にすべきプラグイン
- プラグイン名: ...
- 類似度: ...
- 参考ファイル: ... (URL)

### 実装ステップ
1. ...
2. ...
```

### レビューモードの出力

```markdown
## Compiler Plugin レビューレポート

### 概要
- プラグイン名: ...
- 使用 Extension Point: ...
- K2 対応: Yes/No

### Extension Point の選択
- [適切/要改善] FIR Extension: ...
- [適切/要改善] IR Extension: ...

### 設計パターン
- 採用パターン: パターン 1/2/3/4
- 前例との類似度: ... (最も近いプラグイン名)

### チェックリスト結果
(references/review-checklist.md のチェックリストの結果)

### 問題点・改善提案
1. ...
2. ...

### 前例参照
- 類似プラグイン: ... (URL)
- 参考にすべきファイル: ... (URL)
```

### 調査モードの出力

前例の一覧と、各前例の概要・ソースコード URL を提示する。
