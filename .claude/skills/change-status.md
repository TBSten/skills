---
name: change-status
description: >
  このリポジトリ (tbsten/skills) の既存 skill / rule / prompt の status を変更するためのスキル。
  status は WIP / Experimental / Active / Active-Prime / Archived の 5 値。
  SSoT (skill は SKILL.md の metadata.status、rule / prompt は詳細ドキュメント rules/<name>.md / prompts/<name>.md の frontmatter status) と
  README.md / README.ja.md のステータス列を同期して更新する。
  このスキルはリポジトリのコントリビューター向けであり、skill / rule / prompt の利用者向けではない。
  Use when requested: "status を変更", "ステータスを変更", "Active にする", "Experimental に戻す",
  "WIP から昇格", "change status", "定番にする", "Active-Prime に上げる", "ステータスを上げる",
  "Archived にする", "アーカイブする", "archive".
---

# change-status: skill / rule / prompt の status 変更

このリポジトリの既存 skill / rule / prompt の status を変更し、SSoT と README を同期する。

## status の定義

| status | 絵文字 | 意味 |
|---|---|---|
| `WIP` | 🌱 | 作成中だけど一旦出してみた |
| `Experimental` | 🧪 | 使えるはずだが、しっかり検証はされていない |
| `Active` | ✅ | プロダクションレディで実用的に使える |
| `Active-Prime` | 💎 | Active かつ定番として愛用している |
| `Archived` | ❌ | 役目を終えた・メンテナンスされていない |

## SSoT (status の保存場所)

- **skill**: `skills/<name>/SKILL.md` の frontmatter `metadata.status`
  ```yaml
  ---
  name: <skill-name>
  description: >
    ...
  metadata:
    status: Active
  ---
  ```
- **rule**: `rules/<name>.md` (英語詳細ドキュメント) 先頭の frontmatter `status`
  ```yaml
  ---
  status: Active
  ---
  # <rule-name> Rule
  ```
  - `rules/<name>.ja.md` には frontmatter を付けない。英語版を正 (SSoT) とする。
- **prompt**: `prompts/<name>.md` (英語詳細ドキュメント) 先頭の frontmatter `status` (rule と同じ形式)
  - `prompts/<name>.ja.md` には frontmatter を付けない。英語版を正 (SSoT) とする。

README.md / README.ja.md のステータス列は **frontmatter を正としたミラー**。frontmatter を変更したら必ず README も同じ status に更新する。

## 手順

### Step 1: 対象と新 status の確認

以下を確認する。ユーザーの指示から明確に読み取れる項目は確認を省略してよい。

1. **種別** — skill / rule / prompt のいずれか
2. **名前** — kebab-case (例: `kotlin-tuple`, `kmp-error-handling`)
3. **新しい status** — `WIP` / `Experimental` / `Active` / `Active-Prime` / `Archived` のいずれか。5 値以外が指定されたら差し戻す

対象ファイルが存在することを確認する:
- skill → `skills/<name>/SKILL.md`
- rule → `rules/<name>.md`
- prompt → `prompts/<name>.md`

### Step 2: SSoT の更新

- **skill**: `skills/<name>/SKILL.md` の frontmatter を更新する
  - `metadata.status` が既にあれば値を差し替える
  - `metadata:` ブロックが無ければ frontmatter の閉じ `---` の直前に追加する
- **rule**: `rules/<name>.md` の frontmatter `status` を更新する
  - frontmatter が無ければファイル先頭に `---\nstatus: <new>\n---\n\n` を追加する
- **prompt**: `prompts/<name>.md` の frontmatter `status` を更新する (rule と同様)

### Step 3: README の更新

`README.md` と `README.ja.md` の該当エントリのステータスセルを更新する。

1. 対象名を含む行 (`<a href="./skills/<name>...">` / `<a href="./rules/<name>...">` / `<a href="./prompts/<name>...">`) を探す
2. その行の直後にある install / 実行セルの次の `<td>...Active...</td>` 等のステータスセルを、新しい `<td><絵文字> <status></td>` に置き換える
3. セル表示は `絵文字 + 半角スペース + status ラベル` (例: `✅ Active`, `🧪 Experimental`)

両ファイルとも忘れずに更新する。

#### Archived の場合の特例

新 status が `Archived` の場合は、ステータスセルの更新ではなくエントリの移動を行う:

1. 通常テーブル (⭐️ Available Skills / 📝 Available Rules / 💬 Available Prompts) から該当 `<tr>...</tr>` を削除する
   - 削除する行がグループ先頭行 (グループセルに `絵文字 + グループ名` が入っている行) の場合は、直後の同グループ行の空グループセルにその内容を移す
2. README 末尾 (🤝 Contribute セクションの下) の `<details>` 内 Archived テーブルに、ステータス `❌ Archived` で同じ行を追加する
3. Archived テーブルが無ければ `<details><summary> ❌ Archived Skills / Rules / Prompts </summary>` (日本語版は `❌ Archived スキル / ルール / プロンプト`) セクションごと新規作成する。skill / rule / prompt の 3 種が同居するため、列は `Name`・`Install / Run`・`Status`・`Description` (日本語版は `名前`・`インストール / 実行`・`ステータス`・`説明`) とし、グループ列は持たない

逆に `Archived` から他の status へ戻す場合は、Archived テーブルから通常テーブルへ行を戻す。

### Step 4: 差分確認と報告

1. `git diff` で変更内容を確認し、SSoT と README のステータスが一致していることを確認する
2. 変更ファイル一覧と新旧 status をユーザーに報告する
3. commit / push は **ユーザーの明示的な許可を得てから** 実行する

## 注意点

- frontmatter (SSoT) と README のステータスは **常に一致** させる。片方だけの更新は禁止
- status は 5 値のみ。表記ゆれ (`active`, `WIP中` 等) を許さない
- 複数の skill / rule / prompt をまとめて変更する場合は、1 件ずつ Step 2-3 を繰り返す
