---
name: kotlin-compiler-plugin
description: >
  Kotlin Compiler Plugin (K2 / FIR + IR) の新規セットアップ・開発・レビュー・複数 Kotlin バージョン対応・
  モジュール横断のコンパイル時収集をまとめて扱う。新規プロジェクトは scaffold script で
  compiler-plugin / gradle-plugin / runtime / kctfork テスト / integration-test 一式を生成。
  開発・レビューでは 30+ 既存プラグインの前例から Extension Point と設計パターンを提案し、
  compat module / source set separation でサポート Kotlin バージョンを追加・削除する。
  hint 関数パターンで別モジュールのアノテーション付きクラスを集める scaffold も作る。
  compiler plugin / FIR / IR / KotlinCompilerPluginSupportPlugin / compat module が話題に出たら必ず使う。
  Use when requested: "Kotlin compiler plugin を作りたい", "compiler plugin のプロジェクトをセットアップ",
  "compiler plugin の unit test を書きたい", "compiler plugin をレビュー", "FIR と IR どっちでやるべき？",
  "compiler plugin の前例を探して", "Extension Point の選択は正しいか", "Kotlin X.Y.Z をサポートしたい",
  "compat module を追加したい", "CI matrix に Kotlin を追加", "kctfork を更新したい",
  "新しい Kotlin パッチで NoSuchMethodError が出た", "タンデム vs 独立リリース",
  "Compiler Plugin でクラスを収集したい", "ServiceLoader のコンパイル時版", "cross-module discovery を実装したい".
metadata:
  status: Experimental
  group: Kotlin ライブラリ/ツール開発
---

# Kotlin Compiler Plugin

Kotlin Compiler Plugin の作業を 3 つのモードに振り分け、モードごとの guide (`references/<mode>/guide.md`) に従って進める。

## Step 1: モードを決めて guide を読む

ユーザーの依頼から当てはまるモードを選び、**その guide だけ** を読んで手順に従う。他モードの guide は、guide 内で参照されたときに読む。

| モード | こういう依頼 | 読む guide |
|---|---|---|
| **setup** | compiler plugin のプロジェクトを新規に作る (compiler-plugin / gradle-plugin / runtime / kctfork テスト / integration-test / CI・release)。1 JAR で複数 Kotlin 版に対応する基盤を最初から入れる | `references/setup/guide.md` |
| **dev** | 既存 / 構想中の plugin の設計相談 (FIR と IR どちらか、どの Extension Point か)、前例調査、実装レビュー、サポート Kotlin バージョンの追加・削除、版差分による失敗 (NoSuchMethodError 等) の対処 | `references/dev/guide.md` |
| **collector** | 複数モジュールに散ったアノテーション付きクラスをコンパイル時に集める (コンパイル時 ServiceLoader、hint 関数パターン、KSP では見えない cross-module 収集) | `references/collector/guide.md` |

判断に迷う場合:

- 「収集」が目的なら、新規プロジェクトでも **collector** を選ぶ (専用のモジュール構成を作るため setup の scaffold は使わない)
- 新規作成と同時に設計相談がある場合は、**setup** で土台を作ってから **dev** の Step 2 以降で Extension Point を決める
- どれにも当てはまらない (KSP processor、普通の Kotlin ライブラリ、アプリ側の `kotlin("plugin.xxx")` 設定など) なら、この skill は使わない

## 同梱リソース

パスはすべてこの SKILL.md があるディレクトリ (`${CLAUDE_SKILL_DIR}`) 基準。

| パス | 使うモード | 内容 |
|---|---|---|
| `references/setup/` | setup | guide と、登録クラス / Gradle plugin / テスト / publish / multi-version 基盤の解説 |
| `scripts/scaffold.sh` | setup | `example/` をコピー・置換してプロジェクト一式を生成する。**読解・書き換え・再実装せず、そのまま実行する** |
| `example/` | setup | scaffold のコピー元 (完全なプロジェクト skeleton) |
| `references/dev/` | dev | guide と、前例調査データ (`overview.md` / `patterns.md` / `details/`)、レビュー観点、複数バージョン対応の実運用パターン、外部 guide (kitakkun) の対応表 |
| `assets/` | dev | 対象プロジェクトへコピーするテスト基盤 (`scripts/compiler-plugin-test.sh` / `supported-kotlin-versions.txt` / `smoke-test.sh` / `workflows/compiler-plugin-test.yml`) |
| `references/collector/` | collector | guide と、hint 関数パターンのアーキテクチャ、コードテンプレート (`templates/`) |
