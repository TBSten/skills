---
name: contribute-skill
description: >
  現在のプロジェクトで得た知見・パターン・ワークフローを TBSten/skills リポジトリに
  skill として登録するための PR を自動作成する。
  プロジェクトの CLAUDE.md、.claude/rules/、.claude/skills/、コードベースから知見を収集し、
  再利用可能な skill としてパッケージングして PR を作成するまでを一貫して行う。
  Use when requested: "知見をスキルリポジトリに登録", "contribute skill", "この知見を共有",
  "スキルとして登録", "知見をまとめて PR", "このパターンをスキル化".
  gh CLI と git がインストールされている必要がある。
metadata:
  status: Active
---

# contribute-skill

現在のプロジェクトから知見を収集し、TBSten/skills リポジトリに skill として登録する PR を自動作成する。

## 前提条件チェック

スキル起動時にまず以下を確認する。失敗した場合は対処方法を案内して中断する。

1. `git --version` で git の存在を確認
2. `gh auth status` で gh CLI の認証状態を確認

## Step 1: 起動時の確認

ARGUMENTS からユーザーが収集したい知見の説明を受け取る。以下を確認する。
ユーザーの指示から明確に読み取れる項目は確認を省略してよい。

1. **skill 名** — kebab-case で命名。知見の内容から適切な名前を提案する
2. **収集対象** — どの知見をスキル化するか。具体的なファイルパスやセクションを特定する
3. **スキルの対象ユーザー** — どのようなプロジェクト・状況で使われるスキルか
4. **status** — skill の成熟度 (WIP / Experimental / Active / Active-Prime)。デフォルトは `Experimental`。ユーザーの指示があればそれに従う
5. **group** — skill の所属グループ (「タスク管理」「Kotlin / Android アプリ開発」「Kotlin ライブラリ/ツール開発」「Web フロントエンド」「Git / GitHub」等)。知見の内容から適切なグループを提案する。全一覧は clone 先の `.claude/skills/contribute-skill.md` の group 表を正とする
6. **リポジトリ** — デフォルトは `TBSten/skills`。fork を使う場合はユーザーに確認する

## Step 2: 知見の収集と整理

ユーザーが指定した収集対象を読み取る。

### 収集の効率化

収集対象が多い場合、Agent tool を活用して並列に収集する:

1. コアファイルの読み取り (Glob でファイル一覧取得 → 並列 Read)
2. 利用箇所の調査 (Grep で呼び出し元を検索 → 並列 Read)
3. テストコードの調査 (並列 Agent)

独立したファイル読み取りは必ず並列で実行すること。

主なソース:

- CLAUDE.md の特定セクション
- `.claude/rules/` 内のルールファイル
- `.claude/skills/` 内の既存スキル
- コードベース内のパターンやユーティリティ
- **実際の利用箇所** — Grep 等で対象コードの呼び出し元を調査し、利用パターン（ViewModel での使い方、UI での使い方、テストでの使い方等）を収集する
- **テストコード** — 対象コードのテストを検索 (`*Test.kt`, `*Spec.kt`, `*.test.ts` 等)
- ユーザーの説明そのもの

### テストコードの扱い

テストが見つかった場合、以下の基準で同梱方針を判断する:

- **example/test/ に同梱**: テストの書き方自体がスキルの一部である場合（テストパターンがスキルの価値の一つ）
- **references/ に記載**: テストパターンを参考情報として提供する場合
- **同梱しない**: テストがプロジェクト固有のフレームワークに強く依存する場合

同梱する場合は Step 3.5 と同様にプロジェクト固有の依存を除去する。

読み取った知見を以下の形式で整理し、ユーザーに提示する:

- **スキルの目的** — 1〜2文で何を実現するか
- **トリガー条件** — どのような発話・状況で発動すべきか (5個以上リストアップ)
- **手順・ワークフロー** — 具体的な実行ステップの箇条書き
- **利用パターン** — 実プロジェクトでの使い方を 3 つ以上リストアップ
- **同梱リソース** — サンプルコードやテンプレート等。不要であれば「なし」と明記
- **対象プロジェクトの前提** — 言語、フレームワーク、ディレクトリ構成等の前提条件
- **status** — skill の成熟度 (デフォルト `Experimental`)
- **group** — skill の所属グループ

