# pr-fix-loop

**複数の GitHub PR を並行して green に持っていく** 1 ループ分の運用スキル。 CI 失敗の切り分けと
review コメント対応を、 1 回の機械的な sweep でまとめて処理する。

## インストール

```sh
gh skill install tbsten/skills pr-fix-loop
```

## 概要

「複数 PR を寝かせている間も進める」 ワークフローを skill 化したもの。 PR を 1 本ずつ子守りする
のではなく、 1 回の呼び出しで PR list 全体を sweep し、 `/loop` driver で一定間隔ごとに繰り返す。
各パスで CI ステータスを取得し、 失敗 check をすべて分類して対応する `fix-ci-*` skill に委譲、
review / issue コメントを取得 → 修正 → commit → push → resolve まで処理し、 stack した PR を
トポロジカル順に rebase する。

## 使うべきタイミング

以下をしたいときにこの skill を呼ぶ:

- **複数 PR** (stack / 独立どちらも) を、 1 本ずつ手を掛けずに green にしたい
- 失敗 check (transient infra / lint / binary-compat / build / test) を自動分類して、 適切な
  fixer にルーティングしたい
- review コメントを取得 → 修正 → commit → push → resolve まで一貫して処理したい
- **stacked PR** を、 親 push 後に子を自動 rebase して整合させたい
- loop driver で **無人実行** したい (例: `/loop 10m /pr-fix-loop 179 180 181`)。 N 回連続で
  「変化なし」 なら終了する

**単一 PR** でも、 CI fix と review コメント対応をワンセットで自動化したい文脈なら合う。

## 1 パス = 7 ステップ

| Step | 内容 |
|------|------|
| 1 | `scripts/rebase-pass.sh` — 各 PR ブランチを、 自分の base の最新 (stack なら親ブランチ、 それ以外は default ブランチ) に rebase |
| 2 | (任意) ブランチが base より 10 commits 以上進んでいたら commit を整理 — 非対話で、 commit を絶対失わない |
| 3 | `scripts/fetch-pr-state.sh` — CI ステータス + review thread + issue コメントを一括取得 (両方フルページング) |
| 4 | 失敗 check ごとに `scripts/classify-failure.sh` → 対応する `fix-ci-*` skill に委譲 |
| 5 | 修正後、 `scripts/resolve-thread.sh` (標準 resolve) / `scripts/mark-comment-handled.sh` (対応済マーク wrap) |
| 6 | `scripts/rebase-pass.sh` を再実行して stacked PR をトポロジカル順に rebase 連鎖 |
| 7 | `scripts/streak.sh` — N 回連続「変化なし」 で終了 (デフォルト 5) |

## Scripts (決定的処理の中核)

機械的な処理 — ページング・失敗パターン判定・rebase の段取り・streak 永続化 — はすべて
`pr-fix-loop/scripts/` 配下の実行可能 script に実装されていて、
`${CLAUDE_SKILL_DIR}/scripts/<name>` で呼び出す。 AI は script を**そのまま実行**して 1 行 JSON の
出力を読むだけ (stderr には進捗と「何が・なぜ・どう直すか」のエラーメッセージが出る)。 script の
ロジックを転記・再実装しない — それがページング済みデータを silent に取りこぼす原因だった。
全 script は冪等で、 `GH_CMD` 環境変数によるモック差し替えでテスト可能。 依存は `git` + `gh` + `jq`。

| Script | 1 行説明 |
|--------|----------|
| `fetch-pr-state.sh <pr...>` | preflight + PR ごとの状態 (失敗 check / thread / コメント) を JSON 配列で |
| `classify-failure.sh <runId> <jobId>` | job ログ → `{kind, taskName, evidence, delegate, logTail}` |
| `rebase-pass.sh <pr...>` | トポロジカル順 rebase sweep → `{pr, action: rebased\|clean\|conflict-deferred}` |
| `resolve-thread.sh <threadId> [commit]` | review thread を 1 件 resolve |
| `mark-comment-handled.sh <id> <commit...>` | issue コメント 1 件を対応済マークで wrap |
| `streak.sh <no-change\|changed>` | streak を永続化 → `{streak, limit, terminate}` |

## 設計メモ

- **機械的処理の SSoT は script。** 分類パターン・ページング・rebase の順序は散文でなくコードで
  持つ — AI の責務は JSON を読む → `fix-ci-*` へ委譲 → fix を書く → 報告する、 に収束する
- **静的な stack マップを持たない。** ブランチ / base / stack 関係は毎パス (script 内部で)
  `gh pr view --json headRefName,baseRefName` から読むので、 reorder / rebase / rename に自動追従する
- **`fix-ci-*` は命名規約であって必須依存ではない。** 対応 skill が無ければ、 失敗 job の log を
  要約して報告するだけに留め、 勝手に手動修正へ走らない
- **安全第一。** rebase 衝突は abort して次パス送り (auto-merge しない)、 commit 整理は必ず
  バックアップを取り、 失敗時は復元する
- **チャタリング防止。** 「変化なし」 streak を永続化し、 `/loop` driver が無限に回り続けず
  きれいに止まる

## References

詳細ルールは progressive disclosure のため分割:

- [`failure-classification.md`](./pr-fix-loop/references/failure-classification.md) —
  `classify-failure.sh` の判定仕様の解説 (transient → lint → binary → test → build → unknown の順、
  job 名でなく task 名が真実、 false-positive 注意点)
- [`review-handling.md`](./pr-fix-loop/references/review-handling.md) — Step 5 の規約:
  対応済マーク wrap、 commit と thread の対応付け、 論点別 commit、 見落とし防止
- [`operations.md`](./pr-fix-loop/references/operations.md) — script の契約 (preflight /
  stdout 1 行 JSON / 冪等性)、 log 取得タイミング、 rerun ブロック、 非対話 rebase、
  クロスプラットフォーム `stat`、 commit 粒度、 ページングの背景

## プロジェクトの前提

例は Kotlin/Gradle の task 名 (`ktlintCheck`, `apiCheck`, `compileKotlin`, `jvmTest`) を使うが、
これはこの skill が鍛えられた環境というだけで、 Kotlin 固有の要素はない。 各自のプロジェクトの
lint / API-check / build / test の task 名に置き換えればよい。 分類ヒューリスティクスは汎用的な
job/log パターンをキーにしている。

## 前提条件

- GitHub 上に PR がある Git リポジトリ
- `gh` (GitHub CLI) 認証済み (`gh auth status`) と `jq` (script が両方チェックする)
- `.local/` が `.gitignore` 済み (streak / backup マーカーがそこに置かれる)
- 無人 cadence には loop driver skill (例: `/loop`) との併用が最適
- 任意: 委譲先の project-local `fix-ci-*` skill 群 (lint / binary / build / test / pr-comments)
