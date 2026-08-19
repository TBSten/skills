---
name: add-prompt
description: >
  このリポジトリ (tbsten/skills) に新しい一回限りのプロンプト (prompt) を追加、または既存 prompt を更新するためのスキル。
  prompt のディレクトリ構成、PROMPT.md の作成、詳細ドキュメントの配置、README テーブルの更新までを一貫して行う。
  既存 skill をプロンプトとして公開し直す (プロンプト化する) 場合にも使う。
  prompt はインストール不要で、raw URL を参照させるコピペプロンプトとして配布される。
  このスキルはリポジトリのコントリビューター向けであり、プロンプトの利用者向けではない。
  Use when requested: "プロンプトを追加", "prompt を追加", "新しいプロンプトを作る", "add prompt",
  "プロンプトを更新", "update prompt", "スキルをプロンプト化", "プロンプトとして公開".
---

# add-prompt: Prompt の追加・更新

このリポジトリに新しい prompt を追加、または既存 prompt を更新する。

## Prompt とは

skill と違いインストール不要の、一回限り実行される Markdown 指示書。ユーザーは README の実行プロンプト
(例: `https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/<prompt-name>/PROMPT.md を取得して、その指示に従って実行して`)
を Claude Code にコピペして使う。セットアップ・スキャフォールドのような 1 回で完結する作業に向く。

## Prompt の構成

```
prompts/
├── <prompt-name>.md      # 詳細ドキュメント (英語、frontmatter に status / group)
├── <prompt-name>.ja.md   # 詳細ドキュメント (日本語、frontmatter なし)
└── <prompt-name>/
    └── PROMPT.md          # プロンプト本体 (frontmatter なし)
```

## PROMPT.md の書き方

