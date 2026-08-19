---
name: add-rule
description: >
  このリポジトリ (tbsten/skills) に新しい Claude Code rule を追加、または既存 rule を更新するためのスキル。
  rule のディレクトリ構成、RULE.md の作成、参照ファイルの配置、README テーブルの更新までを一貫して行う。
  rule は rules/install.sh 経由でユーザーの .claude/rules/ にインストールされる。
  このスキルはリポジトリのコントリビューター向けであり、ルールの利用者向けではない。
  Use when requested: "ルールを追加", "rule を追加", "新しいルールを作る", "add rule",
  "ルールを更新", "update rule", "ルールの修正", "ルールの内容を変更".
---

# add-rule: Rule の追加・更新

このリポジトリに新しい rule を追加、または既存 rule を更新する。

## Rule の構成

```
rules/<rule-name>/
├── RULE.md               # ルール本体 (必須。.claude/rules/<rule-name>.md として配置される)
└── **/*                   # 参照ファイル (任意。ユーザーのカレントディレクトリに配置される)
```

詳細ドキュメントは `rules/` ディレクトリ直下に配置:
- `rules/<rule-name>.md` — ルール詳細ドキュメント (英語)
- `rules/<rule-name>.ja.md` — ルール詳細ドキュメント (日本語)

## インストールの仕組み

`rules/install.sh` が以下を行う:
1. `rules/<rule-name>/RULE.md` → ユーザーの `.claude/rules/<rule-name>.md` にコピー
2. `rules/<rule-name>/` 内の RULE.md 以外のファイル → ユーザーのカレントディレクトリに再帰的にコピー
3. `as=<name>` オプションで保存名を変更可能

この仕組みを理解した上でファイルを配置すること。

## 新規追加の手順

### Step 1: ユーザーへの確認

以下を確認する。ユーザーの指示から明確に読み取れる項目は確認を省略してよい。

1. **rule 名** — kebab-case。`rules/<rule-name>/` のディレクトリ名として使用
2. **ルールの内容** — Claude Code にどのような振る舞いを指示するルールか
3. **参照ファイルの有無** — ルールが参照するテンプレートやサンプルコード等があるか。ある場合、インストール時にユーザーのカレントディレクトリに配置されることを考慮してパスを設計する
4. **グループ** — どのグループに属する rule か。「group (必須)」セクションの表から選ぶ

### Step 2: ディレクトリとファイルの作成

1. `rules/<rule-name>/` ディレクトリを作成
2. `rules/<rule-name>/RULE.md` を作成
   - skill と異なり YAML frontmatter は不要。ルールの内容をそのまま記述する
   - ルールは命令形 (imperative) で記述する
3. 参照ファイルがあれば同ディレクトリに配置
   - サブディレクトリのネストも可能 (install.sh が再帰的にダウンロードする)
   - ファイルパスはユーザーのプロジェクトルートからの相対パスになることを意識する

### Step 2.5: 詳細ドキュメントの作成

`rules/` ディレクトリ直下に以下を作成:
- `rules/<rule-name>.md` (英語) — ルールの概要、適用対象、インストール方法、含まれるファイルの説明
- `rules/<rule-name>.ja.md` (日本語) — 上記の日本語版

#### status (必須)

rule の成熟度は **英語詳細ドキュメント `rules/<rule-name>.md` の先頭 frontmatter** に記載する
(RULE.md はユーザーの `.claude/rules/` にそのまま配置されるため frontmatter を持たせない)。
`.ja.md` には付けず、英語版を SSoT とする。

```yaml
---
status: Experimental
group: <グループ名>
---
# <rule-name> Rule
```

値は以下の 4 つのいずれか:

| status | 絵文字 | 意味 |
|---|---|---|
| `WIP` | 🌱 | 作成中だけど一旦出してみた |
| `Experimental` | 🧪 | 使えるはずだが、しっかり検証はされていない |
| `Active` | ✅ | プロダクションレディで実用的に使える |
| `Active-Prime` | 💎 | Active かつ定番として愛用している |

新規追加時のデフォルトは `Experimental` (ユーザーの指示があればそれに従う)。
既存 rule の status を後から変更する場合は `change-status` スキルを使う。

