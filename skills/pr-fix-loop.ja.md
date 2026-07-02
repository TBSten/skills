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
| 1 | 各 PR ブランチを、 自分の base の最新 (stack なら親ブランチ、 それ以外は default ブランチ) に rebase |
| 2 | (任意) ブランチが base より 10 commits 以上進んでいたら commit を整理 — 非対話で、 commit を絶対失わない |
| 3 | CI ステータス + review thread + issue コメントを一括取得 (両方ページング) |
| 4 | 失敗 check を分類し、 対応する `fix-ci-*` skill に委譲 |
| 5 | 未 resolve review thread (標準 resolve) と issue コメント (対応済マーク wrap) を処理 |
| 6 | stacked PR をトポロジカル順に rebase 連鎖 |
| 7 | 終了判定 — N 回連続「変化なし」 で終了 (デフォルト 5) |

## 設計メモ

- **静的な stack マップを持たない。** ブランチ / base / stack 関係は毎パス
  `gh pr view --json headRefName,baseRefName` から読むので、 reorder / rebase / rename に自動追従する
- **`fix-ci-*` は命名規約であって必須依存ではない。** 対応 skill が無ければ、 失敗 job の log を
  要約して報告するだけに留め、 勝手に手動修正へ走らない
- **安全第一。** rebase 衝突は abort して次パス送り (auto-merge しない)、 commit 整理は必ず
  バックアップを取り、 失敗時は復元する
- **チャタリング防止。** 「変化なし」 streak を永続化し、 `/loop` driver が無限に回り続けず
  きれいに止まる

## References

詳細ルールは progressive disclosure のため分割:

- [`failure-classification.md`](./pr-fix-loop/references/failure-classification.md) — Step 4 の CI
  失敗種別ヒューリスティクス (transient → lint → binary → test → build の順、 false-positive 注意点)
- [`review-handling.md`](./pr-fix-loop/references/review-handling.md) — Step 5 の review-thread /
  issue-comment の取得・修正・resolve、 対応済マーク wrap 規約、 ページング
- [`operations.md`](./pr-fix-loop/references/operations.md) — `gh` 前提、 owner/repo 導出、
  dirty worktree 中断、 log 取得タイミング、 rerun ブロック、 非対話 rebase、 クロスプラットフォーム
  `stat`、 commit 粒度

## プロジェクトの前提

例は Kotlin/Gradle の task 名 (`ktlintCheck`, `apiCheck`, `compileKotlin`, `jvmTest`) を使うが、
これはこの skill が鍛えられた環境というだけで、 Kotlin 固有の要素はない。 各自のプロジェクトの
lint / API-check / build / test の task 名に置き換えればよい。 分類ヒューリスティクスは汎用的な
job/log パターンをキーにしている。

## 前提条件

- GitHub 上に PR がある Git リポジトリ
- `gh` (GitHub CLI) 認証済み (`gh auth status`)
- `.local/` が `.gitignore` 済み (streak / backup マーカーがそこに置かれる)
- 無人 cadence には loop driver skill (例: `/loop`) との併用が最適
- 任意: 委譲先の project-local `fix-ci-*` skill 群 (lint / binary / build / test / pr-comments)
