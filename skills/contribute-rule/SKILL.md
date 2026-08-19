---
name: contribute-rule
description: >
  現在のプロジェクトで得た知見・規約・ベストプラクティスを TBSten/skills リポジトリに
  rule として登録するための PR を自動作成する。
  プロジェクトの CLAUDE.md、.claude/rules/、コードベースから知見を収集し、
  再利用可能な Claude Code rule としてパッケージングして PR を作成するまでを一貫して行う。
  rule は .claude/rules/ に配置されるファイルであり、skill とは異なり frontmatter は不要。
  RULE.md がルール本体となり、詳細ドキュメント (<rule-name>.md / <rule-name>.ja.md) を rules/ ディレクトリ直下に配置する。
  Use when requested: "知見をルールとして登録", "contribute rule", "このルールを共有",
  "ルールとして登録", "ルールをまとめて PR", "この規約をルール化",
  "ベストプラクティスをルールに".
  gh CLI と git がインストールされている必要がある。
metadata:
  status: Active
---

# contribute-rule

現在のプロジェクトから知見を収集し、TBSten/skills リポジトリに rule として登録する PR を自動作成する。

## 前提条件チェック

スキル起動時にまず以下を確認する。失敗した場合は対処方法を案内して中断する。

1. `git --version` で git の存在を確認
2. `gh auth status` で gh CLI の認証状態を確認

## Step 1: 起動時の確認

ARGUMENTS からユーザーが収集したい知見の説明を受け取る。以下を確認する。
ユーザーの指示から明確に読み取れる項目は確認を省略してよい。

1. **rule 名** — kebab-case で命名。知見の内容から適切な名前を提案する
2. **収集対象** — どの知見をルール化するか。具体的なファイルパスやセクションを特定する
3. **ルールの対象ユーザー** — どのようなプロジェクト・状況で適用されるルールか
4. **参照ファイルの有無** — ルールが参照するテンプレートやサンプルコード等があるか
5. **status** — rule の成熟度 (WIP / Experimental / Active / Active-Prime)。デフォルトは `Experimental`。ユーザーの指示があればそれに従う
6. **group** — rule の所属グループ (「タスク管理」「Kotlin / Android アプリ開発」「Kotlin ライブラリ/ツール開発」「Web フロントエンド」「Git / GitHub」等)。知見の内容から適切なグループを提案する。全一覧は clone 先の `.claude/skills/contribute-rule.md` の group 表を正とする
7. **リポジトリ** — デフォルトは `TBSten/skills`。fork を使う場合はユーザーに確認する

## Step 2: 知見の収集と整理

ユーザーが指定した収集対象を読み取る。

### 収集の効率化

収集対象が多い場合、Agent tool を活用して並列に収集する:

1. 対象ファイルの読み取り (Glob でファイル一覧取得 → 並列 Read)
2. 関連する規約・パターンの調査 (Grep で検索 → 並列 Read)

独立したファイル読み取りは必ず並列で実行すること。

主なソース:

- CLAUDE.md の特定セクション
- `.claude/rules/` 内の既存ルールファイル
- コードベース内の規約・パターン・ベストプラクティス
- コードレビューで繰り返し指摘される事項
- ユーザーの説明そのもの

読み取った知見を以下の形式で整理し、ユーザーに提示する:

- **ルールの目的** — 1〜2文で何を規定するか
- **ルールの内容** — Claude Code に指示する振る舞いの箇条書き
- **適用条件** — どのようなプロジェクト・状況でこのルールが有効か
- **参照ファイル** — テンプレートやサンプルコード等。不要であれば「なし」と明記
- **status** — rule の成熟度 (デフォルト `Experimental`)
- **group** — rule の所属グループ

ユーザーの承認を得てから次のステップに進む。

## Step 3: ワークディレクトリの準備と contribute-rule.md の読み込み

1. ワークディレクトリを準備する:

```bash
rm -rf /tmp/contribute-rule-<rule-name>
git clone --depth 1 https://github.com/<repo>.git /tmp/contribute-rule-<rule-name>
```