ユーザーの承認を得てから次のステップに進む。

## Step 3: ワークディレクトリの準備と contribute-skill.md の読み込み

1. ワークディレクトリを準備する:

```bash
rm -rf /tmp/contribute-skill-<skill-name>
git clone --depth 1 https://github.com/<repo>.git /tmp/contribute-skill-<skill-name>
```

`<repo>` は Step 1 で確認したリポジトリ (デフォルト: `TBSten/skills`)。
`<skill-name>` は Step 1 で決定したスキル名。

2. clone したリポジトリ内のスキル作成ガイドを読み込む:

```
/tmp/contribute-skill-<skill-name>/.claude/skills/contribute-skill.md
```

contribute-skill.md を読み込んだ後、以下のように統合する:

- **contribute-skill.md の Step 1 (確認事項)**: contribute-skill の Step 1-2 で既に完了。スキップする
- **contribute-skill.md の Step 2 (SKILL.md 作成)**: フォーマット・構成ルールに従う。内容は contribute-skill の Step 2 で整理した知見を使う
- **contribute-skill.md の Step 3-5 (リソース配置・ドキュメント・README)**: そのまま従う
- **contribute-skill.md の「AI 責務最小化 (script 化)」**: Step 4 のリソース設計時に従う。決定的な手順は scripts/ に script 化する

ガイドが見つからない場合は、以下の「フォールバック」セクションに従う。

3. スキル作成のベストプラクティスを参照する:

```
https://github.com/anthropics/skills/blob/main/skills/skill-creator/SKILL.md
```

WebFetch 等で上記の skill-creator SKILL.md を取得し、以下の観点を把握してから Step 4 に進む:

- **SKILL.md の構成**: frontmatter (name, description)、Progressive Disclosure (500行以下)、命令形での記述
- **description の書き方**: トリガーフレーズを具体的に含める。undertrigger 防止のためやや積極的に記述する
- **リソース構成**: references/ (参照ドキュメント)、example/ (サンプルコード)、assets/ (テンプレート等) の使い分け
- **ドメイン別整理**: 複数フレームワーク対応時は references/ 内でファイル分離

取得できない場合はスキップしてよい。

## Step 3.5: コードの汎用化

example にコードを配置する前に以下を確認・実行する:

1. **プロジェクト固有の import を検出**: example 対象ファイルの import を全走査し、スキルの example パッケージ外への依存をリストアップ
2. **依存の分類と対処**:
   - 標準ライブラリ (`kotlin.*`, `kotlinx.*`) → そのまま
   - フレームワーク標準 (`androidx.*`, `react` 等) → そのまま
   - プロジェクト固有ユーティリティ → 標準的な代替に置換、または example に同梱
   - プロジェクト固有アノテーション → 削除
3. **パッケージ名の抽象化**: `com.example.<skill-name-without-hyphens>` に統一 (contribute-skill.md のパッケージ名規約に準拠)
4. **置換結果をユーザーに提示**: 何を何に置換したかの一覧を示し、承認を得る

## Step 4: スキルファイルの作成

clone した `/tmp/contribute-skill-<skill-name>/` 内で、contribute-skill.md の手順に従い以下を作成する。
Step 2 で整理した知見をもとに、SKILL.md の各セクションを埋めていく。

1. `skills/<skill-name>/SKILL.md` — frontmatter (name, description) + 手順書
2. 必要に応じて `references/`, `example/`, `assets/` 内のリソース
3. `skills/<skill-name>.md` — 詳細ドキュメント (英語)
4. `skills/<skill-name>.ja.md` — 詳細ドキュメント (日本語)
5. `README.md` と `README.ja.md` の Available Skills テーブルに行を追加

### レビューの提示方法

作成ファイル数に応じて提示方法を変える:

