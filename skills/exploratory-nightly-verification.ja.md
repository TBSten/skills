# exploratory-nightly-verification

Kotlin プロジェクトの `main` ブランチを 60 分 single-shot で探索的に検証する skill。

## インストール

```sh
gh skill install tbsten/skills exploratory-nightly-verification
```

## 概要

`anthropics/claude-code-action` を cron schedule で回す等、 nightly CI job から起動する想定。
cat1 → cat5 を sequential に walk して 60 分以内に発見を Markdown issue ファイルへ逐次書き出し、
最後に SUMMARY.md を生成する。 **PR への副作用は一切なし** (nightly は read-only)。

兄弟 skill [`exploratory-pr-verification`](./exploratory-pr-verification.ja.md) と異なり、
制約が大きく違う:

| | exploratory-pr-verification | exploratory-nightly-verification |
|--|----------------------------|---------------------------------|
| 対象 | 特定 PR の diff | 最新 `main` |
| 時間予算 | open-ended (deadline まで loop) | 60 分 single shot |
| 並列度 | 5 並列 subagent | 1 agent、 cat1 → cat5 sequential |
| 出力 channel | PR コメント + ticket | issue Markdown ファイルのみ |
| 副作用 | PR コメント投稿 | **なし** (read-only) |
| Loop / 再 kick | あり | なし |

## 使うべきタイミング

- nightly job で main の品質スイープを 60 分で回したい (PR には触らない)
- sequential な cat 網羅で十分 (この budget では並列は危険)
- 発見を即 disk に書き出し、 job timeout でも失わないようにしたい
- 下流 CI step で findings を Slack / Discord / GitHub Issue に流したい (この skill の出力
  format がその contract)

## 不変条件

| # | ルール |
|---|------|
| A | **発見ごとに即** `.local/tmp/exploratory-nightly-<date>/issues/<NN>-<slug>.md` を書き出す。 batch 化禁止 |
| B | **T=50min で新規探索を打ち切る。** 残り 10 分は整形 / 重複統合 / SUMMARY.md に使う |
| C | **issue ファイル名は zero-pad 連番** (`01-<slug>.md`, `02-<slug>.md`, …)。 飛び番号や重複禁止 |
| D | **プロジェクトへの副作用一切禁止**: PR コメント / Issue 起票 / push / branch 操作いずれも禁止 |

## カテゴリ (sequential 実行)

- **cat1 静的解析**: KDoc / Single source of truth / silent failure / 型設計 / TODO grep
- **cat2 CI ログ / 公開 API baseline / docs 整合**: nightly warning grep、 baseline drift、
  README / docs / CHANGELOG の整合
- **cat3 動的 build / test**: project の主要 `test` task (integration / docs サブビルドも、 ある場合)
- **cat4 上流 release 監視**: Kotlin / Compose Multiplatform / Gradle / AGP の release と
  `libs.versions.toml` の比較
- **cat5 比較 / 残角度**: 同分野ライブラリ audit、 残 TODO grep、 sample-app 起動 sanity、
  semver / BCP review、 locale / case folding edge case

詳細は [`categories.md`](./exploratory-nightly-verification/references/categories.md)。

## 同梱スクリプト

決定的な処理は script として同梱しており (`bash` / `git` / `curl` / `jq`)、 agent は毎晩
ロジックを再発明せず script をそのまま実行する:

| Script | 役割 |
|--------|------|
| `scripts/init-run.sh` | run ディレクトリの scaffold + 開始 epoch 記録 + `{"date","commit","dir","startedAt"}` 出力 |
| `scripts/new-issue.sh <catN> <P0..P3> <slug> [title]` | 次の `<NN>` をアトミックに採番 (並列安全・gapless) して issue 雛形を生成 |
| `scripts/summary.sh` | `issues/*.md` を集計して SUMMARY.md を生成 + format lint を JSON 報告 |
| `scripts/check-upstream.sh` | cat4 の上流ソースを取得して version drift 表 `[{"tool","latest","project","drift"}]` を出力 |

issue の採番・metadata 行・SUMMARY.md・上流 version 比較は script の責務。 AI は issue 本文の
記述と breaking change 判定だけを担う。

## Issue ファイル format

各発見は `scripts/new-issue.sh` で生成する (手書き禁止)。 パスは
`.local/tmp/exploratory-nightly-<date>/issues/<NN>-<slug>.md` で、 AI は本文 section だけを埋める:

```markdown
# <短いタイトル>

**Category**: cat<N> (<名前>)
**Severity**: P0 | P1 | P2 | P3
**Detected at commit**: <git rev-parse HEAD の先頭 12 文字>

## Reproduction
- ...

## Detail
<200 文字程度。 先頭が通知抜粋の対象>

## Fix proposal (optional)
- ...
```

完全仕様は [`issue-format.md`](./exploratory-nightly-verification/references/issue-format.md)。

## 通知連携 (任意)

skill は Markdown を書くだけで、 **通知 step は skill の scope 外**。
Slack / Discord / GitHub Issue 同期を欲しい場合は、 別 CI step が `.local/tmp/exploratory-nightly-<date>/issues/`
を `sort` で walk して通知 payload を生成する構成にする。
ファイル名 / metadata 行 / 見出しレベルを `issue-format.md` で厳格に縛っているのは、
この parsing を robust にするため。

## 前提条件

- Git 管理された Kotlin / Gradle プロジェクト (Android / JVM / KMP / Compose / server-side)
- CI 環境で `gh` CLI 利用可能。 同梱 script 用に `jq` + `curl` も必要
- `.local/` が `.gitignore` 済み
- skill を driving する CI workflow ファイル (例: `.github/workflows/nightly-checking.yml`) を
  schedule で回す