#### group (必須)

rule の所属グループも status と同じく **英語詳細ドキュメント `rules/<rule-name>.md` の frontmatter** に、
**日本語グループ名** で記載する (`.ja.md` には付けず、英語版を SSoT とする。絵文字は付けない)。値は以下のいずれか:

| 絵文字 | グループ (ja) | Group (en) |
|---|---|---|
| 🔴 | タスク管理 | Task Management |
| 🟢 | Kotlin / Android アプリ開発 | Kotlin / Android App Development |
| 🟣 | Kotlin ライブラリ/ツール開発 | Kotlin Library / Tool Development |
| 🔵 | Web フロントエンド | Web Frontend |
| ⚫ | Git / GitHub | Git / GitHub |

適切なグループが無い場合は、CLAUDE.md の Group 表とこの表に新グループを追加した上で使う。

### Step 3: README の更新

`README.md` と `README.ja.md` の **Available Rules** テーブルに行を追加する。
既存の行のフォーマットに厳密に合わせること。テーブルは先頭に **Group** (日本語版は **グループ**) 列があり、
Install と Description の間に **Status** (日本語版は **ステータス**) 列がある。

- **行の挿入位置**: テーブルの行はグループごとにまとまっている。新しい行はテーブル末尾ではなく、
  同じグループの既存行のまとまりの末尾に挿入する (そのグループの行がまだ無ければ「group (必須)」の表の順に従った位置に挿入する)
- **グループセル**: グループの **先頭行のみ** `<td><絵文字> <グループ名></td>` (例: `<td>🟢 Kotlin / Android アプリ開発</td>`、英語版 README では表の英語名) を記載し、
  同グループの 2 行目以降は空セル `<td></td>` にする。グループ名は詳細ドキュメントの `group` frontmatter と必ず一致させる
- **ステータスセル**: `絵文字 + 半角スペース + status ラベル` で記載する (例: `🧪 Experimental`, `✅ Active`)。
  詳細ドキュメントの `status` frontmatter と必ず一致させる

**README.md テンプレート** (グループ先頭行になる場合は 1 つ目の `<td></td>` を `<td><絵文字> <Group in English></td>` にする):
````html
<tr>
<td></td>
<td><a href="./rules/<rule-name>.md"><rule-name></a></td>
<td>

```sh
curl -fsSL https://rules.tbsten.me/i | bash -s -- <rule-name>
```

</td>
<td>🧪 Experimental</td>
<td>Description in English</td>
</tr>
````

**README.ja.md テンプレート** (グループ先頭行になる場合は 1 つ目の `<td></td>` を `<td><絵文字> <日本語グループ名></td>` にする):
````html
<tr>
<td></td>
<td><a href="./rules/<rule-name>.ja.md"><rule-name></a></td>
<td>

```sh
curl -fsSL https://rules.tbsten.me/i | bash -s -- <rule-name>
```

</td>
<td>🧪 Experimental</td>
<td>日本語の説明</td>
</tr>
````

## 既存ルールの更新

1. `rules/<rule-name>/RULE.md` を編集する
2. 参照ファイルの追加・削除・変更も同ディレクトリ内で行う
3. `rules/<rule-name>.md` / `<rule-name>.ja.md` の説明も必要に応じて同期する
4. `rules/install.sh` は全ルール共通のスクリプトなので、個別ルールの更新では変更不要
5. ユーザーは同じインストールコマンドを再実行すれば最新版に更新される

## 注意点

- `<rule-name>.md` (英語) と `<rule-name>.ja.md` (日本語) は **常に同期して更新** する
- テーブルは HTML `<table>` タグで記述し、Install 列のコマンドは ```sh code block で記載する
- 参照ファイルのパス設計時は、ユーザーのプロジェクトルートに展開されることを考慮する
- status は `rules/<rule-name>.md` の frontmatter を SSoT とし、README のステータス列と必ず一致させる (RULE.md には書かない)
- group も `rules/<rule-name>.md` の frontmatter (日本語グループ名) を SSoT とし、README のグループ列 (英語版は英語名) と必ず一致させる (RULE.md には書かない)。グループセルの記載はグループ先頭行のみ (絵文字付き)、継続行は空セル