- **frontmatter を付けない**。raw URL で直接取得・実行されるため、本文のみで完結させる
- 冒頭に `# <内容を表す日本語タイトル>` と、「このプロンプトは [TBSten/skills](https://github.com/TBSten/skills) の `prompts/<prompt-name>` として配布されている一回限りのプロンプト。<目的 1 文>」を書く
- 命令形 (imperative) で記述する
- **参照ファイルはリポジトリ内の既存ファイルを raw URL で参照する**。prompts/ 配下にコピーを置かない:
  - 単一ファイル: `https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/<パス>`
  - ディレクトリの一覧: `https://api.github.com/repos/TBSten/skills/contents/<パス>`
  - ファイルが多い場合は sparse clone を案内する:

    ```sh
    git clone --depth 1 --filter=blob:none --sparse https://github.com/TBSten/skills.git /tmp/tbsten-skills
    git -C /tmp/tbsten-skills sparse-checkout set <対象ディレクトリ>
    ```
- 冒頭付近に「## 参照ファイルの取得方法」セクションを置き、上記の取得手段をまとめて案内する

### 既存 skill のプロンプト化

セットアップ系など 1 回で完結する skill は、prompt として公開し直せる:

1. `skills/<skill-name>/SKILL.md` の本文をベースに `prompts/<skill-name>/PROMPT.md` を作成する (frontmatter は除く)
2. `assets/...` / `example/...` / `references/...` へのローカル相対パス参照をすべて
   `https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/skills/<skill-name>/<path>` への参照に書き換える
   (複数ファイルのコピーは sparse clone からのコピー指示にする)
3. 「このスキル」等の自己言及は「このプロンプト」に置き換える。手順・表・チェックリストの内容は変えない
4. skill 本体は削除しない。skill と prompt は併存し、参照ファイルは skill 側 (SSoT) を共有する

## 詳細ドキュメント (必須)

`prompts/` ディレクトリ直下に以下を作成:
- `prompts/<prompt-name>.md` (英語) — 概要、実行プロンプト、実行内容、参照ファイル、skill 版へのリンク
- `prompts/<prompt-name>.ja.md` (日本語) — 上記の日本語版

### status / group (必須)

rule と同様、**英語詳細ドキュメント `prompts/<prompt-name>.md` の先頭 frontmatter** に記載する
(PROMPT.md には書かない)。`.ja.md` には付けず、英語版を SSoT とする。

```yaml
---
status: Experimental
group: <グループ名>
---
# <prompt-name> Prompt
```

status は以下のいずれか:

| status | 絵文字 | 意味 |
|---|---|---|
| `WIP` | 🌱 | 作成中だけど一旦出してみた |
| `Experimental` | 🧪 | 使えるはずだが、しっかり検証はされていない |
| `Active` | ✅ | プロダクションレディで実用的に使える |
| `Active-Prime` | 💎 | Active かつ定番として愛用している |
| `Archived` | ❌ | 役目を終えた・メンテナンスされていない |

group は **日本語グループ名** で記載する (絵文字は付けない)。値は以下のいずれか:

| 絵文字 | グループ (ja) | Group (en) |
|---|---|---|
| 🔴 | タスク管理 | Task Management |
| 🟢 | Kotlin / Android アプリ開発 | Kotlin / Android App Development |
| 🟣 | Kotlin ライブラリ/ツール開発 | Kotlin Library / Tool Development |
| 🔵 | Web フロントエンド | Web Frontend |
| ⚫️ | Git / GitHub | Git / GitHub |

新規追加時のデフォルトは `Experimental` (ユーザーの指示があればそれに従う)。
既存 prompt の status を後から変更する場合は `change-status` スキルを使う。
適切なグループが無い場合は、CLAUDE.md の Group 表とこの表に新グループを追加した上で使う。

## README の更新

`README.md` と `README.ja.md` の **💬 Available Prompts** (日本語版: **利用可能なプロンプト**) テーブルに行を追加する。
セクションは 📝 Available Rules の下、🤝 Contribute の上に位置する。既存の行のフォーマットに厳密に合わせること。

- **行の挿入位置**: 同じグループの既存行のまとまりの末尾に挿入する (そのグループの行がまだ無ければ group 表の順に従った位置に挿入する)
- **グループセル**: グループの **先頭行のみ** `<td><絵文字> <グループ名></td>` (英語版は英語名) を記載し、同グループの 2 行目以降は空セル `<td></td>` にする
- **実行セル**: インストールコマンドの代わりに、言語指定なしの code block で実行プロンプトを記載する
- **ステータスセル**: `絵文字 + 半角スペース + status ラベル` (例: `🧪 Experimental`)。frontmatter の `status` と必ず一致させる
- **説明セル**: 最大 80 文字。詳細は詳細ドキュメント側に書く

**README.md テンプレート** (グループ先頭行になる場合は 1 つ目の `<td></td>` を `<td><絵文字> <Group in English></td>` にする):
````html
<tr>
<td></td>
<td><a href="./prompts/<prompt-name>.md"><prompt-name></a></td>
<td>

```
Fetch https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/<prompt-name>/PROMPT.md and follow its instructions
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
<td><a href="./prompts/<prompt-name>.ja.md"><prompt-name></a></td>
<td>

```
https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/<prompt-name>/PROMPT.md を取得して、その指示に従って実行して
```

</td>
<td>🧪 Experimental</td>
<td>日本語の説明</td>
</tr>
````

## 既存プロンプトの更新

1. `prompts/<prompt-name>/PROMPT.md` を編集する
2. プロンプト化元の skill が更新された場合は、PROMPT.md にも同じ変更を反映する (内容の同期)
3. `prompts/<prompt-name>.md` / `<prompt-name>.ja.md` の説明も必要に応じて同期する
4. 利用者は raw URL を参照するため、main にマージされた時点で最新版が配布される

## 注意点

- `<prompt-name>.md` (英語) と `<prompt-name>.ja.md` (日本語) は **常に同期して更新** する
- PROMPT.md は raw URL 直取得で単体実行されるため、リポジトリ内の相対パス参照を残さない (必ず raw URL / sparse clone 経由にする)
- status / group は `prompts/<prompt-name>.md` の frontmatter を SSoT とし、README と必ず一致させる (PROMPT.md には書かない)
- グループセルの記載はグループ先頭行のみ (絵文字付き)、継続行は空セル
- README の説明列は最大 80 文字。収まらない詳細は詳細ドキュメント側に書く
