# exploratory-pr-verification

Kotlin プロジェクト (Android / JVM / KMP / Compose / server-side Kotlin) の PR を、
複数 subagent 並列で探索的に検証するための運用規約。

## インストール

```sh
gh skill install tbsten/skills exploratory-pr-verification
```

## 概要

OSS の PR を「メンテナと並走しながら多角的に深掘り audit する」 ワークフローを skill として
パッケージ化したもの。 主管 (orchestrator) は各 iter 開始前に SKILL.md を再読し、
関連 section を各 subagent prompt に注入する。 5 並列 cat 完了後は §17 終了条件チェックを
機械的に走らせる。

## 使うべきタイミング

以下のような状況でこの skill を呼ぶ:

- 規模のある PR を複数 iter (典型: 5〜20 iter) にわたって深く audit したい
- **N 並列 subagent** (デフォルト 5) を category 別に kick して、 検証領域を分業したい
- PR スレッドを荒らさず、 番号付き ticket pool で発見を管理したい
- どのタイミングで **PR コメントを投稿**し、 どれを ticket だけに留めるか判断したい
- メンテナ反応 latency に追随して loop の cadence を調整したい
- deadline で clean に close したい (retrospective + cluster 分析を含む)

PR 差分ではなく **main** を 60 分 single-shot で見るだけなら、 兄弟 skill
[`exploratory-nightly-verification`](./exploratory-nightly-verification.ja.md) を使う。

## 5 つの category

| cat | 担当領域 | build 必要 |
|-----|---------|-----------|
| cat1 | ソースコード静的解析 | ✗ |
| cat2 | PR / 環境 / docs / CI | ✗ |
| cat3 | build / test (動的、 隔離 cache) | ✓ |
| cat4 | e2e / happy path (MCP 駆動) | ✓ |
| cat5 | 同分野ライブラリ比較 / 残角度 | ✗ または ✓ |

各 cat の angle 詳細と、 Kotlin プロジェクトのジャンル別 (Compose / KMP utility / DI /
annotation-processor / compiler plugin / server framework / test library / build tooling) の
比較対象ライブラリ一覧は
[`category-roles.md`](./exploratory-pr-verification/references/category-roles.md) を参照。

## Core constraint (他のすべての section より優先)

過去 phase で最も多くユーザに繰り返し指摘された 2 ルールを「上書き level」 に昇格:

- **A. ループは止めない** — 1 iter 完了通知後、 §17 終了条件 list を機械的に walk。 未達なら
  即次 iter kick。 「次どうしますか?」とユーザに聞き返してはいけない
- **B. ユーザ feedback は即 skill 反映** — 1 回でも新ルールを指摘されたら、 同じ turn 中に
  skill ファイルへ反映。 ad-hoc prompt 調整や session memory で済ませない

## 一時ディレクトリ規約

すべての一時ファイル / log / sandbox / ticket は以下に配置:

```
.local/tmp/exploratory-pr-<id>/
├── FINAL-SUMMARY.md
├── problems/<NNNN>-<slug>.md
├── log/iter<N>-cat<X>-*.log
├── gradle-isolation/cat<N>/
├── iter<N>-poc/           # PR コメント投稿前 PoC sandbox
└── iter<N>-cat<X>/sandbox/ # cat 専用 source 改変 sandbox
```

`<id>` は通常 PR 番号、 stacked PR を audit する場合は `186-187` のような range。

## 同梱スクリプト & テンプレート

loop の決定的な処理は script として同梱しており (`bash` / `git` / `jq` / `gh`)、
orchestrator と各 cat は script をそのまま実行する:

| Script / template | 役割 |
|-------------------|------|
| `scripts/init-exploration.sh <pr-id>` | 探索ディレクトリの scaffold + FINAL-SUMMARY.md 雛形 (cluster family 表込み) |
| `scripts/new-ticket.sh --cat N --severity P0..P3 --slug <slug> --id <pr-id>` | 5 並列 cat 横断のアトミック採番 (予約 range・renumber 不要) + ticket 雛形生成 |
| `scripts/pr-state.sh <pr>` | force-push 検出 / 新規 commit / メンテナ latency 帯域 / 自コメント数と 8/12 しきい値 / 未 resolve thread 数を 1 JSON で出力 |
| `templates/kick-prompts/cat1.md 〜 cat5.md` | 各 cat の subagent kick プロンプト雛形 — orchestrator は `<id>` と `<iter>` を差し込むだけ |

## References

詳細な運用規約は以下に分割:

- [`category-roles.md`](./exploratory-pr-verification/references/category-roles.md) — 各 cat の
  担当領域 / angle / MCP driver 対応表、 ジャンル別の比較対象ライブラリ一覧
- [`pdca-workflow.md`](./exploratory-pr-verification/references/pdca-workflow.md) — 動的検証の
  5 段階 PDCA cycle、 改変可能な設定ファイル surface、 出力確認手法
- [`ticket-format.md`](./exploratory-pr-verification/references/ticket-format.md) — 採番契約
  (script が採番)、 ディレクトリ構成、 ファイル format、 重要度 / 重複回避ルール
- [`pr-comment-policy.md`](./exploratory-pr-verification/references/pr-comment-policy.md) —
  投稿閾値、 cluster policy、 飽和上限、 投稿前必須 PoC、 自己訂正コメント format、
  latency 対応 cadence
- [`cluster-families.md`](./exploratory-pr-verification/references/cluster-families.md) — C-1 〜
  C-11 family 分類 (follow-up PR scope 設計用)
- [`retrospective-meta.md`](./exploratory-pr-verification/references/retrospective-meta.md) —
  過去 phase の指摘 table、 lag pattern 解釈、 table のメンテナンス方法

## 前提条件

- Git 管理された Kotlin / Gradle プロジェクト
- `gh` CLI 認証済み。 同梱 script 用に `jq` も必要
- `.local/` が `.gitignore` 済み
- 検証対象 surface に対応する MCP driver (web = Playwright、 Android = Maestro、
  IDE plugin = IntelliJ-MCP 等) — cat4 でのみ必要
- 主管が N 並列 subagent と background task を起動できる環境
