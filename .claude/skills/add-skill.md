---
name: add-skill
description: >
  このリポジトリ (tbsten/skills) に新しい Claude Code skill を追加、または既存 skill を更新するためのスキル。
  skill のディレクトリ構成、SKILL.md の作成、参照ドキュメント・サンプルコードの配置、
  README テーブルの更新までを一貫して行う。
  このスキルはリポジトリのコントリビューター向けであり、スキルの利用者向けではない。
  Use when requested: "スキルを追加", "skill を追加", "新しいスキルを作る", "add skill",
  "スキルを更新", "update skill", "スキルの修正", "スキルの内容を変更".
---

# add-skill: Skill の追加・更新

このリポジトリに新しい skill を追加、または既存 skill を更新する。

## Skill の構成

```
skills/<skill-name>/
├── SKILL.md              # スキル本体 (YAML frontmatter + Markdown 手順書、必須)
├── references/            # Claude がコンテキストに読み込む参照ドキュメント (任意)
├── example/               # サンプルコード。コピー元として利用される場合がある (任意)
└── assets/                # 出力に使用されるファイル (テンプレート等、任意)
```

リポジトリルートにも以下を配置:
- `<skill-name>.md` — スキル詳細ドキュメント (英語)
- `<skill-name>.ja.md` — スキル詳細ドキュメント (日本語)

## 新規追加の手順

### Step 1: ユーザーへの確認

以下を確認する。ユーザーの指示から明確に読み取れる項目は確認を省略してよい。
一度に大量の質問をせず、重要な項目から順に確認する。

1. **skill 名** — kebab-case。`skills/<skill-name>/` のディレクトリ名として使用
2. **スキルの目的** — 何を生成・実行するスキルか。具体的なユースケースを把握する
3. **トリガーフレーズ** — ユーザーがどのような発話でこのスキルを呼び出すか
4. **必要なリソース** — scripts / references / assets / example のうち何が必要か

### Step 2: SKILL.md の作成

`skills/<skill-name>/SKILL.md` を作成する。

#### YAML Frontmatter (必須)

```yaml
---
name: <skill-name>
description: >
  スキルの概要を具体的に記述する。Claude Code がスキルの発動タイミングを判断する
  最も重要な情報源となるため、以下を含めること:
  - スキルが何を行うか
  - どのようなプロジェクト・状況で使うか
  - Use when requested: "トリガーフレーズ1", "フレーズ2", ...
---
```

description は第三者視点で記述する (例: "This skill should be used when..." ではなく具体的な機能説明)。

#### Markdown 本文

命令形 (imperative) で記述する。"あなたは〜してください" ではなく "〜する" の形式。
以下の構成を基本とする:

1. **スキルの概要** — 1〜2文で目的を説明
2. **Usage** — 利用前の確認事項、スキップ条件
3. **手順** — Step-by-step の実行手順
4. **リソースの参照方法** — bundled resources がある場合、いつ・どのように使うか

#### Progressive Disclosure の原則

SKILL.md は 5,000 words 以下に抑える。詳細な情報は references/ に分離し、
必要なときだけ読み込むようにする。SKILL.md と references で情報を重複させない。

### Step 3: リソースの配置

Step 1 で特定したリソースを配置する:

- **references/** — Claude が作業中に参照するドキュメント (スキーマ定義、API仕様等)
- **example/** — コピー元として使うサンプルコード
- **assets/** — 出力に使うテンプレートや画像等

不要なディレクトリは作成しない。

### Step 4: 詳細ドキュメントの作成

リポジトリルートに以下を作成:
- `<skill-name>.md` (英語) — スキルの概要、使い方、生成されるファイルの説明
- `<skill-name>.ja.md` (日本語) — 上記の日本語版

### Step 5: README の更新

`README.md` と `README.ja.md` の **Available Skills** テーブルに行を追加する。
既存の行のフォーマットに厳密に合わせること。

**README.md テンプレート:**
````html
<tr>
<td><skill-name></td>
<td>

```sh
npx skills add tbsten/skills --skill <skill-name>
```

</td>
<td>Description in English</td>
<td><a href="./<skill-name>.md"><skill-name>.md</a></td>
</tr>
````

**README.ja.md テンプレート:**
````html
<tr>
<td><skill-name></td>
<td>

```sh
npx skills add tbsten/skills --skill <skill-name>
```

</td>
<td>日本語の説明</td>
<td><a href="./<skill-name>.ja.md"><skill-name>.ja.md</a></td>
</tr>
````

## 既存スキルの更新

1. `skills/<skill-name>/` 内の該当ファイルを編集する
2. SKILL.md の `description` を変更した場合は README.md / README.ja.md の説明も同期する
3. 詳細ドキュメント (`<skill-name>.md`, `<skill-name>.ja.md`) も必要に応じて更新する
4. リソースの追加・削除も同ディレクトリ内で行う

## 注意点

- README.md (英語) と README.ja.md (日本語) は **常に同期して更新** する
- テーブルは HTML `<table>` タグで記述し、Install 列のコマンドは ```sh code block で記載する
- description は Claude Code がスキル発動を判断する最重要情報。具体的なトリガーフレーズを含めること
- SKILL.md と references で情報を重複させない (Single Source of Truth)