`<repo>` は Step 1 で確認したリポジトリ (デフォルト: `TBSten/skills`)。
`<rule-name>` は Step 1 で決定したルール名。

2. clone したリポジトリ内のルール作成ガイドを読み込む:

```
/tmp/contribute-rule-<rule-name>/.claude/skills/contribute-rule.md
```

contribute-rule.md を読み込んだ後、以下のように統合する:

- **contribute-rule.md の Step 1 (確認事項)**: contribute-rule の Step 1-2 で既に完了。スキップする
- **contribute-rule.md の Step 2 (ディレクトリとファイルの作成)**: 構成ルールに従う。内容は contribute-rule の Step 2 で整理した知見を使う
- **contribute-rule.md の Step 2.5 (詳細ドキュメントの作成)**: そのまま従う
- **contribute-rule.md の Step 3 (README の更新)**: そのまま従う

ガイドが見つからない場合は、以下の「フォールバック」セクションに従う。

3. 高品質なルールの書き方を参照する:

```
https://github.com/anthropics/skills/blob/main/skills/skill-creator/SKILL.md
```

WebFetch 等で上記の skill-creator SKILL.md を取得し、以下の観点を把握してから Step 4 に進む:

- **命令形での記述**: 「〜してください」ではなく「〜する」の形式
- **Progressive Disclosure**: RULE.md 本文は簡潔に。詳細な参照情報は別ファイルに分離
- **具体的かつ汎用的**: 特定プロジェクトに依存しない、再利用可能な記述

取得できない場合はスキップしてよい。

## Step 4: ルールファイルの作成

clone した `/tmp/contribute-rule-<rule-name>/` 内で、contribute-rule.md の手順に従い以下を作成する。
Step 2 で整理した知見をもとに RULE.md を記述する。

1. `rules/<rule-name>/RULE.md` — ルール本体。YAML frontmatter は不要。命令形 (imperative) で記述する
2. 参照ファイルがあれば同ディレクトリに配置
   - インストール時にユーザーのカレントディレクトリにコピーされることを意識してパスを設計する
   - サブディレクトリのネストも可能
3. `rules/<rule-name>.md` — 詳細ドキュメント (英語)
4. `rules/<rule-name>.ja.md` — 詳細ドキュメント (日本語)
5. `README.md` と `README.ja.md` の Available Rules テーブルに行を追加

### レビューの提示方法

作成ファイル数に応じて提示方法を変える:

- **3 ファイル以下**: 全ファイルの内容を提示
- **4 ファイル以上**: 以下を提示
  1. ファイル一覧と各ファイルの概要 (1行)
  2. RULE.md の全内容 (最重要ファイル)
  3. 「他のファイルも確認しますか？」とユーザーに確認

フィードバックがあれば修正してから次に進む。

### プロジェクト固有情報の除外チェック

ルールは公開リポジトリに登録されるため、作成したファイルに以下が含まれていないか細心の注意を払う:

- プロジェクト固有のファイルパス、URL、ドメイン名
- 社内システムやサービスの名前
- 認証情報、トークン、API キー
- 個人名、メールアドレス、チーム名
- 社内ドキュメントへのリンク
- その他、公開すべきでない情報

知見を汎用化する際に具体例が必要な場合は、プレースホルダー (`<project-name>`, `<your-domain>` 等) に置き換える。
チェック結果をユーザーに報告し、問題がないことを確認してから次に進む。

## Step 4.5: セルフレビュー

PR 作成前に以下を確認する。

### ファイル・フォーマットチェック

