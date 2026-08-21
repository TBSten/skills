---
name: contribute-prompt
description: >
  現在のプロジェクトで得た知見・手順を TBSten/skills リポジトリに
  一回限りのプロンプト (prompt) として登録するための PR を自動作成する。
  プロジェクトの CLAUDE.md、.claude/rules/、.claude/skills/、コードベースから知見を収集し、
  raw URL 参照でコピペ実行できる PROMPT.md としてパッケージングして PR を作成するまでを一貫して行う。
  セットアップ・スキャフォールドのような 1 回で完結する手順に向く。
  Use when requested: "知見をプロンプトとして登録", "contribute prompt", "このプロンプトを共有",
  "プロンプトとして登録", "プロンプトをまとめて PR", "この手順をプロンプト化",
  "ワンショットプロンプトにして".
  gh CLI と git がインストールされている必要がある。
metadata:
  status: Experimental
---

# contribute-prompt

現在のプロジェクトから知見を収集し、TBSten/skills リポジトリに一回限りのプロンプト (prompt) として登録する PR を自動作成する。

## 前提条件チェック

スキル起動時にまず以下を確認する。失敗した場合は対処方法を案内して中断する。

1. `git --version` で git の存在を確認
2. `gh auth status` で gh CLI の認証状態を確認

## Step 1: 起動時の確認

ARGUMENTS からユーザーが収集したい知見の説明を受け取る。以下を確認する。
ユーザーの指示から明確に読み取れる項目は確認を省略してよい。

1. **prompt 名** — kebab-case で命名。知見の内容から適切な名前を提案する
2. **収集対象** — どの知見をプロンプト化するか。具体的なファイルパスやセクションを特定する
3. **prompt に向いているか** — 1 回で完結する作業 (セットアップ、スキャフォールド等) か。継続的に発動すべき知見なら contribute-skill / contribute-rule を提案する
4. **status** — prompt の成熟度 (WIP / Experimental / Active / Active-Prime)。デフォルトは `Experimental`。ユーザーの指示があればそれに従う
5. **group** — prompt の所属グループ (「タスク管理」「Kotlin / Android アプリ開発」「Kotlin ライブラリ/ツール開発」「Web フロントエンド」「Git / GitHub」等)。知見の内容から適切なグループを提案する。全一覧は clone 先の `.claude/skills/contribute-prompt.md` の group 表を正とする
6. **リポジトリ** — デフォルトは `TBSten/skills`。fork を使う場合はユーザーに確認する

## Step 2: 知見の収集と整理

ユーザーが指定した収集対象を読み取る。独立したファイル読み取りは必ず並列で実行すること。

主なソース:

- CLAUDE.md の特定セクション
- `.claude/rules/` / `.claude/skills/` 内の既存ファイル
- コードベース内の手順・設定ファイル・テンプレート
- ユーザーの説明そのもの

読み取った知見を以下の形式で整理し、ユーザーに提示する:

- **プロンプトの目的** — 1〜2文で何を実現するか
- **実行手順** — プロンプトが指示するステップの箇条書き
- **参照ファイル** — プロンプトが参照するテンプレート等。PROMPT.md 内へのインライン化を基本とし、既存 skill のファイルを使う場合は raw URL 参照にする
- **対象プロジェクトの前提** — 言語、フレームワーク等の前提条件
- **status** — prompt の成熟度 (デフォルト `Experimental`)
- **group** — prompt の所属グループ

ユーザーの承認を得てから次のステップに進む。

## Step 3: ワークディレクトリの準備と contribute-prompt.md の読み込み

1. ワークディレクトリを準備する:

```bash
rm -rf /tmp/contribute-prompt-<prompt-name>
git clone --depth 1 https://github.com/<repo>.git /tmp/contribute-prompt-<prompt-name>
```

`<repo>` は Step 1 で確認したリポジトリ (デフォルト: `TBSten/skills`)。
`<prompt-name>` は Step 1 で決定したプロンプト名。

2. clone したリポジトリ内のプロンプト作成ガイドを読み込む:

```
/tmp/contribute-prompt-<prompt-name>/.claude/skills/contribute-prompt.md
```

contribute-prompt.md を読み込んだ後、以下のように統合する:

- **contribute-prompt.md の「PROMPT.md の書き方」「詳細ドキュメント」「README の更新」**: そのまま従う。内容は contribute-prompt の Step 2 で整理した知見を使う
- **contribute-prompt.md の「AI 責務最小化 (script 化)」**: Step 4 で決定的な手順を script 化し、raw URL 取得で実行させる

ガイドが見つからない場合は、以下の「フォールバック」セクションに従う。

## Step 4: プロンプトファイルの作成

