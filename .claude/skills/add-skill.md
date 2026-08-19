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

詳細ドキュメントは `skills/` ディレクトリ直下に配置:
- `skills/<skill-name>.md` — スキル詳細ドキュメント (英語)
- `skills/<skill-name>.ja.md` — スキル詳細ドキュメント (日本語)

## 新規追加の手順

### Step 1: ユーザーへの確認

以下を確認する。ユーザーの指示から明確に読み取れる項目は確認を省略してよい。
一度に大量の質問をせず、重要な項目から順に確認する。

1. **skill 名** — kebab-case。`skills/<skill-name>/` のディレクトリ名として使用
2. **スキルの目的** — 何を生成・実行するスキルか。具体的なユースケースを把握する
3. **トリガーフレーズ** — ユーザーがどのような発話でこのスキルを呼び出すか
4. **必要なリソース** — scripts / references / assets / example のうち何が必要か
5. **グループ** — どのグループに属する skill か。「group (必須)」セクションの表から選ぶ

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
metadata:
  status: Experimental
  group: <グループ名>
---
```

description は第三者視点で記述する (例: "This skill should be used when..." ではなく具体的な機能説明)。

#### status (必須)

`metadata.status` に skill の成熟度を記載する。値は以下の 4 つのいずれか:

| status | 絵文字 | 意味 |
|---|---|---|
| `WIP` | 🌱 | 作成中だけど一旦出してみた |
| `Experimental` | 🧪 | 使えるはずだが、しっかり検証はされていない |
| `Active` | 🟢 | プロダクションレディで実用的に使える |
| `Active-Prime` | 💎 | Active かつ定番として愛用している |

新規追加時のデフォルトは `Experimental` (ユーザーの指示があればそれに従う)。
既存 skill の status を後から変更する場合は `change-status` スキルを使う。

#### group (必須)

`metadata.group` に skill の所属グループを **日本語グループ名** で記載する。値は以下のいずれか:

| グループ (ja) | Group (en) |
|---|---|
| タスク管理 | Task Management |
| Kotlin / Android アプリ開発 | Kotlin / Android App Development |
| Kotlin ライブラリ/ツール開発 | Kotlin Library / Tool Development |
| Web フロントエンド | Web Frontend |
| Git / GitHub | Git / GitHub |

適切なグループが無い場合は、CLAUDE.md の Group 表とこの表に新グループを追加した上で使う。

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

#### example コードのパッケージ名規約

example コードのパッケージ名は以下の規約に従う:

- `com.example.<skill-name-without-hyphens>` を使用する (例: `com.example.simpleloader`)
- スキル利用時にユーザーのパッケージ名に sed 等で置換される前提のプレースホルダーとして扱う
- プロジェクト固有のパッケージ名 (個人名・組織名を含むもの) は使わない

### Step 4: 詳細ドキュメントの作成

`skills/` ディレクトリ直下に以下を作成:
- `skills/<skill-name>.md` (英語) — スキルの概要、使い方、生成されるファイルの説明
- `skills/<skill-name>.ja.md` (日本語) — 上記の日本語版

### Step 5: README の更新

`README.md` と `README.ja.md` の **Available Skills** テーブルに行を追加する。
既存の行のフォーマットに厳密に合わせること。テーブルは先頭に **Group** (日本語版は **グループ**) 列があり、
Install と Description の間に **Status** (日本語版は **ステータス**) 列がある。

- **行の挿入位置**: テーブルの行はグループごとにまとまっている。新しい行はテーブル末尾ではなく、
  同じグループの既存行のまとまりの末尾に挿入する (そのグループの行がまだ無ければ「group (必須)」の表の順に従った位置に挿入する)
- **グループセル**: frontmatter の `metadata.group` と必ず一致させる。英語版 README では表の英語名 (Group (en)) を使う
- **ステータスセル**: `絵文字 + 半角スペース + status ラベル` で記載する (例: `🧪 Experimental`, `🟢 Active`)。
  frontmatter の `metadata.status` と必ず一致させる

**README.md テンプレート:**
````html
<tr>
<td>Group in English</td>
<td><a href="./skills/<skill-name>.md"><skill-name></a></td>
<td>

```sh
gh skill install tbsten/skills <skill-name>
```

</td>
<td>🧪 Experimental</td>
<td>Description in English</td>
</tr>
````

**README.ja.md テンプレート:**
````html
<tr>
<td>日本語グループ名</td>
<td><a href="./skills/<skill-name>.ja.md"><skill-name></a></td>
<td>

```sh
gh skill install tbsten/skills <skill-name>
```

</td>
<td>🧪 Experimental</td>
<td>日本語の説明</td>
</tr>
````

## 既存スキルの更新

1. `skills/<skill-name>/` 内の該当ファイルを編集する
2. SKILL.md の `description` を変更した場合は `skills/<skill-name>.md` / `<skill-name>.ja.md` の説明も同期する
3. 詳細ドキュメント (`skills/<skill-name>.md`, `<skill-name>.ja.md`) も必要に応じて更新する
4. リソースの追加・削除も同ディレクトリ内で行う

## 注意点

- `<skill-name>.md` (英語) と `<skill-name>.ja.md` (日本語) は **常に同期して更新** する
- テーブルは HTML `<table>` タグで記述し、Install 列のコマンドは ```sh code block で記載する
- description は Claude Code がスキル発動を判断する最重要情報。具体的なトリガーフレーズを含めること
- SKILL.md と references で情報を重複させない (Single Source of Truth)
- status は SKILL.md の `metadata.status` を SSoT とし、README のステータス列と必ず一致させる
- group は SKILL.md の `metadata.group` (日本語グループ名) を SSoT とし、README のグループ列 (英語版は英語名) と必ず一致させる