- **5 ファイル以下**: 全ファイルの内容を提示
- **6 ファイル以上**: 以下を提示
  1. ファイル一覧と各ファイルの概要 (1行)
  2. SKILL.md の全内容 (最重要ファイル)
  3. 特に注意が必要なファイルをピックアップして内容を提示
  4. 「他のファイルも確認しますか？」とユーザーに確認

フィードバックがあれば修正してから次に進む。

### プロジェクト固有情報の除外チェック

スキルは公開リポジトリに登録されるため、作成したファイルに以下が含まれていないか細心の注意を払う:

- プロジェクト固有のファイルパス、URL、ドメイン名
- 社内システムやサービスの名前
- 認証情報、トークン、API キー
- 個人名、メールアドレス、チーム名
- 社内ドキュメントへのリンク
- その他、公開すべきでない情報

知見を汎用化する際に具体例が必要な場合は、プレースホルダー (`<project-name>`, `<your-package>` 等) に置き換える。
チェック結果をユーザーに報告し、問題がないことを確認してから次に進む。

## Step 4.5: セルフレビュー

PR 作成前に以下を確認する。

### ファイル・フォーマットチェック

1. **必要なファイルが揃っているか** — 以下のファイルが存在することを確認する:
   - `./skills/<skill-name>.ja.md` が存在すること
   - `./skills/<skill-name>.md` が存在すること
   - `./skills/<skill-name>/SKILL.md` が存在すること
   - SKILL.md 内で参照しているすべてのファイルが `./skills/<skill-name>/` 配下に存在すること
   - `README.md` と `README.ja.md` の Available Skills テーブルに新しいスキルのエントリが追加されていること
   - SKILL.md の frontmatter に `metadata.status` があり、README のステータス列 (絵文字+ラベル) と一致していること
   - SKILL.md の frontmatter に `metadata.group` があり、README のグループ列と一致していること。行が同じグループのまとまりに挿入され、グループセルはグループ先頭行のみ絵文字付きで記載・継続行は空セルになっていること
   - README の説明セルが 80 文字以内であること (収まらない詳細は詳細ドキュメント側に書く)
2. **SKILL.md の word count が 5,000 words 以下か** — `wc -w` で確認。超える場合は詳細を references/ に分離する
3. **SKILL.md と references/ に情報重複がないか** — SKILL.md には概要・手順のみ、詳細コード例は references/ に分離
4. **example/ 内の import が整合しているか** — `grep -r "^import"` でプロジェクト固有依存が残っていないか確認
5. **README テーブルが既存行と同じフォーマットか** — HTML タグ、改行、code block の書き方を既存行と比較

### ベストプラクティス準拠チェック

Step 3 で参照した skill-creator のベストプラクティスに照らして以下を確認する:

1. **description がトリガーとして十分か** — 具体的なトリガーフレーズが含まれているか。undertrigger 防止のため積極的に記述されているか
2. **Progressive Disclosure** — SKILL.md 本文が 500 行以下に収まっているか。詳細は references/ に分離されているか
3. **命令形で記述されているか** — 「〜してください」ではなく「〜する」の形式
4. **リソース構成が適切か** — 不要なディレクトリが作られていないか。references/ と example/ の役割が混在していないか
5. **AI 責務最小化** — contribute-skill.md の「PR 前セルフレビュー: AI 責務最小化チェックリスト」を実施したか

問題が見つかった場合は修正してからユーザーに報告する。

## Step 5: PR の作成

以下の形式でユーザーに確認し、**明示的な許可を得てから** push・PR 作成を実行する:

```
/tmp/contribute-skill-<skill-name>/ にスキルファイルを作成しました。
内容を確認したい場合はこのディレクトリを直接参照できます。

- push 先: <repo> (ブランチ: add-skill/<skill-name>)
- コミット対象ファイル:
  - skills/<skill-name>/SKILL.md
  - <その他の追加ファイル一覧>
- プロジェクト固有情報: 含まれていないことを確認済み

このまま PR を作成してよろしいですか？
```

許可を得たら、以下を実行する:

```bash
cd /tmp/contribute-skill-<skill-name>
git checkout -b add-skill/<skill-name>
git add skills/<skill-name>/ README.md README.ja.md
git commit -m "add <skill-name> skill"
git push -u origin add-skill/<skill-name>
```

