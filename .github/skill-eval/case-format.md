# eval の形式

skill-eval が使う eval の形式。テストケースは `claude plugin eval` (Claude Code CLI v2.1.269 以降) が読む形式で、
発火テストは skill-creator の `run_loop.py` が読む形式。`scripts/skill-eval/check-evals.sh --skill <name>` がこの形式を検証する。

```
skills/<name>/evals/
├── <case-name>/            # テストケース 1 件 = 1 フォルダ
│   ├── prompt.md           # 実行設定 (frontmatter) + ユーザーの依頼文
│   ├── case.yaml           # 名前と、良い結果の説明
│   ├── graders/            # 判定 1 つ = 1 ファイル
│   │   ├── 01-<few-words>.md
│   │   └── 02-<few-words>.md
│   ├── files/              # 入力ファイル (必要な時だけ)
│   └── place-files.sh      # files/ を作業ディレクトリに置く (必要な時だけ)
└── trigger-eval.json       # 発火テスト
```

## case 名 (フォルダ名)

英小文字・数字・ハイフンで、英字で始める。何を確かめるかが分かる数語 (例: `flags-negative-quantity`)。
`results` / `mocks` / `node_modules` / `files` (CLI の予約名) と `true` / `false` / `null` は使わない。

## prompt.md

frontmatter をこの形のまま書き、その後にユーザーが打つとおりの依頼文を書く。

```markdown
---
tags: [skill-eval]
max_turns: 20
timeout_seconds: 600
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

ユーザーの依頼文
```

- frontmatter に書けるキーは `tags` / `max_turns` / `timeout_seconds` / `allowed_tools` だけ。未知のキーがあると case が読み込めない
- frontmatter に自由文を書かない (途中の `---` で frontmatter が終わってしまう)。説明は case.yaml に書く
- `allowed_tools` に Bash を入れると、実行環境に sandbox (Linux は bubblewrap と socat) が必要になる。要らなければ入れない

## case.yaml

```yaml
schema_version: "1.1"
name: flags-negative-quantity
expected_outcome: |
  良い結果の説明
```

- `schema_version` は引用符付きの文字列 (数値として読まれると case が読み込めない)
- `name` はフォルダ名と同じ
- 入力ファイルを使う時は次を足す:

```yaml
context:
  scaffold_script: place-files.sh
```

## graders/NN-few-words.md

1 ファイルに検証できる文を 1 つ。番号で順序を保つ。frontmatter はこの形のまま (frontmatter の無いファイルは CLI に無視される)。

```markdown
---
type: llm
focus: trace
weight: 1
---

summary.md states a total revenue of 693.5
```

- `focus: trace` で、judge が最終メッセージだけでなく途中の操作も見られる
- weight を揃えると、CLI のスコアがそのまま pass rate になる
- grader の無い case は読み込みに失敗する
- 人が CLI 用に書いた他の type (`regex` / `file_exists` / `tool_used` / `tool_order` / `baseline`) はそのまま残す

## 入力ファイル

`files/` に置き、依頼文の最後に `Input files: orders.csv, notes.txt` の行を書き、次の `place-files.sh` を prompt.md の隣に置いて case.yaml の `context.scaffold_script` で指定する。

```bash
#!/usr/bin/env bash
set -eu
cp -R "$(dirname "$0")/files/." .
```

## trigger-eval.json

```json
[
  {"query": "ユーザーが打つ依頼文", "should_trigger": true},
  {"query": "似ているが別の作業の依頼文", "should_trigger": false}
]
```

- 20 件前後。`should_trigger` が true / false それぞれ 8 件以上 (check-evals.sh が検証する)、同じ query の重複は不可
- query は具体的に (パス・固有名詞・背景を含める)。短すぎる依頼は skill を使うまでもなく処理されるので発火テストにならない
- false は無関係な依頼ではなく、キーワードは被るが別の作業が必要な near-miss にする

詳しい考え方 (何をテストするか) は skill-creator の SKILL.md「Test Cases」「Description Optimization」も参考になる。
