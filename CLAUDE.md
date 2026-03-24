# CLAUDE.md

TBSten の Claude Code skills・rules コレクションリポジトリ。

## プロジェクト構成

```
.
├── README.md / README.ja.md    # スキル・ルール一覧 (HTML table 形式)
├── skills/
│   └── <skill-name>/
│       ├── SKILL.md             # スキル本体 (frontmatter 付き)
│       ├── *.md                 # 参照ドキュメント
│       └── example/             # サンプルコード
├── rules/
│   ├── install.sh               # ルールインストールスクリプト
│   └── <rule-name>/
│       ├── RULE.md              # ルール本体 (.claude/rules/<rule-name>.md として配置される)
│       └── **/*                 # 参照ファイル (カレントディレクトリに配置される)
├── <skill-name>.md              # スキル詳細ドキュメント (英語)
└── <skill-name>.ja.md           # スキル詳細ドキュメント (日本語)
```

## Skills の構成ルール

- `skills/<skill-name>/SKILL.md` がスキルのエントリポイント
- SKILL.md には YAML frontmatter (`name`, `description`) を含める
- 参照ドキュメントやサンプルコードは同ディレクトリ内に配置
- ルートに `<skill-name>.md` / `<skill-name>.ja.md` で詳細ドキュメントを用意
- インストール: `npx skills add tbsten/skills --skill <skill-name>`

## Rules の構成ルール

- `rules/<rule-name>/RULE.md` がルール本体
- RULE.md 以外のファイルは参照ファイルとしてユーザーのカレントディレクトリに配置される
- サブディレクトリのネストも可能 (再帰的にダウンロードされる)
- インストール: `curl -fsSL https://raw.githubusercontent.com/tbsten/skills/main/rules/install.sh | bash -s -- <rule-name>`
- `as=<name>` オプションで保存名を変更可能
