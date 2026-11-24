# watch: 記録が増えたらすぐ loop に回す

Inbox を見張り、新しい記録が保存されたら 1 件ごとに知らせる（またはコマンドを実行する）。人がアプリで送った直後に、Claude Code が対応を始められるようにするためのもの。中身を読む・archive する・分類するのは今までどおり loop（`loop.md`）で、watch は「増えた」を知らせるだけ。

検証状況: node 25 の `node:test` 8 件（ファイル・HTTP の受け口・`--exec`・`--once`・`--timeout`・書きかけを拾わない）。実際の受け口（`core/feedback-dev-server.mjs`）に POST して、ファイルの Inbox と `--url` の両方で `NEW <id>` が出て `--exec` が動くことを確かめた。

## 使い方

```bash
# ファイルの Inbox（導入先のルートで）
node "${CLAUDE_SKILL_DIR}/scripts/watch-inbox.mjs" feedback
# 別マシンの受け口（HTTP の Sink）
node "${CLAUDE_SKILL_DIR}/scripts/watch-inbox.mjs" --url http://127.0.0.1:7373/__feedback
```

- 出力は 1 件ごとに `NEW <id> <ref>`。`--json` で JSON の 1 行。
- 起動時に既にある記録は出さない（`--include-existing` で出す）。直下の `yymmdd-xx.md` だけを数え、`archived/` などは見ない。
- 大きさが `--settle`（既定 1000ms）変わらなくなってから出す。0 バイトの記録は出さない（書きかけ・作成直後）。
- `--once`: 新しい記録を見つけたら、その回の分を出して終わる。
- `--timeout <秒>`: その間に新しい記録が無ければ `TIMEOUT` を出して終わる（終了コード 3）。loop の終了判定（`loop.md` §2 の 6）で使う。
- `--exec "<コマンド>"`: 1 件ごとに `sh -c` で実行する。1 件ずつ順番で、終わるまで次を始めない。環境変数は `FEEDBACK_ID`・`FEEDBACK_REF`・`FEEDBACK_FILE`（ファイルのときだけ）。
- script は読解・書き換えせず、そのまま実行する。

## Claude Code から使う

1. **会話の中で待つ（推奨）**: オーケストレータが watch をバックグラウンドで動かし、`NEW` の行で起こしてもらう（Monitor や背景実行の通知など、その環境の仕組みを使う。会話を止めて sleep しない）。起きたら `loop.md` §2 の手順をその 1 件で回す。人が見ている会話の中で、分類・承認・archive の決まりがそのまま守られる。
2. **`--once` を繰り返す**: `node watch-inbox.mjs feedback --once` が終わったら loop を回し、また `--once` で待つ。
3. **`--exec` で別のプロセスを起こす**: 例 `--exec 'claude -p "feedback-mode の loop で $FEEDBACK_ID を対応して"'`。人の確認なしに動くので、使う前に人の承認を取り、`claude -p` の権限（書き込める範囲・外部への送信）を絞る。archive・設計の判断は人のいる会話に残す（`loop.md` §1）。

## 注意

- 監視しているのは保存されたかだけで、受け口が止まっている（`--url` で一覧が取れない）ときは stderr に出して見張りを続ける。
- 開発中だけ動かす。止めるのは Ctrl+C（SIGINT / SIGTERM）。
