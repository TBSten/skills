# CLAUDE.md

TBSten の Claude Code skills・rules コレクションリポジトリ。

## プロジェクト構成

```
.
├── README.md / README.ja.md    # スキル・ルール一覧 (HTML table 形式)
├── skills/
│   ├── <skill-name>.md          # スキル詳細ドキュメント (英語)
│   ├── <skill-name>.ja.md       # スキル詳細ドキュメント (日本語)
│   └── <skill-name>/
│       ├── SKILL.md             # スキル本体 (frontmatter 付き)
│       ├── *.md                 # 参照ドキュメント
│       └── example/             # サンプルコード
├── rules/
│   ├── install.sh               # ルールインストールスクリプト
│   ├── install/                 # 短縮URL用 Cloudflare Worker (rules.tbsten.me/i)
│   ├── <rule-name>.md           # ルール詳細ドキュメント (英語)
│   ├── <rule-name>.ja.md        # ルール詳細ドキュメント (日本語)
│   └── <rule-name>/
│       ├── RULE.md              # ルール本体 (.claude/rules/<rule-name>.md として配置される)
│       └── **/*                 # 参照ファイル (カレントディレクトリに配置される)
└── prompts/
    ├── <prompt-name>.md         # プロンプト詳細ドキュメント (英語)
    ├── <prompt-name>.ja.md      # プロンプト詳細ドキュメント (日本語)
    └── <prompt-name>/
        └── PROMPT.md            # プロンプト本体 (raw URL 参照で一回限り実行される)
```

## Skills の構成ルール

- `skills/<skill-name>/SKILL.md` がスキルのエントリポイント
- SKILL.md には YAML frontmatter (`name`, `description`, `metadata.status`) を含める
- 参照ドキュメントやサンプルコードは同ディレクトリ内に配置
- `skills/<skill-name>.md` / `<skill-name>.ja.md` で詳細ドキュメントを用意
- インストール: `gh skill install tbsten/skills <skill-name>`
- skill の変更を main に取り込んだら、ローカルにインストール済みのコピー (`~/.claude/skills/<skill-name>`) があるか確認し、あれば同期する (再インストール or `scripts/` `assets/` `references/` の rsync)。リポジトリが SSoT で、同期を忘れると修正が実行時に効かない

## Rules の構成ルール

- `rules/<rule-name>/RULE.md` がルール本体
- RULE.md 以外のファイルは参照ファイルとしてユーザーのカレントディレクトリに配置される
- サブディレクトリのネストも可能 (再帰的にダウンロードされる)
- `rules/<rule-name>.md` / `<rule-name>.ja.md` で詳細ドキュメントを用意
- status は英語詳細ドキュメント `rules/<rule-name>.md` の frontmatter に記載 (RULE.md には書かない)
- インストール: `curl -fsSL https://rules.tbsten.me/i | bash -s -- <rule-name>`

## Prompts の構成ルール

- `prompts/<prompt-name>/PROMPT.md` がプロンプト本体。skill と違いインストール不要の一回限りのプロンプトで、README の実行プロンプトから raw URL (`https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/<prompt-name>/PROMPT.md`) を参照させて実行する
- PROMPT.md には frontmatter を付けない (raw URL で直接取得・実行されるため)
- 参照ファイルは prompts/ にコピーせず、既存 skill 等のファイルを raw.githubusercontent.com URL で参照する (多数の場合は sparse clone を案内)
- `prompts/<prompt-name>.md` / `<prompt-name>.ja.md` で詳細ドキュメントを用意
- status / group は英語詳細ドキュメント `prompts/<prompt-name>.md` の frontmatter に記載 (PROMPT.md には書かない)
- 追加・更新は `.claude/skills/contribute-prompt.md` スキルで行う (既存 skill のプロンプト化手順もそこに記載)

## AI 責務最小化

- 各 skill / rule / prompt は、実行時に AI が判断する範囲を最小限にする。決定的な作業 (scaffold・コピー・置換・コマンド列・検証) は script に寄せる
- 判断基準・種別ごとの script 同梱方法・PR 前チェックリストは `.claude/skills/contribute-{skill,rule,prompt}.md` の「AI 責務最小化 (script 化)」セクションを正とする

## Status

各 skill / rule / prompt には成熟度を表す status を持たせる。

| status | 絵文字 | 意味 |
|---|---|---|
| `WIP` | 🌱 | 作成中だけど一旦出してみた |
| `Experimental` | 🧪 | 使えるはずだが、しっかり検証はされていない |
| `Active` | ✅ | プロダクションレディで実用的に使える |
| `Active-Prime` | 💎 | Active かつ定番として愛用している |
| `Archived` | ❌ | 役目を終えた・メンテナンスされていない |

- **SSoT**: skill は `SKILL.md` の `metadata.status`、rule / prompt はそれぞれ `rules/<name>.md` / `prompts/<name>.md` の frontmatter `status`
- README.md / README.ja.md の Status 列は SSoT のミラー (絵文字+ラベル表示)。必ず一致させる
- `Archived` の skill / rule / prompt は README の通常テーブルには載せず、🤝 Contribute セクション下の `<details>` (Archived 一覧) に移動する
- 新規追加時のデフォルトは `Experimental`
- status の変更は `.claude/skills/change-status.md` スキルで行う
- `as=<name>` オプションで保存名を変更可能

## Group

各 skill / rule / prompt には所属グループを持たせ、README の一覧テーブルはグループごとにまとめて表示する。

| 絵文字 | グループ (ja) | Group (en) |
|---|---|---|
| 🔴 | タスク管理 | Task Management |
| 🟢 | Kotlin / Android アプリ開発 | Kotlin / Android App Development |
| 🟣 | Kotlin ライブラリ/ツール開発 | Kotlin Library / Tool Development |
| 🔵 | Web フロントエンド | Web Frontend |
| ⚫️ | Git / GitHub | Git / GitHub |

- **SSoT**: skill は `SKILL.md` の `metadata.group`、rule / prompt はそれぞれ `rules/<name>.md` / `prompts/<name>.md` の frontmatter `group`。いずれも日本語グループ名で記載する (絵文字は付けない)
- README.md / README.ja.md の一覧テーブル先頭の Group / グループ列は SSoT のミラー (英語版は上表の英語名で表示)。必ず一致させる
- グループセルは各グループの **先頭行のみ** `絵文字 + 半角スペース + グループ名` (例: `🔴 タスク管理`) を記載し、同グループの 2 行目以降は空セル `<td></td>` にする
- テーブルの行は上表のグループ順に並べ、同じグループの行は隣接させる (行の追加は該当グループのまとまりの中に挿入する)
- 新規追加時は既存グループから選ぶ。適切なグループが無ければ上表に新グループを追加した上で使う
- 🤝 Contribute セクションの skill (contribute-skill / contribute-rule / contribute-prompt) はグループ対象外
