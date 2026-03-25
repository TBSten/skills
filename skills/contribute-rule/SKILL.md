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
5. **リポジトリ** — デフォルトは `TBSten/skills`。fork を使う場合はユーザーに確認する

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

ユーザーの承認を得てから次のステップに進む。

## Step 3: ワークディレクトリの準備と add-rule.md の読み込み

1. ワークディレクトリを準備する:

```bash
rm -rf /tmp/contribute-rule
git clone --depth 1 https://github.com/<repo>.git /tmp/contribute-rule
```

`<repo>` は Step 1 で確認したリポジトリ (デフォルト: `TBSten/skills`)。

2. clone したリポジトリ内のルール作成ガイドを読み込む:

```
/tmp/contribute-rule/.claude/skills/add-rule.md
```

add-rule.md を読み込んだ後、以下のように統合する:

- **add-rule.md の Step 1 (確認事項)**: contribute-rule の Step 1-2 で既に完了。スキップする
- **add-rule.md の Step 2 (ディレクトリとファイルの作成)**: 構成ルールに従う。内容は contribute-rule の Step 2 で整理した知見を使う
- **add-rule.md の Step 2.5 (詳細ドキュメントの作成)**: そのまま従う
- **add-rule.md の Step 3 (README の更新)**: そのまま従う

ガイドが見つからない場合は、以下の「フォールバック」セクションに従う。

## Step 4: ルールファイルの作成

clone した `/tmp/contribute-rule/` 内で、add-rule.md の手順に従い以下を作成する。
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

PR 作成前に以下を確認する:

1. **必要なファイルが揃っているか** — 以下のファイルが存在することを確認する:
   - `./rules/<rule-name>.md` が存在すること (詳細ドキュメント 英語)
   - `./rules/<rule-name>.ja.md` が存在すること (詳細ドキュメント 日本語)
   - `./rules/<rule-name>/RULE.md` が存在すること
   - ルールが参照するすべてのファイルが `./rules/<rule-name>/` 配下に存在すること
   - `README.md` と `README.ja.md` の Available Rules テーブルに新しいルールのエントリが追加されていること
2. **RULE.md が命令形で記述されているか** — 「〜してください」ではなく「〜する」の形式
3. **ルールの内容が汎用的か** — 特定プロジェクトでしか適用できないルールになっていないか
4. **参照ファイルのパスが適切か** — ユーザーのプロジェクトルートに展開されることを考慮
5. **README テーブルが既存行と同じフォーマットか** — HTML タグ、改行、code block の書き方を既存行と比較

問題が見つかった場合は修正してからユーザーに報告する。

## Step 5: PR の作成

以下の内容をユーザーに提示し、**push と PR 作成の許可を明示的に得てから**実行する:

- push 先リポジトリとブランチ名
- コミットに含まれるファイル一覧
- プロジェクト固有情報が含まれていないことの最終確認

許可を得たら、以下を実行する:

```bash
cd /tmp/contribute-rule
git checkout -b add-rule/<rule-name>
git add rules/<rule-name>/ README.md README.ja.md
git commit -m "add <rule-name> rule"
git push -u origin add-rule/<rule-name>
```

PR を作成する:

```bash
gh pr create \
  --repo <repo> \
  --head add-rule/<rule-name> \
  --title "Add <rule-name> rule" \
  --body "## Summary
- <Step 2 で整理したルールの目的>

## Files
- rules/<rule-name>/RULE.md
- <その他の追加ファイル一覧>

## Test plan
- [ ] curl -fsSL .../rules/install.sh | bash -s -- <rule-name> でインストールできること
- [ ] .claude/rules/<rule-name>.md が正しく配置されること
- [ ] 参照ファイルがカレントディレクトリに正しく配置されること
"
```

作成された PR の URL をユーザーに報告する。

## エラー時の対応

- `gh auth status` 失敗 → `gh auth login` を案内
- clone 失敗 → ネットワーク接続の確認を案内
- push 失敗 → リポジトリへの write 権限を確認するよう案内。fork の利用を提案
- PR 作成失敗で同名ブランチが既存 → ブランチ名にサフィックス (`-v2` 等) を付与して再試行

## フォールバック

add-rule.md が clone 先に存在しない場合、以下の最低限の構成で作成する:

1. `rules/<rule-name>/RULE.md` にルールの内容を記述
2. 参照ファイルがあれば同ディレクトリに配置
3. README のテーブルは手動で更新するようユーザーに案内する