PR を作成する。タイトル・本文は下記「PR フォーマット」に **厳密に** 従う:

```bash
gh pr create \
  --repo <repo> \
  --head add-skill/<skill-name> \
  --title "Add skill: <skill-name>" \
  --body "## Summary
- <スキルの目的 (50 文字以内)>
- 主要ファイル: skills/<skill-name>/SKILL.md

## 実行イメージ
1. <トリガーフレーズで skill が発動する様子>
2. <実行される主なステップ>
3. <最終的な成果物・完了報告>

## 備考
- 特になし
"
```

### PR フォーマット

- **タイトル**: 新規追加は `Add skill: <skill-name>`、既存 skill の更新は `Update skill: <skill-name>` (rule / prompt の contribute では `skill` の箇所を `rule` / `prompt` に変える)
- **本文**: `## Summary` / `## 実行イメージ` / `## 備考` の 3 セクション **のみ** で構成する。これ以外のセクションを入れてはならない
  - **Summary** — 箇条書き最大 3 つ、各行 50 文字以内。主要ファイル 1〜3 つ (基本 1 つ。SKILL.md や README 等) を含める
  - **実行イメージ** — skill 実行時に何が起きるかを `1.` 始まりの番号付き箇条書きで列挙する。各行 100 文字以内
  - **備考** — 補足事項。無ければ「特になし」と書く

### PR 作成後のフォーマットチェック

PR 作成後、以下のコマンドでフォーマット準拠を検証する。NG が出た場合は `gh pr edit` で修正し、再度チェックする:

```bash
gh pr view <PR番号> --repo <repo> --json title,body > /tmp/pr-format.json
python3 - <<'EOF'
import json, re, sys
d = json.load(open('/tmp/pr-format.json'))
title, body = d['title'], d['body']
errs = []
if not re.match(r'^(Add|Update) (skill|rule|prompt): \S+$', title):
    errs.append(f'タイトルが "Add skill: <name>" / "Update skill: <name>" 形式でない: {title!r}')
headers = re.findall(r'^## (.+?)\s*$', body, re.M)
if headers != ['Summary', '実行イメージ', '備考']:
    errs.append(f'セクションが Summary / 実行イメージ / 備考 の 3 つちょうどでない: {headers}')
else:
    summary, image, _notes = re.split(r'^## .+$', body, flags=re.M)[1:]
    bullets = [l.strip()[2:] for l in summary.splitlines() if l.strip().startswith('- ')]
    if not 1 <= len(bullets) <= 3:
        errs.append(f'Summary の箇条書きが {len(bullets)} 個 (1〜3 個にする)')
    errs += [f'Summary 50 文字超: {b!r}' for b in bullets if len(b) > 50]
    nums = [re.sub(r'^\s*\d+\.\s*', '', l) for l in image.splitlines() if re.match(r'\s*\d+\.', l)]
    if not nums:
        errs.append('実行イメージに番号付き箇条書きがない')
    errs += [f'実行イメージ 100 文字超: {n!r}' for n in nums if len(n) > 100]
print('\n'.join(errs) if errs else 'PR format OK')
sys.exit(1 if errs else 0)
EOF
```

作成された PR の URL とフォーマットチェック結果をユーザーに報告する。

## エラー時の対応

- `gh auth status` 失敗 → `gh auth login` を案内
- clone 失敗 → ネットワーク接続の確認を案内
- push 失敗 → リポジトリへの write 権限を確認するよう案内。fork の利用を提案
- PR 作成失敗で同名ブランチが既存 → ブランチ名にサフィックス (`-v2` 等) を付与して再試行

## フォールバック

contribute-skill.md が clone 先に存在しない場合、以下の最低限の構成で作成する:

1. `skills/<skill-name>/SKILL.md` に YAML frontmatter (name, description) + 手順を記述
2. `skills/<skill-name>.md` / `<skill-name>.ja.md` に概要・使い方・前提条件を記述
3. README のテーブルは手動で更新するようユーザーに案内する