1. **必要なファイルが揃っているか** — 以下のファイルが存在することを確認する:
   - `./rules/<rule-name>.md` が存在すること (詳細ドキュメント 英語)
   - `./rules/<rule-name>.ja.md` が存在すること (詳細ドキュメント 日本語)
   - `./rules/<rule-name>/RULE.md` が存在すること
   - ルールが参照するすべてのファイルが `./rules/<rule-name>/` 配下に存在すること
   - `README.md` と `README.ja.md` の Available Rules テーブルに新しいルールのエントリが追加されていること
   - `rules/<rule-name>.md` の frontmatter に `status` があり、README のステータス列 (絵文字+ラベル) と一致していること
   - `rules/<rule-name>.md` の frontmatter に `group` があり、README のグループ列と一致していること。行が同じグループのまとまりに挿入され、グループセルはグループ先頭行のみ絵文字付きで記載・継続行は空セルになっていること
   - README の説明セルが 80 文字以内であること (収まらない詳細は詳細ドキュメント側に書く)
2. **参照ファイルのパスが適切か** — ユーザーのプロジェクトルートに展開されることを考慮
3. **README テーブルが既存行と同じフォーマットか** — HTML タグ、改行、code block の書き方を既存行と比較

### ベストプラクティス準拠チェック

Step 3 で参照したベストプラクティスに照らして以下を確認する:

1. **RULE.md が命令形で記述されているか** — 「〜してください」ではなく「〜する」の形式
2. **ルールの内容が汎用的か** — 特定プロジェクトでしか適用できないルールになっていないか
3. **Progressive Disclosure** — RULE.md 本文は簡潔か。詳細な情報は参照ファイルに分離されているか
4. **具体的なのに再利用可能か** — 曖昧すぎず、かつ特定プロジェクトに依存しない記述になっているか

問題が見つかった場合は修正してからユーザーに報告する。

## Step 5: PR の作成

以下の形式でユーザーに確認し、**明示的な許可を得てから** push・PR 作成を実行する:

```
/tmp/contribute-rule-<rule-name>/ にルールファイルを作成しました。
内容を確認したい場合はこのディレクトリを直接参照できます。

- push 先: <repo> (ブランチ: add-rule/<rule-name>)
- コミット対象ファイル:
  - rules/<rule-name>/RULE.md
  - <その他の追加ファイル一覧>
- プロジェクト固有情報: 含まれていないことを確認済み

このまま PR を作成してよろしいですか？
```

許可を得たら、以下を実行する:

```bash
cd /tmp/contribute-rule-<rule-name>
git checkout -b add-rule/<rule-name>
git add rules/<rule-name>/ README.md README.ja.md
git commit -m "add <rule-name> rule"
git push -u origin add-rule/<rule-name>
```

PR を作成する。タイトル・本文は下記「PR フォーマット」に **厳密に** 従う:

```bash
gh pr create \
  --repo <repo> \
  --head add-rule/<rule-name> \
  --title "Add rule: <rule-name>" \
  --body "## Summary
- <ルールの目的 (50 文字以内)>
- 主要ファイル: rules/<rule-name>/RULE.md

## 実行イメージ
1. <インストール後、ルールが読み込まれる様子>
2. <ルールが適用される場面・振る舞い>
3. <ユーザーから見た効果>

## 備考
- 特になし
"
```

### PR フォーマット

- **タイトル**: 新規追加は `Add rule: <rule-name>`、既存 rule の更新は `Update rule: <rule-name>` (skill / prompt の contribute では `rule` の箇所を `skill` / `prompt` に変える)
- **本文**: `## Summary` / `## 実行イメージ` / `## 備考` の 3 セクション **のみ** で構成する。これ以外のセクションを入れてはならない
  - **Summary** — 箇条書き最大 3 つ、各行 50 文字以内。主要ファイル 1〜3 つ (基本 1 つ。RULE.md や README 等) を含める
  - **実行イメージ** — rule 適用時に何が起きるかを `1.` 始まりの番号付き箇条書きで列挙する。各行 100 文字以内
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
    errs.append(f'タイトルが "Add rule: <name>" / "Update rule: <name>" 形式でない: {title!r}')
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

contribute-rule.md が clone 先に存在しない場合、以下の最低限の構成で作成する:

1. `rules/<rule-name>/RULE.md` にルールの内容を記述
2. 参照ファイルがあれば同ディレクトリに配置
3. README のテーブルは手動で更新するようユーザーに案内する
