---
name: contribute-prompt
description: >
  このリポジトリ (tbsten/skills) に新しい一回限りのプロンプト (prompt) を追加、または既存 prompt を更新するためのスキル。
  prompt のディレクトリ構成、PROMPT.md の作成、詳細ドキュメントの配置、README テーブルの更新までを一貫して行う。
  既存 skill をプロンプトとして公開し直す (プロンプト化する) 場合にも使う。
  prompt はインストール不要で、raw URL を参照させるコピペプロンプトとして配布される。
  このスキルはリポジトリのコントリビューター向けであり、プロンプトの利用者向けではない。
  Use when requested: "プロンプトを追加", "prompt を追加", "新しいプロンプトを作る", "add prompt",
  "プロンプトを更新", "update prompt", "スキルをプロンプト化", "プロンプトとして公開".
---

# contribute-prompt: Prompt の追加・更新

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
    ├── PROMPT.md          # プロンプト本体 (frontmatter なし)
    └── scripts/           # AI が判断せずそのまま実行する決定的な script (任意、raw URL で配布される)
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

## AI 責務最小化 (script 化)

skill / rule / prompt は、実行時に AI Agent が実装・作業・判断する範囲を最小限にする。
決定的な作業は事前に用意した script に寄せ、AI には判断が必要な部分だけを残す。
(このセクションの共通部分は contribute-skill.md / contribute-rule.md / contribute-prompt.md で同期して更新する)

### 判断基準

| script に寄せる (決定的な作業) | AI に残す (判断が必要な作業) |
|---|---|
| scaffold・テンプレート展開 | プロジェクト状況の把握と設計判断 |
| ファイルのコピー・リネーム・配置 | script に渡す入力値 (名前・パス・オプション) の決定 |
| sed 等による一括置換 | ユーザーへの確認・提案 |
| 順序が決まっているコマンド列 | script が失敗した時の原因分析と対処 |
| 成果物の検証 (ファイル存在・フォーマット・整合性チェック) | 検証 NG 時の修正内容の判断 |

「入力が同じなら結果が一意に決まる手順」を prose で AI に指示している箇所は、script 化の対象。
逆に、状況依存の判断まで script に押し込んで柔軟性を失わせない。

### script の同梱と参照 (prompt)

- PROMPT.md は raw URL で単体取得され、ファイルは何も配置されない。script も raw URL で取得して実行させる。基本形:

  ```sh
  curl -fsSL https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/<script のリポジトリ内パス> -o /tmp/<prompt-name>-<script 名>
  bash /tmp/<prompt-name>-<script 名> <引数>
  ```

  1 行の `curl -fsSL <raw URL> | bash -s -- <引数>` でもよいが、取得と実行を分けると失敗箇所の切り分けと実行前の内容確認がしやすい
- script の置き場所 (SSoT):
  - 既存 skill のプロンプト化の場合 → skill 側の `skills/<skill-name>/scripts/` を raw URL で参照する (prompts/ にコピーしない)
  - prompt 専用の script の場合 → `prompts/<prompt-name>/scripts/` に置く (PROMPT.md と同様 raw URL で配布される。「参照ファイルを prompts/ にコピーしない」規約は既存ファイルの複製を禁じるものであり、prompt が SSoT となる新規 script はここに置いてよい)
- script が複数ある場合は sparse clone を案内し、clone 先のパスで実行させる
- PROMPT.md には「script を読解・書き換え・再実装せず、そのまま実行する」と明記する

### script の品質要件

- bash は `set -euo pipefail` で始める
- 引数・前提条件を冒頭で検証し、失敗時は「何が・なぜ・どう直すか」が分かるエラーメッセージを stderr に出して非 0 で終了する
- 成功時の出力は AI が判定しやすい形式にする (要点のみ。可能なら `OK` 行や JSON)
- 冪等にする (再実行しても壊れない)。破壊的操作は明示フラグを必須にする

### PR 前セルフレビュー: AI 責務最小化チェックリスト

PR 作成前に以下を確認する:

- [ ] 決定的な手順 (scaffold / コピー / 置換 / 決まったコマンド列 / 検証) が prose で AI に委ねられていないか。該当があれば script 化したか
- [ ] script 化した作業の手順が本文 (SKILL.md / RULE.md / PROMPT.md) に重複記述されていないか (script を SSoT とする)
- [ ] 本文に「script を読解・書き換え・再実装せず、そのまま実行する」旨を明記したか
- [ ] script は失敗時に「何が・なぜ・どう直すか」が分かるエラーメッセージを出して非 0 終了するか
- [ ] script は冪等か。破壊的操作は明示フラグ必須になっているか
- [ ] script の入力 (引数・環境変数) が本文で定義され、AI の仕事が「入力値の決定」だけになっているか
- [ ] AI に残した作業は本当に判断が必要か (状況把握 / 設計判断 / ユーザー確認 / エラー分析のいずれかに該当するか)
- [ ] 逆に、判断が必要な部分まで script に押し込んで柔軟性を失っていないか
- [ ] script の参照パスが配布形態に合っているか (skill: `${CLAUDE_SKILL_DIR}/scripts/`、rule: カレント展開後の相対パス + インタープリタ明示、prompt: raw URL 取得)
- [ ] 検証も script 化されているか。AI の目視確認だけに頼っていないか

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
- 決定的な手順は prose で書かず script 化する。「AI 責務最小化 (script 化)」セクションのチェックリストを PR 前に実施する