clone した `/tmp/contribute-prompt-<prompt-name>/` 内で、contribute-prompt.md の手順に従い以下を作成する。

1. `prompts/<prompt-name>/PROMPT.md` — プロンプト本体 (frontmatter なし、命令形、self-contained)
2. `prompts/<prompt-name>.md` — 詳細ドキュメント (英語、frontmatter に `status` / `group`)
3. `prompts/<prompt-name>.ja.md` — 詳細ドキュメント (日本語、frontmatter なし)
4. `README.md` と `README.ja.md` の Available Prompts テーブルに行を追加

### プロジェクト固有情報の除外チェック

プロンプトは公開リポジトリに登録されるため、作成したファイルに以下が含まれていないか細心の注意を払う:

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

1. **必要なファイルが揃っているか**:
   - `./prompts/<prompt-name>/PROMPT.md` が存在すること
   - `./prompts/<prompt-name>.md` / `./prompts/<prompt-name>.ja.md` が存在すること
   - `README.md` と `README.ja.md` の Available Prompts テーブルに新しいエントリが追加されていること
2. **PROMPT.md に frontmatter が無いこと**。raw URL 直取得で単体実行できる self-contained な内容になっていること
3. **リポジトリ内ファイルへの相対パス参照が残っていないこと** — 参照はすべて raw URL / sparse clone 経由
4. **status / group の SSoT 一致** — `prompts/<prompt-name>.md` の frontmatter と README のステータス列・グループ列が一致していること。グループセルはグループ先頭行のみ絵文字付きで記載・継続行は空セルになっていること
5. **README の説明セルが 80 文字以内であること** (収まらない詳細は詳細ドキュメント側に書く)
6. **命令形で記述されているか** — 「〜してください」ではなく「〜する」の形式
7. **AI 責務最小化** — contribute-prompt.md の「PR 前セルフレビュー: AI 責務最小化チェックリスト」を実施したか

問題が見つかった場合は修正してからユーザーに報告する。

## Step 5: PR の作成

以下の形式でユーザーに確認し、**明示的な許可を得てから** push・PR 作成を実行する:

```
/tmp/contribute-prompt-<prompt-name>/ にプロンプトファイルを作成しました。
内容を確認したい場合はこのディレクトリを直接参照できます。

- push 先: <repo> (ブランチ: add-prompt/<prompt-name>)
- コミット対象ファイル:
  - prompts/<prompt-name>/PROMPT.md
  - <その他の追加ファイル一覧>
- プロジェクト固有情報: 含まれていないことを確認済み

このまま PR を作成してよろしいですか？
```

許可を得たら、以下を実行する:

```bash
cd /tmp/contribute-prompt-<prompt-name>
git checkout -b add-prompt/<prompt-name>
git add prompts/ README.md README.ja.md
git commit -m "add <prompt-name> prompt"
git push -u origin add-prompt/<prompt-name>
```

PR を作成する。タイトル・本文は下記「PR フォーマット」に **厳密に** 従う:

```bash
gh pr create \
  --repo <repo> \
  --head add-prompt/<prompt-name> \
  --title "Add prompt: <prompt-name>" \
  --body "## Summary
- <プロンプトの目的 (50 文字以内)>
- 主要ファイル: prompts/<prompt-name>/PROMPT.md

## 実行イメージ
1. <ユーザーが README の実行プロンプトをコピペする>
2. <PROMPT.md が取得され、実行される主なステップ>
3. <最終的な成果物・完了報告>

## 備考
- 特になし
"
```

### PR フォーマット

- **タイトル**: 新規追加は `Add prompt: <prompt-name>`、既存 prompt の更新は `Update prompt: <prompt-name>` (skill / rule の contribute では `prompt` の箇所を `skill` / `rule` に変える)
- **本文**: `## Summary` / `## 実行イメージ` / `## 備考` の 3 セクション **のみ** で構成する。これ以外のセクションを入れてはならない
  - **Summary** — 箇条書き最大 3 つ、各行 50 文字以内。主要ファイル 1〜3 つ (基本 1 つ。PROMPT.md や README 等) を含める
  - **実行イメージ** — prompt 実行時に何が起きるかを `1.` 始まりの番号付き箇条書きで列挙する。各行 100 文字以内
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
    errs.append(f'タイトルが "Add prompt: <name>" / "Update prompt: <name>" 形式でない: {title!r}')
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

contribute-prompt.md が clone 先に存在しない場合、以下の最低限の構成で作成する:

1. `prompts/<prompt-name>/PROMPT.md` にプロンプトの内容を記述 (frontmatter なし)
2. `prompts/<prompt-name>.md` / `<prompt-name>.ja.md` に概要・実行プロンプト・前提条件を記述
3. README のテーブルは手動で更新するようユーザーに案内する
