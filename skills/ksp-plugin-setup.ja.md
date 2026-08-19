# KSP Plugin Setup スキル

[English](./ksp-plugin-setup.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

KSP plugin (Symbol Processor) プロジェクトを [cream.kt](https://github.com/TBSten/cream) が到達した
構成でスキャフォールドする [Claude Code](https://docs.anthropic.com/en/docs/claude-code) スキル。
3 モジュール構成・4 層の processor 設計・golden ファイルによるテスト基盤に加え、作った後も構成が
崩れないようにアーキテクチャルールを生成先に配置する。

インストール不要の一回限り版は
[`ksp-plugin-setup` プロンプト](../prompts/ksp-plugin-setup.ja.md) にある。

## クイックスタート

### 1. スキルをインストール:

```bash
gh skill install tbsten/skills ksp-plugin-setup
```

### 2. AI エージェントに依頼:

```
KSP プラグインのプロジェクトをセットアップして。
```

## セットアップされるもの

### モジュール構成

| モジュール | 説明 |
|---|---|
| `<project-name>-runtime/` | アノテーション宣言**のみ** (実行時ロジックゼロ)。だから KMP 全ターゲット。publish 対象 |
| `<project-name>-ksp/` | processor 本体。JVM only (KSP の制約)、`-Xcontext-parameters`。publish 対象 |
| `test/` | KMP 統合テスト。processor を実際に適用し、生成コードの振る舞いを全ターゲットで検証 |
| `buildLogic/` | root の version catalog を共有する included build。convention plugin は lint のみの最小構成 |

### processor の層

| 層 | 責務 |
|---|---|
| root (`ksp/*.kt`) | `SymbolProcessor` / `Provider` / `ProcessContext` の 3 ファイルのみ。オーケストレーションだけ |
| `feature/<name>/` | 1 注釈 = 1 ディレクトリ = 1 エントリ関数。発見 → 検証 → core 呼び出し |
| `core/<sub>/` | プロジェクト固有の生成ロジックと、leaf な `core/error` 例外階層 |
| `options/` | KSP option のモデルとパースを 1 データクラスに集約 |
| `util/` · `util/ksp/` | 汎用ヘルパのみ。直下は KSP API に依存しない |

依存は `feature → core → util` の一方向で、唯一の上向きが `feature → ProcessContext`。
feature 間の依存は禁止。

### テスト基盤

| コンポーネント | 説明 |
|---|---|
| kctfork | 実際の Kotlin + KSP コンパイルによる e2e テスト |
| facet 形式の golden | 入力 / options / ExitCode / コンソール出力 / 生成物を 1 Markdown ファイルに束ねる |
| generator 駆動 snapshot | KotlinPoet で組んだシナリオ family を option 軸と直積。1 点 = 1 テスト = 1 golden |
| 診断の golden | エラーメッセージ本文そのものを ExitCode と併せて固定 |
| Konsist | 層・root 許可ファイル・1 ファイル行数上限を import ベースで自動強制 |

### ビルド / CI

| ファイル | 説明 |
|---|---|
| `gradle/libs.versions.toml` | 自プロジェクトの version も含む SSoT |
| `gradle.properties` | configuration cache + build cache、`ksp.incremental=false` |
| `test/build.gradle.kts` | KSP × KMP workaround (`*Test` の ksp タスクを残す理由つき) |
| `.github/workflows/gradle.yml` | `matrix.include` で OS 最小化。concurrency / timeout つき |
| `.github/workflows/publish.yml` | GitHub Release `published` トリガ (pre-release でも発火) |

### 生成先に置かれるルール

`.claude/rules/*.md` を 5 ファイル配置する。path-scoped なので該当ファイルを触る時だけ読み込まれる:
アーキテクチャ依存テーブル、root / feature / core の配置ルール、テスト規約。Konsist テストと対に
なっているので、テーブルを変えたら同じコミットでテストも更新する。

## 主要コンセプト

### 診断は throw せず report する

ユーザーの誤用は `logger.error(message, ksNode)` + 直後の `return` で扱う。throw すると
clean な COMPILATION_ERROR が KSP の INTERNAL_ERROR になり、中途半端な生成物が残りうる。
全診断に「何が悪いか」だけでなく「どう直すか」を含める。

### 注釈ごとの差分を `when` で分岐しない

`GenerateSourceAnnotation` は意図的に sealed ではない。注釈の追加は実装ファイルを 1 つ足して
必要な rule だけ override するだけで済み、既存の分岐は一切触らない。プロパティ単位の rule は
独立した関数型として生成関数に通常の引数で渡す。

### 生成はトランザクショナル

書き出し口は 1 箇所に集約され、そこが package / import の boilerplate を所有する。本文は先に
buffer へ書き、空ならファイル自体を開かない (空の `package` + `import` ファイルはコンパイル対象に
残るぶん、ファイルが無いより悪い)。

### option の parse は lazy に行う

コンストラクタで parse すると、不正な option 値が意味のないメッセージの INTERNAL_ERROR になる。
`process()` の中で parse すれば、報告可能な COMPILATION_ERROR にできる。

## 関連スキル

- [`kotlin-maven-central-publish`](./kotlin-maven-central-publish.ja.md) — このスキルが意図的に
  重複させていない Maven Central 公開の完全手順 (GPG・secrets・Sonatype)
- [`kotlin-compiler-plugin-setup`](./kotlin-compiler-plugin-setup.ja.md) — symbol processor ではなく
  Kotlin **compiler** plugin (FIR/IR) を作る場合
